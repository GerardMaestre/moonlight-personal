package com.limelight.shared.data.remote.repository

import com.limelight.shared.data.immich.ImmichConnectionConfig
import com.limelight.shared.data.remote.dto.AssetDto
import com.limelight.shared.data.remote.dto.AssetsResponseDto
import com.limelight.shared.data.remote.mapper.AssetMapper
import com.limelight.shared.data.remote.paging.AssetPagingSource
import com.limelight.shared.data.session.AuthHeaderProvider
import com.limelight.shared.domain.media.Asset
import com.limelight.shared.domain.media.TimelinePage
import com.limelight.shared.network.immich.ImmichApiClient
import com.limelight.shared.network.immich.normalizedBaseUrl
import com.limelight.platform.Logger
import io.ktor.http.Headers
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.plugins.ConnectTimeoutException
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import io.ktor.http.URLBuilder
import io.ktor.http.appendPathSegments
import io.ktor.http.isSuccess
import io.ktor.utils.io.core.buildPacket
import io.ktor.utils.io.core.writeFully
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retryWhen

class ImmichAssetRepository(
    private val config: ImmichConnectionConfig,
    private val httpClient: HttpClient = ImmichApiClient.defaultHttpClient(),
    private val authHeaderProvider: AuthHeaderProvider = AuthHeaderProvider(),
    private val logger: Logger = Logger(),
) {
    private val tag = "ImmichAssetRepository"
    private val pagingSource = AssetPagingSource(loader = ::loadAssetPage)

    suspend fun getTimelinePage(page: Int?, cursor: String?, size: Int): TimelinePage =
        pagingSource.load(page = page ?: 1, cursor = cursor)

    suspend fun getAssetDetail(assetId: String): Asset {
        val dto = request<AssetDto>("/api/assets/$assetId")
        return AssetMapper.toDomain(dto)
    }

    fun getThumbnailUrl(assetId: String): String = URLBuilder(normalizedBaseUrl(config.baseUrl)).apply {
        appendPathSegments("api", "assets", assetId, "thumbnail")
        parameters.append("size", "thumbnail")
    }.buildString()

    suspend fun uploadAssetMultipart(fileName: String, bytes: ByteArray, mimeType: String): String {
        val response = httpClient.post(normalizedBaseUrl(config.baseUrl) + "/api/assets") {
            authHeaderProvider.headersFor(config).forEach { (name, value) -> header(name, value) }
            setBody(MultiPartFormDataContent(formData {
                append("assetData", buildPacket { writeFully(bytes) }, headers = Headers.build {
                    append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                    append(HttpHeaders.ContentType, mimeType)
                })
            }))
        }
        if (!response.status.isSuccess()) throw DataError.Network()
        val created = response.body<AssetDto>()
        return requireNotNull(created.id) { "Uploaded asset id missing" }
    }



data class UploadProgress(val sentBytes: Long, val totalBytes: Long, val remoteId: String? = null) {
    val fraction: Float get() = if (totalBytes <= 0L) 0f else sentBytes.toFloat() / totalBytes.toFloat()
}

fun uploadAssetMultipartFlow(fileName: String, bytes: ByteArray, mimeType: String, maxRetries: Int = 3): Flow<UploadProgress> = flow {
    emit(UploadProgress(0, bytes.size.toLong()))
    val remoteId = uploadAssetMultipart(fileName, bytes, mimeType)
    emit(UploadProgress(bytes.size.toLong(), bytes.size.toLong(), remoteId))
}.retryWhen { cause, attempt ->
    if (attempt >= maxRetries) return@retryWhen false
    if (cause is DataError.Network) {
        val backoff = 500L * (1L shl attempt.toInt())
        delay(backoff)
        true
    } else false
}

    fun streamTimeline(size: Int = 60): Flow<TimelinePage> = flow {
        var nextPage: Int? = 1
        var nextCursor: String? = null
        do {
            val page = getTimelinePage(nextPage, nextCursor, size)
            emit(page)
            nextPage = page.nextPage
            nextCursor = page.nextCursor
        } while (nextPage != null || nextCursor != null)
    }.retryWhen { cause, attempt ->
        if (attempt >= 3) return@retryWhen false
        if (cause is DataError.Network) {
            delay((attempt + 1) * 400)
            true
        } else false
    }

    private suspend fun loadAssetPage(page: Int?, cursor: String?, size: Int): AssetsResponseDto = request("/api/assets") {
        parameter("page", page ?: 1)
        parameter("size", size)
        cursor?.let { parameter("cursor", it) }
    }

    private suspend inline fun <reified T> request(path: String, crossinline block: io.ktor.client.request.HttpRequestBuilder.() -> Unit = {}): T {
        return try {
            httpClient.get(normalizedBaseUrl(config.baseUrl) + path) {
                authHeaderProvider.headersFor(config).forEach { (name, value) -> header(name, value) }
                block()
            }.body()
        } catch (error: ClientRequestException) {
            val mapped = when (error.response.status.value) {
                401 -> DataError.Validation("401 no autorizado: API Key o token inválido.")
                403 -> DataError.Validation("403 prohibido: credenciales sin permisos para este recurso.")
                else -> DataError.Network(error)
            }
            logger.error(tag, "request failed with client status ${error.response.status.value}: ${mapped.message}", error)
            throw mapped
        } catch (error: ServerResponseException) {
            logger.error(tag, "request failed with server status ${error.response.status.value}", error)
            throw DataError.Validation("Immich devolvió ${error.response.status.value}. Reintenta más tarde.")
        } catch (error: ConnectTimeoutException) {
            logger.error(tag, "request timeout while connecting to Immich", error)
            throw DataError.Validation("Timeout de conexión con Immich. Revisa URL y red.")
        } catch (error: HttpRequestTimeoutException) {
            logger.error(tag, "request timeout waiting Immich response", error)
            throw DataError.Validation("Timeout de respuesta de Immich. Reintenta en unos segundos.")
        } catch (error: IllegalArgumentException) {
            logger.error(tag, "request validation failed: ${error.message}", error)
            throw DataError.Validation(error.message ?: "Invalid request")
        } catch (error: Exception) {
            if (error.hasCauseNamed("SSLHandshakeException") || error.hasCauseNamed("TLSException")) {
                logger.error(tag, "request TLS/certificate failure", error)
                throw DataError.Validation("Error TLS/certificado. Verifica certificado y fecha del sistema.")
            }
            logger.error(tag, "request failed with unknown error", error)
            throw DataError.Unknown(error)
        }
    }
}

private fun Throwable.hasCauseNamed(simpleName: String): Boolean {
    var current: Throwable? = this
    while (current != null) {
        if (current::class.simpleName == simpleName) return true
        current = current.cause
    }
    return false
}


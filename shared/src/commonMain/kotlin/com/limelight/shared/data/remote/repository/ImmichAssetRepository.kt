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
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import kotlinx.serialization.json.*
import kotlinx.datetime.Clock

import io.ktor.utils.io.core.buildPacket
import io.ktor.utils.io.core.writeFully
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retryWhen

data class UploadProgress(val sentBytes: Long, val totalBytes: Long, val remoteId: String? = null) {
    val fraction: Float get() = if (totalBytes <= 0L) 0f else sentBytes.toFloat() / totalBytes.toFloat()
}

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

    suspend fun uploadAssetMultipart(fileName: String, bytes: ByteArray, mimeType: String, createdAt: String? = null): String {
        val deviceId = "moonlight-personal-mobile"
        val cleanFileName = fileName.replace(Regex("[^a-zA-Z0-9.]"), "_")
        val deviceAssetId = "$cleanFileName-${bytes.size}-${Clock.System.now().toEpochMilliseconds()}"
        val nowStr = Clock.System.now().toString()
        val uploadCreatedAt = createdAt ?: nowStr

        val response = httpClient.post(normalizedBaseUrl(config.baseUrl) + "/api/assets") {
            authHeaderProvider.headersFor(config).forEach { (name, value) -> header(name, value) }
            setBody(MultiPartFormDataContent(formData {
                append("deviceAssetId", deviceAssetId)
                append("deviceId", deviceId)
                append("fileCreatedAt", uploadCreatedAt)
                append("fileModifiedAt", uploadCreatedAt)
                append("isFavorite", "false")
                append("assetData", buildPacket { writeFully(bytes) }, headers = Headers.build {
                    append(HttpHeaders.ContentDisposition, "form-data; name=\"assetData\"; filename=\"$cleanFileName\"")
                    append(HttpHeaders.ContentType, mimeType)
                })
            }))
        }
        if (!response.status.isSuccess()) throw DataError.Network()
        val created = response.body<AssetDto>()
        return requireNotNull(created.id) { "Uploaded asset id missing" }
    }



    fun uploadAssetMultipartFlow(fileName: String, bytes: ByteArray, mimeType: String, createdAt: String? = null, maxRetries: Int = 3): Flow<UploadProgress> = flow {
        emit(UploadProgress(0, bytes.size.toLong()))
        val remoteId = uploadAssetMultipart(fileName, bytes, mimeType, createdAt = createdAt)
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
        var hasMore = true
        while (hasMore) {
            val page = getTimelinePage(nextPage, nextCursor, size)
            emit(page)
            nextPage = page.nextPage
            nextCursor = page.nextCursor
            hasMore = nextPage != null || nextCursor != null
        }
    }.retryWhen { cause, attempt ->
        if (attempt >= 3) return@retryWhen false
        if (cause is DataError.Network) {
            delay((attempt + 1) * 400)
            true
        } else false
    }

    private suspend fun loadAssetPage(page: Int?, cursor: String?, size: Int): AssetsResponseDto {
        return try {
            val requestBody = com.limelight.shared.network.immich.ImmichSearchAssetsRequest(page = page ?: 1, size = size)
            val responseElement: kotlinx.serialization.json.JsonElement = httpClient.post(normalizedBaseUrl(config.baseUrl) + "/api/search/metadata") {
                authHeaderProvider.headersFor(config).forEach { (name, value) -> header(name, value) }
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }.body()
            
            val pageData = when {
                responseElement is JsonObject && responseElement.containsKey("assets") -> {
                    val searchResponse = com.limelight.shared.network.immich.ImmichJson.decodeFromJsonElement<com.limelight.shared.network.immich.ImmichSearchAssetsResponse>(responseElement)
                    searchResponse.assets ?: com.limelight.shared.network.immich.ImmichAssetPage()
                }
                responseElement is JsonObject && responseElement.containsKey("items") -> {
                    com.limelight.shared.network.immich.ImmichJson.decodeFromJsonElement<com.limelight.shared.network.immich.ImmichAssetPage>(responseElement)
                }
                else -> com.limelight.shared.network.immich.ImmichAssetPage()
            }
            AssetsResponseDto(
                count = pageData.count,
                total = pageData.total,
                nextPage = pageData.nextPage,
                items = pageData.items.map { 
                    AssetDto(
                        id = it.id,
                        type = it.type,
                        originalFileName = it.originalFileName,
                        originalMimeType = it.originalMimeType,
                        fileCreatedAt = it.fileCreatedAt,
                        updatedAt = it.updatedAt,
                        isFavorite = it.isFavorite,
                        exifInfo = it.exifInfo?.let { exif -> com.limelight.shared.data.remote.dto.ExifInfoDto(city = exif.city, country = exif.country) }
                    ) 
                }
            )
        } catch (error: io.ktor.client.plugins.ClientRequestException) {
            val mapped = when (error.response.status.value) {
                401 -> DataError.Validation("401 no autorizado: API Key o token inválido.")
                403 -> DataError.Validation("403 prohibido: credenciales sin permisos para este recurso.")
                else -> DataError.Network(error)
            }
            logger.error(tag, "loadAssetPage failed with client status ${error.response.status.value}", error)
            throw mapped
        } catch (error: Exception) {
            logger.error(tag, "loadAssetPage failed", error)
            throw DataError.Unknown(error)
        }
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

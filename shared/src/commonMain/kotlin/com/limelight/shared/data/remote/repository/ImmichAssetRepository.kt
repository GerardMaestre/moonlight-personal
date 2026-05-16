package com.limelight.shared.data.remote.repository

import com.limelight.shared.data.immich.ImmichConnectionConfig
import com.limelight.shared.data.remote.dto.AssetDto
import com.limelight.shared.data.remote.dto.AssetsResponseDto
import com.limelight.shared.data.remote.mapper.AssetMapper
import com.limelight.shared.data.remote.paging.AssetPagingSource
import com.limelight.shared.domain.media.Asset
import com.limelight.shared.domain.media.TimelinePage
import com.limelight.shared.network.immich.defaultHttpClient
import com.limelight.shared.network.immich.normalizedBaseUrl
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.bearerAuth
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
    private val httpClient: HttpClient = defaultHttpClient(),
) {
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
            applyAuth()
            setBody(MultiPartFormDataContent(formData {
                append("assetData", buildPacket { writeFully(bytes) }, headers = io.ktor.http.headersOf(
                    HttpHeaders.ContentDisposition,
                    "filename=\"$fileName\"",
                    HttpHeaders.ContentType,
                    mimeType,
                ))
            }))
        }
        if (!response.status.isSuccess()) throw DataError.Network()
        val created = response.body<AssetDto>()
        return requireNotNull(created.id) { "Uploaded asset id missing" }
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
                applyAuth()
                block()
            }.body()
        } catch (error: ClientRequestException) {
            throw if (error.response.status.value == 401) DataError.Unauthorized(error) else DataError.Network(error)
        } catch (error: IllegalArgumentException) {
            throw DataError.Validation(error.message ?: "Invalid request")
        } catch (error: Exception) {
            throw DataError.Unknown(error)
        }
    }

    private fun io.ktor.client.request.HttpRequestBuilder.applyAuth() {
        when {
            config.apiKey.isNotBlank() -> header("x-api-key", config.apiKey)
            config.bearerToken.isNotBlank() -> bearerAuth(config.bearerToken)
        }
    }
}

package com.limelight.shared.network.immich

import com.limelight.shared.data.immich.ImmichConnectionConfig
import com.limelight.shared.data.immich.ImmichPhotoAsset
import com.limelight.shared.data.immich.ImmichServerSummary
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.URLBuilder
import io.ktor.http.appendPathSegments
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class ImmichApiClient(
    private val httpClient: HttpClient = defaultHttpClient(),
) {
    suspend fun ping(config: ImmichConnectionConfig): ImmichPingResponse = get(config, "/api/server/ping")

    suspend fun loadOverview(config: ImmichConnectionConfig, pageSize: Int = 60): ImmichOverview {
        require(config.baseUrl.isNotBlank()) { "Configura la URL de Immich antes de conectar." }
        require(config.hasCredentials) { "Configura una API Key o Bearer token de Immich." }

        val version = runCatching { getVersion(config) }.getOrElse {
            runCatching { getAbout(config).version }.getOrNull() ?: "Desconocida"
        }
        val user = runCatching { getCurrentUser(config) }.getOrNull()
        val stats = runCatching { searchStatistics(config) }.getOrDefault(ImmichAssetStatisticsResponse())
        val photoPage = searchAssets(config, pageSize = pageSize)
        val photos = photoPage.items.map { it.toPhotoAsset(config) }
        val total = when {
            stats.total > 0 -> stats.total
            photoPage.total > 0 -> photoPage.total
            else -> photos.size
        }

        return ImmichOverview(
            summary = ImmichServerSummary(
                version = version,
                images = stats.images.takeIf { it > 0 } ?: photoPage.total,
                videos = stats.videos,
                totalAssets = total,
                quotaUsageBytes = user?.quotaUsageInBytes,
                quotaSizeBytes = user?.quotaSizeInBytes,
                userName = user?.name ?: user?.email,
            ),
            photos = photos,
        )
    }

    suspend fun getAbout(config: ImmichConnectionConfig): ImmichAboutResponse = get(config, "/api/server/about")

    suspend fun getVersion(config: ImmichConnectionConfig): String {
        val version = get<ImmichVersionResponse>(config, "/api/server/version")
        return listOfNotNull(version.major, version.minor, version.patch).joinToString(".").ifBlank { "Desconocida" }
    }

    suspend fun getCurrentUser(config: ImmichConnectionConfig): ImmichUserResponse = get(config, "/api/users/me")

    suspend fun searchStatistics(config: ImmichConnectionConfig): ImmichAssetStatisticsResponse =
        post(config, "/api/search/statistics", ImmichSearchStatisticsRequest())

    suspend fun searchAssets(config: ImmichConnectionConfig, page: Int = 1, pageSize: Int = 60): ImmichAssetPage {
        val response = post<ImmichSearchAssetsRequest, ImmichSearchAssetsResponse>(
            config = config,
            path = "/api/search/assets",
            body = ImmichSearchAssetsRequest(page = page, size = pageSize.coerceIn(1, 1000)),
        )
        return response.assets ?: ImmichAssetPage()
    }

    fun thumbnailUrl(config: ImmichConnectionConfig, assetId: String): String = URLBuilder(normalizedBaseUrl(config.baseUrl)).apply {
        appendPathSegments("api", "assets", assetId, "thumbnail")
        parameters.append("size", "thumbnail")
    }.buildString()

    private suspend inline fun <reified T> get(config: ImmichConnectionConfig, path: String): T =
        executeImmichRequest {
            httpClient.get(normalizedBaseUrl(config.baseUrl) + path) {
                applyAuthentication(config)
                accept(ContentType.Application.Json)
            }.body()
        }

    private suspend inline fun <reified Request : Any, reified Response> post(
        config: ImmichConnectionConfig,
        path: String,
        body: Request,
    ): Response = executeImmichRequest {
        httpClient.post(normalizedBaseUrl(config.baseUrl) + path) {
            applyAuthentication(config)
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            setBody(body)
        }.body()
    }

    private fun ImmichAssetResponse.toPhotoAsset(config: ImmichConnectionConfig): ImmichPhotoAsset = ImmichPhotoAsset(
        id = id,
        name = originalFileName ?: id,
        thumbnailUrl = thumbnailUrl(config, id),
        createdAt = localDateTime ?: fileCreatedAt,
        location = listOfNotNull(exifInfo?.city, exifInfo?.country).joinToString(", ").ifBlank { null },
        isFavorite = isFavorite,
    )

    private fun io.ktor.client.request.HttpRequestBuilder.applyAuthentication(config: ImmichConnectionConfig) {
        when {
            config.apiKey.isNotBlank() -> header("x-api-key", config.apiKey)
            config.bearerToken.isNotBlank() -> bearerAuth(config.bearerToken)
        }
    }

    companion object {
        fun defaultHttpClient(): HttpClient = HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
            defaultRequest {
                header(HttpHeaders.CacheControl, "no-cache")
            }
        }
    }
}

data class ImmichOverview(
    val summary: ImmichServerSummary,
    val photos: List<ImmichPhotoAsset>,
)

fun normalizedBaseUrl(baseUrl: String): String = baseUrl.trim().trimEnd('/').removeSuffix("/api")

suspend inline fun <T> executeImmichRequest(crossinline block: suspend () -> T): T = try {
    block()
} catch (error: ClientRequestException) {
    throw ImmichApiException("Immich rechazó la solicitud (${error.response.status.value}). Revisa la URL y la clave API.", error)
} catch (error: ServerResponseException) {
    throw ImmichApiException("Immich devolvió un error del servidor (${error.response.status.value}).", error)
} catch (error: HttpRequestTimeoutException) {
    throw ImmichApiException("Timeout conectando con Immich.", error)
} catch (error: IllegalArgumentException) {
    throw ImmichApiException(error.message ?: "Configuración de Immich inválida.", error)
} catch (error: Exception) {
    throw ImmichApiException(error.message ?: "No se pudo conectar con Immich.", error)
}

class ImmichApiException(message: String, cause: Throwable? = null) : Exception(message, cause)

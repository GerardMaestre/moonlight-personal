package com.limelight.shared.network.immich

import com.limelight.shared.data.immich.ImmichConnectionConfig
import com.limelight.shared.data.immich.ImmichPhotoAsset
import com.limelight.shared.data.immich.ImmichServerSummary
import com.limelight.shared.data.session.AuthHeaderProvider
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.json.*

internal val ImmichJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

class ImmichApiClient(
    private val httpClient: HttpClient = defaultHttpClient(),
    private val authHeaderProvider: AuthHeaderProvider = AuthHeaderProvider(),
) {

    private suspend inline fun <reified T> get(config: ImmichConnectionConfig, path: String): T {
        return httpClient.get(normalizedBaseUrl(config.baseUrl) + path) {
            authHeaderProvider.headersFor(config).forEach { (name, value) -> header(name, value) }
            accept(ContentType.Application.Json)
        }.body()
    }

    private suspend inline fun <reified B, reified T> post(config: ImmichConnectionConfig, path: String, body: B): T {
        return httpClient.post(normalizedBaseUrl(config.baseUrl) + path) {
            authHeaderProvider.headersFor(config).forEach { (name, value) -> header(name, value) }
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            setBody(body)
        }.body()
    }

    suspend fun ping(config: ImmichConnectionConfig): ImmichPingResponse = get(config, "/api/server/ping")

    suspend fun loadOverview(config: ImmichConnectionConfig, pageSize: Int = 60): ImmichOverview {
        require(config.baseUrl.isNotBlank()) { "Configura la URL de Immich antes de conectar." }
        require(config.hasCredentials) { "Configura una API Key o Bearer token de Immich." }

        val version = runCatching { getVersion(config) }.getOrElse {
            runCatching { getAbout(config).version }.getOrNull() ?: "Desconocida"
        }
        val user = runCatching { getCurrentUser(config) }.getOrNull()
        val stats = runCatching { searchStatistics(config) }.getOrDefault(ImmichAssetStatisticsResponse())
        
        val photos = runCatching { 
            val element = get<JsonElement>(config, "/api/assets")
            when {
                element is JsonArray -> {
                    ImmichJson.decodeFromJsonElement<List<ImmichAssetResponse>>(element)
                }
                element is JsonObject && element.containsKey("items") -> {
                    ImmichJson.decodeFromJsonElement<List<ImmichAssetResponse>>(element["items"]!!)
                }
                else -> emptyList()
            }
        }.onFailure { 
            println("ImmichApiClient getAssets error: ${it.message}")
            it.printStackTrace() 
        }.getOrElse { 
            searchAssets(config, pageSize = pageSize).items 
        }.map { it.toPhotoAsset(config) }

        val total = when {
            stats.total > 0 -> stats.total
            photos.size > 0 -> photos.size
            else -> 0
        }

        return ImmichOverview(
            summary = ImmichServerSummary(
                version = version,
                images = stats.images.takeIf { it > 0 } ?: photos.size,
                videos = stats.videos,
                totalAssets = total,
                quotaUsageBytes = user?.quotaUsageInBytes,
                quotaSizeBytes = user?.quotaSizeInBytes,
                userName = user?.name ?: user?.email,
            ),
            photos = photos
        )
    }

    suspend fun getVersion(config: ImmichConnectionConfig): String {
        val v = get<ImmichVersionResponse>(config, "/api/server/version")
        return "${v.major}.${v.minor}.${v.patch}"
    }

    suspend fun getAbout(config: ImmichConnectionConfig): ImmichAboutResponse = get(config, "/api/server/about")

    suspend fun getCurrentUser(config: ImmichConnectionConfig): ImmichUserResponse = get(config, "/api/users/me")

    suspend fun searchStatistics(config: ImmichConnectionConfig): ImmichAssetStatisticsResponse =
        post(config, "/api/search/statistics", ImmichSearchStatisticsRequest())

    suspend fun getAssets(config: ImmichConnectionConfig): JsonElement =
        get(config, "/api/assets")

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
    }.buildString()

    fun photoUrl(config: ImmichConnectionConfig, assetId: String): String = URLBuilder(normalizedBaseUrl(config.baseUrl)).apply {
        appendPathSegments("api", "assets", assetId, "original")
    }.buildString()

    private fun ImmichAssetResponse.toPhotoAsset(config: ImmichConnectionConfig) = ImmichPhotoAsset(
        id = id,
        name = originalFileName ?: id,
        thumbnailUrl = thumbnailUrl(config, id),
        createdAt = localDateTime ?: fileCreatedAt,
        location = listOfNotNull(exifInfo?.city, exifInfo?.country).joinToString(", ").ifBlank { null },
        isFavorite = isFavorite,
    )

    companion object {
        fun defaultHttpClient(): HttpClient = platformImmichHttpClient(ImmichJson)
    }
}

data class ImmichOverview(
    val summary: ImmichServerSummary,
    val photos: List<ImmichPhotoAsset>,
)

fun normalizedBaseUrl(baseUrl: String): String = baseUrl.trim().trimEnd('/').removeSuffix("/api")

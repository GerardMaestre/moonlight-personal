package com.limelight.shared.network.immich

import com.limelight.shared.data.immich.ImmichConnectionConfig
import com.limelight.shared.data.immich.ImmichPhotoAsset
import com.limelight.shared.data.immich.ImmichServerSummary
import com.limelight.shared.data.session.AuthHeaderProvider
import com.limelight.platform.Logger
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.ServerResponseException
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
    private val logger: Logger = Logger(),
) {
    private val tag = "ImmichApiClient"


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

    suspend fun loadOverview(config: ImmichConnectionConfig): ImmichOverview {
        require(config.baseUrl.isNotBlank()) { "Configura la URL de Immich antes de conectar." }
        require(config.hasCredentials) { "Configura una API Key o Bearer token de Immich." }

        val version = runCatching { getVersion(config) }.getOrElse {
            runCatching { getAbout(config).version }.getOrNull() ?: "Desconocida"
        }
        val user = runCatching { getCurrentUser(config) }.getOrNull()
        val stats = runCatching { searchStatistics(config) }.getOrDefault(ImmichAssetStatisticsResponse())
        
        val allPhotos = mutableListOf<ImmichPhotoAsset>()
        var currentPage = 1
        val pageSize = 1000
        
        try {
            var keepLoading = true
            while (keepLoading) {
                val page = searchAssets(config, page = currentPage, pageSize = pageSize)
                val items = page.items.map { it.toPhotoAsset(config) }
                allPhotos.addAll(items)

                val hasMorePages = items.size >= pageSize
                val canLoadMore = currentPage < 10 // Limit to 10k assets for safety
                if (hasMorePages && canLoadMore) {
                    currentPage++
                } else {
                    keepLoading = false
                }
            }
            
            logger.debug(tag, "Mapeados ${allPhotos.size} assets en total")
        } catch (error: Throwable) {
            val mapped = mapImmichConnectionError(error)
            logger.error(tag, "loadOverview failed while requesting assets: ${mapped.message}", error)
            throw mapped
        }

        val total = if (allPhotos.size > stats.total) allPhotos.size else stats.total

        return ImmichOverview(
            summary = ImmichServerSummary(
                version = version,
                images = stats.images.takeIf { it > 0 } ?: allPhotos.count { !it.isVideo },
                videos = stats.videos.takeIf { it > 0 } ?: allPhotos.count { it.isVideo },
                totalAssets = total,
                quotaUsageBytes = user?.quotaUsageInBytes,
                quotaSizeBytes = user?.quotaSizeInBytes,
                userName = user?.name ?: user?.email,
            ),
            photos = allPhotos
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

    suspend fun updateAssetFavorite(config: ImmichConnectionConfig, assetId: String, isFavorite: Boolean): Boolean {
        val response = httpClient.put(normalizedBaseUrl(config.baseUrl) + "/api/assets/$assetId") {
            authHeaderProvider.headersFor(config).forEach { (name, value) -> header(name, value) }
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject { put("isFavorite", isFavorite) })
        }
        return response.status.value in 200..299
    }

    suspend fun deleteAsset(config: ImmichConnectionConfig, assetId: String): Boolean {
        val response = httpClient.delete(normalizedBaseUrl(config.baseUrl) + "/api/assets") {
            authHeaderProvider.headersFor(config).forEach { (name, value) -> header(name, value) }
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("ids", kotlinx.serialization.json.buildJsonArray {
                    add(kotlinx.serialization.json.JsonPrimitive(assetId))
                })
            })
        }
        return response.status.value in 200..299
    }

    suspend fun uploadAsset(
        config: ImmichConnectionConfig,
        fileName: String,
        fileBytes: ByteArray,
        mimeType: String = "image/jpeg"
    ): Boolean {
        val deviceId = "moonlight-personal-mobile"
        val deviceAssetId = "$fileName-${fileBytes.size}-${System.currentTimeMillis()}"
        val nowStr = "2026-05-17T12:00:00Z"

        val response = httpClient.post(normalizedBaseUrl(config.baseUrl) + "/api/assets") {
            authHeaderProvider.headersFor(config).forEach { (name, value) -> header(name, value) }
            setBody(MultiPartFormDataContent(
                formData {
                    append("deviceAssetId", deviceAssetId)
                    append("deviceId", deviceId)
                    append("fileCreatedAt", nowStr)
                    append("fileModifiedAt", nowStr)
                    append("isFavorite", "false")
                    append("assetData", fileBytes, Headers.build {
                        append(HttpHeaders.ContentType, mimeType)
                        append(HttpHeaders.ContentDisposition, "form-data; name=\"assetData\"; filename=\"$fileName\"")
                    })
                }
            ))
        }
        return response.status.value in 200..299
    }

    suspend fun getFavorites(config: ImmichConnectionConfig): List<ImmichPhotoAsset> {
        val element = httpClient.get(normalizedBaseUrl(config.baseUrl) + "/api/assets?isFavorite=true") {
            authHeaderProvider.headersFor(config).forEach { (name, value) -> header(name, value) }
            accept(ContentType.Application.Json)
        }.body<JsonArray>()
        
        return element.map { item ->
            val assetResponse = ImmichJson.decodeFromJsonElement<ImmichAssetResponse>(item)
            ImmichPhotoAsset(
                id = assetResponse.id,
                name = assetResponse.originalFileName ?: assetResponse.id,
                thumbnailUrl = thumbnailUrl(config, assetResponse.id),
                createdAt = assetResponse.localDateTime ?: assetResponse.fileCreatedAt,
                location = listOfNotNull(assetResponse.exifInfo?.city, assetResponse.exifInfo?.country).joinToString(", ").ifBlank { null },
                isFavorite = assetResponse.isFavorite,
                isVideo = assetResponse.type.equals("VIDEO", ignoreCase = true) || (assetResponse.originalMimeType?.startsWith("video/") == true),
                isAnimated = (assetResponse.originalMimeType?.lowercase() in setOf("image/gif", "image/webp", "image/apng")),
            )
        }
    }
    suspend fun getPeopleNames(config: ImmichConnectionConfig): List<String> {
        val response = get<JsonElement>(config, "/api/people")
        return when (response) {
            is JsonArray -> response.mapNotNull { entry ->
                val item = entry as? JsonObject ?: return@mapNotNull null
                item["name"]?.jsonPrimitive?.contentOrNull?.trim()?.takeIf { it.isNotBlank() }
            }
            is JsonObject -> {
                val items = response["people"] as? JsonArray ?: JsonArray(emptyList())
                items.mapNotNull { entry ->
                    val item = entry as? JsonObject ?: return@mapNotNull null
                    item["name"]?.jsonPrimitive?.contentOrNull?.trim()?.takeIf { it.isNotBlank() }
                }
            }
            else -> emptyList()
        }.distinct().sorted()
    }

    suspend fun searchAssets(config: ImmichConnectionConfig, page: Int = 1, pageSize: Int = 60): ImmichAssetPage {
        val element = post<ImmichSearchAssetsRequest, JsonElement>(
            config = config,
            path = "/api/search/metadata",
            body = ImmichSearchAssetsRequest(page = page, size = pageSize.coerceIn(1, 1000)),
        )
        logger.debug(tag, "Respuesta raw de /api/search/metadata: $element")
        
        return when {
            element is JsonObject && element.containsKey("assets") -> {
                ImmichJson.decodeFromJsonElement<ImmichSearchAssetsResponse>(element).assets ?: ImmichAssetPage()
            }
            element is JsonObject && element.containsKey("items") -> {
                ImmichJson.decodeFromJsonElement<ImmichAssetPage>(element)
            }
            else -> ImmichAssetPage()
        }
    }

    suspend fun getPeople(config: ImmichConnectionConfig): List<ImmichPersonResponse> {
        val response = httpClient.get(normalizedBaseUrl(config.baseUrl) + "/api/people?withHidden=false") {
            authHeaderProvider.headersFor(config).forEach { (name, value) -> header(name, value) }
            accept(ContentType.Application.Json)
        }.body<JsonArray>()
        
        return response.map { element ->
            ImmichJson.decodeFromJsonElement<ImmichPersonResponse>(element)
        }
    }

    fun personThumbnailUrl(config: ImmichConnectionConfig, personId: String): String = URLBuilder(normalizedBaseUrl(config.baseUrl)).apply {
        appendPathSegments("api", "people", personId, "thumbnail")
    }.buildString()

    suspend fun getImmichPeople(config: ImmichConnectionConfig): List<ImmichPerson> {
        val peopleResponse = getPeople(config)
        return peopleResponse.filter { it.name.trim().isNotBlank() }.map { response ->
            ImmichPerson(
                id = response.id,
                name = response.name,
                thumbnailUrl = personThumbnailUrl(config, response.id)
            )
        }.sortedBy { it.name }
    }


    suspend fun getPersonAssets(config: ImmichConnectionConfig, personId: String): List<ImmichPhotoAsset> {
        val response = httpClient.get(normalizedBaseUrl(config.baseUrl) + "/api/people/$personId/assets") {
            authHeaderProvider.headersFor(config).forEach { (name, value) -> header(name, value) }
            accept(ContentType.Application.Json)
        }.body<JsonArray>()
        
        return response.map { element ->
            val assetResponse = ImmichJson.decodeFromJsonElement<ImmichAssetResponse>(element)
            ImmichPhotoAsset(
                id = assetResponse.id,
                name = assetResponse.originalFileName ?: assetResponse.id,
                thumbnailUrl = thumbnailUrl(config, assetResponse.id),
                createdAt = assetResponse.localDateTime ?: assetResponse.fileCreatedAt,
                location = listOfNotNull(assetResponse.exifInfo?.city, assetResponse.exifInfo?.country).joinToString(", ").ifBlank { null },
                isFavorite = assetResponse.isFavorite,
                isVideo = assetResponse.type.equals("VIDEO", ignoreCase = true) || (assetResponse.originalMimeType?.startsWith("video/") == true),
                isAnimated = (assetResponse.originalMimeType?.lowercase() in setOf("image/gif", "image/webp", "image/apng")),
                fileSizeInByte = assetResponse.exifInfo?.fileSizeInByte,
                make = assetResponse.exifInfo?.make,
                model = assetResponse.exifInfo?.model,
                width = assetResponse.exifInfo?.exifImageWidth,
                height = assetResponse.exifInfo?.exifImageHeight,
                mimeType = assetResponse.originalMimeType,
            )
        }
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
        isVideo = type.equals("VIDEO", ignoreCase = true) || (originalMimeType?.startsWith("video/") == true),
        isAnimated = (originalMimeType?.lowercase() in setOf("image/gif", "image/webp", "image/apng")),
        fileSizeInByte = exifInfo?.fileSizeInByte,
        make = exifInfo?.make,
        model = exifInfo?.model,
        width = exifInfo?.exifImageWidth,
        height = exifInfo?.exifImageHeight,
        mimeType = originalMimeType,
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

suspend fun <T> executeImmichRequest(block: suspend () -> T): T {
    return try {
        block()
    } catch (error: Throwable) {
        throw mapImmichConnectionError(error)
    }
}

fun mapImmichConnectionError(error: Throwable): Throwable {
    return when (error) {
        is ConnectTimeoutException, is HttpRequestTimeoutException -> 
            IllegalStateException("Timeout de conexión con Immich. Revisa la URL y tu red.", error)
        is ClientRequestException -> {
            when (error.response.status.value) {
                401 -> IllegalStateException("401 No autorizado: API Key o token inválido.", error)
                403 -> IllegalStateException("403 Prohibido: No tienes permisos para este recurso.", error)
                else -> error
            }
        }
        is ServerResponseException -> 
            IllegalStateException("Error del servidor Immich (${error.response.status.value}).", error)
        else -> error
    }
}

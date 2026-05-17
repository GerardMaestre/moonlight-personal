package com.limelight.shared.data.remote.repository

import com.limelight.shared.data.immich.ImmichConnectionConfig
import com.limelight.shared.data.remote.dto.AddAssetsToAlbumRequestDto
import com.limelight.shared.data.remote.dto.AlbumDto
import com.limelight.shared.data.remote.dto.CreateAlbumRequestDto
import com.limelight.shared.data.remote.mapper.AlbumMapper
import com.limelight.shared.domain.media.Album
import com.limelight.shared.network.immich.ImmichApiClient
import com.limelight.shared.network.immich.normalizedBaseUrl
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.decodeFromJsonElement

class ImmichAlbumRepository(
    private val config: ImmichConnectionConfig,
    private val httpClient: HttpClient = ImmichApiClient.defaultHttpClient(),
) {
    suspend fun getAlbumAssets(albumId: String): List<com.limelight.shared.data.immich.ImmichPhotoAsset> {
        val response = httpClient.get(normalizedBaseUrl(config.baseUrl) + "/api/albums/$albumId") { applyAuth() }
            .body<kotlinx.serialization.json.JsonObject>()
        
        val assetsArray = response["assets"]?.jsonArray ?: return emptyList()
        val client = ImmichApiClient()
        return assetsArray.map { element ->
            val assetResponse = com.limelight.shared.network.immich.ImmichJson.decodeFromJsonElement<com.limelight.shared.network.immich.ImmichAssetResponse>(element)
            com.limelight.shared.data.immich.ImmichPhotoAsset(
                id = assetResponse.id,
                name = assetResponse.originalFileName ?: assetResponse.id,
                thumbnailUrl = client.thumbnailUrl(config, assetResponse.id),
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
    suspend fun listAlbums(): List<Album> =
        httpClient.post(normalizedBaseUrl(config.baseUrl) + "/api/albums") { applyAuth() }
            .body<List<AlbumDto>>()
            .map(AlbumMapper::toDomain)

    suspend fun createAlbum(name: String): Album =
        httpClient.post(normalizedBaseUrl(config.baseUrl) + "/api/albums") {
            applyAuth()
            setBody(CreateAlbumRequestDto(name))
        }.body<AlbumDto>().let(AlbumMapper::toDomain)

    suspend fun addAssetsToAlbum(albumId: String, assetIds: List<String>) {
        httpClient.post(normalizedBaseUrl(config.baseUrl) + "/api/albums/$albumId/assets") {
            applyAuth()
            setBody(AddAssetsToAlbumRequestDto(assetIds))
        }
    }

    private fun io.ktor.client.request.HttpRequestBuilder.applyAuth() {
        when {
            config.apiKey.isNotBlank() -> header("x-api-key", config.apiKey)
            config.bearerToken.isNotBlank() -> bearerAuth(config.bearerToken)
        }
    }
}

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
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody

class ImmichAlbumRepository(
    private val config: ImmichConnectionConfig,
    private val httpClient: HttpClient = ImmichApiClient.defaultHttpClient(),
) {
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

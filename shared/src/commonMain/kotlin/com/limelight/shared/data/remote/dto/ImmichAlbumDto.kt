package com.limelight.shared.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class AlbumDto(
    val id: String? = null,
    val albumName: String? = null,
    val assetCount: Int? = null,
    val createdAt: String? = null,
)

@Serializable
data class CreateAlbumRequestDto(
    val albumName: String,
)

@Serializable
data class AddAssetsToAlbumRequestDto(
    val ids: List<String>,
)

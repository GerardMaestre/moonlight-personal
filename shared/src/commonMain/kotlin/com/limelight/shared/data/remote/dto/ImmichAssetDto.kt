package com.limelight.shared.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class AssetsResponseDto(
    val count: Int? = null,
    val total: Int? = null,
    val nextPage: String? = null,
    val nextCursor: String? = null,
    val items: List<AssetDto>? = null,
)

@Serializable
data class AssetDto(
    val id: String? = null,
    val type: String? = null,
    val originalFileName: String? = null,
    val originalMimeType: String? = null,
    val fileCreatedAt: String? = null,
    val updatedAt: String? = null,
    val isFavorite: Boolean? = null,
    val exifInfo: ExifInfoDto? = null,
)

@Serializable
data class ExifInfoDto(
    val city: String? = null,
    val country: String? = null,
)

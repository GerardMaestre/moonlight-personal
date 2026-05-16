package com.limelight.shared.network.immich

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ImmichPingResponse(
    val res: String? = null,
    val status: String? = null,
)

@Serializable
data class ImmichAboutResponse(
    val version: String? = null,
    val build: String? = null,
    val licensed: Boolean? = null,
)

@Serializable
data class ImmichVersionResponse(
    val major: Int? = null,
    val minor: Int? = null,
    val patch: Int? = null,
)

@Serializable
data class ImmichSearchAssetsRequest(
    val page: Int = 1,
    val size: Int = 60,
    val order: String = "desc",
    val type: String? = "IMAGE",
    val withExif: Boolean = true,
    val withPeople: Boolean = false,
    val withStacked: Boolean = false,
    val visibility: String? = "timeline",
)

@Serializable
data class ImmichSearchStatisticsRequest(
    val type: String? = null,
    val visibility: String? = "timeline",
)

@Serializable
data class ImmichSearchAssetsResponse(
    val assets: ImmichAssetPage? = null,
)

@Serializable
data class ImmichAssetPage(
    val count: Int = 0,
    val total: Int = 0,
    val nextPage: String? = null,
    val items: List<ImmichAssetResponse> = emptyList(),
)

@Serializable
data class ImmichAssetResponse(
    val id: String,
    val type: String? = null,
    val originalFileName: String? = null,
    val originalMimeType: String? = null,
    val fileCreatedAt: String? = null,
    val localDateTime: String? = null,
    val updatedAt: String? = null,
    val isFavorite: Boolean = false,
    val thumbhash: String? = null,
    val exifInfo: ImmichExifInfo? = null,
)

@Serializable
data class ImmichExifInfo(
    val city: String? = null,
    val country: String? = null,
    val description: String? = null,
    val exifImageHeight: Double? = null,
    val exifImageWidth: Double? = null,
    val fileSizeInByte: Long? = null,
    val make: String? = null,
    val model: String? = null,
)

@Serializable
data class ImmichAssetStatisticsResponse(
    val images: Int = 0,
    val videos: Int = 0,
    val total: Int = 0,
)

@Serializable
data class ImmichUserResponse(
    val id: String,
    val email: String? = null,
    val name: String? = null,
    val profileImagePath: String? = null,
    val quotaSizeInBytes: Long? = null,
    val quotaUsageInBytes: Long? = null,
)

@Serializable
data class ImmichApiError(
    val message: String? = null,
    @SerialName("error") val errorName: String? = null,
    val statusCode: Int? = null,
)

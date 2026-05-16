package com.limelight.shared.data.immich

data class ImmichConnectionConfig(
    val baseUrl: String = "",
    val apiKey: String = "",
    val bearerToken: String = "",
) {
    val hasCredentials: Boolean
        get() = apiKey.isNotBlank() || bearerToken.isNotBlank()
}

data class ImmichServerSummary(
    val version: String = "Desconocida",
    val images: Int = 0,
    val videos: Int = 0,
    val totalAssets: Int = 0,
    val quotaUsageBytes: Long? = null,
    val quotaSizeBytes: Long? = null,
    val userName: String? = null,
)

data class ImmichPhotoAsset(
    val id: String,
    val name: String,
    val thumbnailUrl: String,
    val createdAt: String? = null,
    val location: String? = null,
    val isFavorite: Boolean = false,
)

sealed interface ImmichGalleryState {
    data object Idle : ImmichGalleryState
    data object Loading : ImmichGalleryState
    data class Success(
        val summary: ImmichServerSummary,
        val photos: List<ImmichPhotoAsset>,
    ) : ImmichGalleryState
    data class Error(val message: String) : ImmichGalleryState
}

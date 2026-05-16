package com.limelight.shared.domain.media

enum class AssetType {
    IMAGE,
    VIDEO,
    OTHER,
}

data class Asset(
    val id: String,
    val fileName: String,
    val mimeType: String,
    val createdAtIso: String,
    val updatedAtIso: String?,
    val type: AssetType,
    val isFavorite: Boolean,
    val city: String?,
    val country: String?,
)

data class Album(
    val id: String,
    val name: String,
    val assetCount: Int,
    val createdAtIso: String,
)

data class SearchQuery(
    val text: String? = null,
    val fromDateIso: String? = null,
    val toDateIso: String? = null,
    val type: AssetType? = null,
    val person: String? = null,
    val objectLabel: String? = null,
    val page: Int = 1,
    val size: Int = 60,
)

data class TimelinePage(
    val items: List<Asset>,
    val nextPage: Int?,
    val nextCursor: String?,
    val total: Int,
)

data class SearchResult(
    val page: TimelinePage,
)

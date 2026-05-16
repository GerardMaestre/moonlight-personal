package com.limelight.shared.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class SearchAssetsRequestDto(
    val page: Int = 1,
    val size: Int = 60,
    val query: String? = null,
    val takenAfter: String? = null,
    val takenBefore: String? = null,
    val type: String? = null,
    val person: String? = null,
    val withArchived: Boolean = false,
    val `object`: String? = null,
)

@Serializable
data class SearchResponseDto(
    val assets: AssetsResponseDto? = null,
)

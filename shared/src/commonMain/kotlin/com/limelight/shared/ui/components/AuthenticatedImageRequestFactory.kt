package com.limelight.shared.ui.components

import coil3.PlatformContext
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.size.Size
import com.limelight.shared.data.immich.ImmichConnectionConfig

class AuthenticatedImageRequestFactory {
    fun buildThumbnailUrl(baseUrl: String, assetId: String): String {
        val normalizedBaseUrl = baseUrl.trim().trimEnd('/')
        return "$normalizedBaseUrl/api/assets/$assetId/thumbnail?size=thumbnail"
    }

    fun buildThumbnailRequest(
        context: PlatformContext,
        config: ImmichConnectionConfig,
        assetId: String,
        targetSizePx: Int,
    ): ImageRequest {
        val headers = NetworkHeaders.Builder().apply {
            when {
                config.apiKey.isNotBlank() -> set("x-api-key", config.apiKey)
                config.bearerToken.isNotBlank() -> set("Authorization", "Bearer ${config.bearerToken}")
            }
        }.build()

        val safeSize = targetSizePx.coerceIn(96, 1024)
        return ImageRequest.Builder(context)
            .data(buildThumbnailUrl(config.baseUrl, assetId))
            .httpHeaders(headers)
            .size(Size(safeSize, safeSize))
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
    }
}

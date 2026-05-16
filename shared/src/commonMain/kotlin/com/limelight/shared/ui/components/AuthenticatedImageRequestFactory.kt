package com.limelight.shared.ui.components

import coil3.PlatformContext
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.size.Size
import com.limelight.shared.data.immich.ImmichConnectionConfig
import com.limelight.shared.data.session.AuthHeaderProvider

class AuthenticatedImageRequestFactory(
    private val authHeaderProvider: AuthHeaderProvider = AuthHeaderProvider(),
) {
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
            authHeaderProvider.headersFor(config).forEach { (name, value) ->
                set(name, value)
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

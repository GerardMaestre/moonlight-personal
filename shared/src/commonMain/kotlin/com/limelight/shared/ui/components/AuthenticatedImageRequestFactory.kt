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
    fun buildThumbnailUrl(baseUrl: String, assetId: String, size: String = "thumbnail"): String {
        val normalizedBaseUrl = baseUrl.trim().trimEnd('/')
        return "$normalizedBaseUrl/api/assets/$assetId/thumbnail?size=$size"
    }

    fun buildOriginalUrl(baseUrl: String, assetId: String): String {
        val normalizedBaseUrl = baseUrl.trim().trimEnd('/')
        return "$normalizedBaseUrl/api/assets/$assetId/original"
    }

    fun buildThumbnailRequest(
        context: PlatformContext,
        config: ImmichConnectionConfig,
        assetId: String,
        targetSizePx: Int,
        highQuality: Boolean = false
    ): ImageRequest {
        val headers = NetworkHeaders.Builder().apply {
            authHeaderProvider.headersFor(config).forEach { (name, value) ->
                set(name, value)
            }
        }.build()

        val url = if (highQuality) {
            buildThumbnailUrl(config.baseUrl, assetId, size = "preview")
        } else {
            buildThumbnailUrl(config.baseUrl, assetId, size = "thumbnail")
        }

        return ImageRequest.Builder(context)
            .data(url)
            .httpHeaders(headers)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
    }

    fun buildOriginalRequest(
        context: PlatformContext,
        config: ImmichConnectionConfig,
        assetId: String,
    ): ImageRequest {
        val headers = NetworkHeaders.Builder().apply {
            authHeaderProvider.headersFor(config).forEach { (name, value) ->
                set(name, value)
            }
        }.build()

        return ImageRequest.Builder(context)
            .data(buildOriginalUrl(config.baseUrl, assetId))
            .httpHeaders(headers)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
    }

    fun buildPeopleFaceRequest(
        context: PlatformContext,
        config: ImmichConnectionConfig,
        personId: String
    ): ImageRequest {
        val headers = NetworkHeaders.Builder().apply {
            authHeaderProvider.headersFor(config).forEach { (name, value) ->
                set(name, value)
            }
        }.build()

        val normalizedBaseUrl = config.baseUrl.trim().trimEnd('/')
        val url = "$normalizedBaseUrl/api/people/$personId/thumbnail"

        return ImageRequest.Builder(context)
            .data(url)
            .httpHeaders(headers)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
    }
}

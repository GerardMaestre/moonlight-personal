package com.limelight.shared.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.compose.AsyncImagePainter
import com.limelight.shared.data.immich.ImmichConnectionConfig
import com.limelight.shared.ui.theme.MoonlightColors

@Composable
fun ThumbnailImage(
    assetId: String,
    contentDescription: String?,
    config: ImmichConnectionConfig,
    cellSize: Dp,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    highQuality: Boolean = false
) {
    val context = LocalPlatformContext.current
    val factory = remember { AuthenticatedImageRequestFactory() }
    val request = remember(config, assetId, cellSize, highQuality) {
        if (highQuality) {
            factory.buildOriginalRequest(context, config, assetId)
        } else {
            factory.buildThumbnailRequest(context, config, assetId, (cellSize.value * 2).toInt())
        }
    }

    var isSuccess by remember { mutableStateOf(false) }
    var isError by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(MoonlightColors.SurfaceContainerHighest),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = request,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            onState = { state ->
                isSuccess = state is AsyncImagePainter.State.Success
                isError = state is AsyncImagePainter.State.Error
            },
            modifier = Modifier.fillMaxSize()
        )

        // Ultra-lightweight background shimmer drawn ONLY when loading
        if (!isSuccess && !isError) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.08f))
            )
        }

        if (isError) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.BrokenImage,
                    contentDescription = null,
                    tint = MoonlightColors.OnSurfaceVariant
                )
            }
        }
    }
}

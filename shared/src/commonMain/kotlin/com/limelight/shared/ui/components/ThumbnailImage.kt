package com.limelight.shared.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import com.limelight.shared.data.immich.ImmichConnectionConfig
import com.limelight.shared.ui.theme.MoonlightColors

@Composable
fun ThumbnailImage(assetId: String, contentDescription: String?, config: ImmichConnectionConfig, cellSize: Dp, modifier: Modifier = Modifier, cornerRadius: Dp = 24.dp) {
    val context = LocalPlatformContext.current
    val request = AuthenticatedImageRequestFactory().buildThumbnailRequest(context, config, assetId, (cellSize.value * 2).toInt())
    val transition = rememberInfiniteTransition(label = "thumbShimmer")
    val shimmerAlpha by transition.animateFloat(0.35f, 0.75f, infiniteRepeatable(animation = tween(900), repeatMode = RepeatMode.Reverse), label = "thumbShimmerAlpha")

    SubcomposeAsyncImage(
        model = request,
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier.clip(RoundedCornerShape(cornerRadius)).background(MoonlightColors.SurfaceContainerHighest),
        loading = {
            Box(Modifier.fillMaxSize().alpha(shimmerAlpha).background(Color.White.copy(alpha = 0.14f)))
        },
        error = {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.BrokenImage, contentDescription = null, tint = MoonlightColors.OnSurfaceVariant)
            }
        },
    )
}

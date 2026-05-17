package com.limelight.shared.ui.components

import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.limelight.shared.data.immich.ImmichConnectionConfig

@Composable
fun ZoomableThumbnailImage(
    assetId: String,
    contentDescription: String?,
    config: ImmichConnectionConfig,
    cellSize: Dp = 800.dp,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    val state = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale *= zoomChange
        offset += offsetChange
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer(
                scaleX = scale.coerceIn(1f, 5f),
                scaleY = scale.coerceIn(1f, 5f),
                translationX = offset.x,
                translationY = offset.y
            )
            .transformable(state = state)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        scale = if (scale > 1f) 1f else 3f
                        offset = androidx.compose.ui.geometry.Offset.Zero
                    }
                )
            }
    ) {
        ThumbnailImage(
            assetId = assetId,
            contentDescription = contentDescription,
            config = config,
            cellSize = cellSize,
            modifier = Modifier.fillMaxSize(),
            cornerRadius = 0.dp,
            highQuality = true
        )
    }
}

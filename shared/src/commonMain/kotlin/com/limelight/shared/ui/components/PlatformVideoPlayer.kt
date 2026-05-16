package com.limelight.shared.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.limelight.shared.data.immich.ImmichConnectionConfig

@Composable
expect fun PlatformVideoPlayer(
    streamingUrl: String,
    authConfig: ImmichConnectionConfig,
    isPlaying: Boolean,
    onDurationKnown: (Long) -> Unit,
    onPositionChanged: (Long) -> Unit,
    modifier: Modifier = Modifier,
)

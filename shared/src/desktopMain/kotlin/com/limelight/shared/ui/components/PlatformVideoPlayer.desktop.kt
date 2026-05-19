package com.limelight.shared.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.limelight.shared.data.immich.ImmichConnectionConfig

@Composable
actual fun PlatformVideoPlayer(
    streamingUrl: String,
    authConfig: ImmichConnectionConfig,
    isPlaying: Boolean,
    onDurationKnown: (Long) -> Unit,
    onPositionChanged: (Long) -> Unit,
    seekPosition: Long,
    modifier: Modifier
) {
    onDurationKnown(1)
    onPositionChanged(if (isPlaying) 1 else 0)
    Text("Video streaming: $streamingUrl", modifier = modifier)
}

package com.limelight.shared.ui.components

import android.widget.VideoView
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.limelight.shared.data.immich.ImmichConnectionConfig

@Composable
actual fun PlatformVideoPlayer(
    streamingUrl: String,
    authConfig: ImmichConnectionConfig,
    isPlaying: Boolean,
    onDurationKnown: (Long) -> Unit,
    onPositionChanged: (Long) -> Unit,
    modifier: Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            VideoView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                
                val headers = mutableMapOf<String, String>()
                if (authConfig.apiKey.isNotBlank()) {
                    headers["x-api-key"] = authConfig.apiKey
                }
                if (authConfig.bearerToken.isNotBlank()) {
                    headers["Authorization"] = "Bearer ${authConfig.bearerToken}"
                }
                
                try {
                    setVideoURI(Uri.parse(streamingUrl), headers)
                } catch (e: Exception) {
                    println("Error setting video URI: ${e.message}")
                }
                
                setOnPreparedListener { mp ->
                    onDurationKnown(mp.duration.toLong())
                    mp.isLooping = true
                    if (isPlaying) start()
                }
                
                setOnErrorListener { _, what, extra ->
                    println("VideoView error: what=$what, extra=$extra")
                    true // Prevents showing the error dialog which can crash apps
                }
            }
        },
        update = { view ->
            try {
                if (isPlaying) {
                    if (!view.isPlaying) view.start()
                } else {
                    if (view.isPlaying) view.pause()
                }
            } catch (e: Exception) {
                // Ignore update errors
            }
        },
        onRelease = { view ->
            view.stopPlayback()
        }
    )
}

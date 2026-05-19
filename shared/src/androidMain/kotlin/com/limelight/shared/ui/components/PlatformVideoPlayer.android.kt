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
    seekPosition: Long,
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
                
                val progressRunnable = object : Runnable {
                    override fun run() {
                        try {
                            if (this@apply.isPlaying) {
                                onPositionChanged(this@apply.currentPosition.toLong())
                            }
                        } catch (e: Exception) {}
                        postDelayed(this, 250)
                    }
                }
                
                setOnPreparedListener { mp ->
                    onDurationKnown(mp.duration.toLong())
                    mp.isLooping = true
                    if (isPlaying) start()
                    post(progressRunnable)
                }
                
                setOnErrorListener { _, what, extra ->
                    println("VideoView error: what=$what, extra=$extra")
                    true // Prevents showing the error dialog which can crash apps
                }
                
                addOnAttachStateChangeListener(object : android.view.View.OnAttachStateChangeListener {
                    override fun onViewAttachedToWindow(v: android.view.View) {}
                    override fun onViewDetachedFromWindow(v: android.view.View) {
                        removeCallbacks(progressRunnable)
                    }
                })
            }
        },
        update = { view ->
            try {
                if (isPlaying) {
                    if (!view.isPlaying) view.start()
                } else {
                    if (view.isPlaying) view.pause()
                }
                
                if (seekPosition >= 0L) {
                    val currentPos = view.currentPosition.toLong()
                    if (Math.abs(currentPos - seekPosition) > 1000) {
                        view.seekTo(seekPosition.toInt())
                    }
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

package com.limelight.shared.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
actual fun PlatformImmichGallery(
    url: String,
    engineId: String,
    modifier: Modifier,
    onBackAvailable: (Boolean) -> Unit,
    backTrigger: Boolean,
    onBackHandled: () -> Unit,
    onTokenAcquired: (String) -> Unit
) {
    // Detect runtime presence of the FlutterView class (only resolved when the AAR is bundled)
    val isFlutterAvailable = try {
        Class.forName("io.flutter.embedding.android.FlutterView")
        true
    } catch (e: Exception) {
        false
    }

    if (isFlutterAvailable) {
        // Embed the high-performance FlutterView directly inside Jetpack Compose
        AndroidView<android.view.View>(
            factory = { context: android.content.Context ->
                val flutterView = io.flutter.embedding.android.FlutterView(context)
                val flutterEngine = io.flutter.embedding.engine.FlutterEngineCache.getInstance().get(engineId)
                if (flutterEngine != null) {
                    flutterView.attachToFlutterEngine(flutterEngine)
                }
                flutterView
            },
            modifier = modifier
        )
    } else {
        // Fallback: Embed the HTML5 platform gallery directly within the app hierarchy
        PlatformWebView(
            url = url,
            modifier = modifier,
            onBackAvailable = onBackAvailable,
            backTrigger = backTrigger,
            onBackHandled = onBackHandled,
            onTokenAcquired = onTokenAcquired
        )
    }
}

package com.limelight.shared.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

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
    // On Desktop, embed the WebView to view the gallery directly within the desktop UI
    PlatformWebView(
        url = url,
        modifier = modifier,
        onBackAvailable = onBackAvailable,
        backTrigger = backTrigger,
        onBackHandled = onBackHandled,
        onTokenAcquired = onTokenAcquired
    )
}

package com.limelight.shared.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * A platform-agnostic Jetpack Compose component that embeds the Immich multimedia gallery.
 * - On Android: If the Flutter SDK AAR is present, it embeds the native [FlutterView] directly
 *   into the Compose layout without any activity transitions. Otherwise, it falls back to the fully-embedded webview.
 * - On Desktop: It embeds the Web-based platform gallery directly.
 */
@Composable
expect fun PlatformImmichGallery(
    url: String,
    engineId: String,
    modifier: Modifier = Modifier,
    onBackAvailable: (Boolean) -> Unit = {},
    backTrigger: Boolean = false,
    onBackHandled: () -> Unit = {},
    onTokenAcquired: (String) -> Unit = {}
)

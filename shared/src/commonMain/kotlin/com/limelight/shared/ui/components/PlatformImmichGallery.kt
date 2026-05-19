package com.limelight.shared.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * A platform-agnostic Jetpack Compose component that embeds the Immich multimedia gallery.
 * - On Android: It embeds the HTML5 gallery directly into the Compose hierarchy.
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

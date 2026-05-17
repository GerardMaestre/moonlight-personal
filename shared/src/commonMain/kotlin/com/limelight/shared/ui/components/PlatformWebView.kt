package com.limelight.shared.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun PlatformWebView(
    url: String,
    modifier: Modifier = Modifier,
    onBackAvailable: (Boolean) -> Unit = {},
    backTrigger: Boolean = false,
    onBackHandled: () -> Unit = {}
)

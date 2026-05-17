package com.limelight.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.limelight.shared.platform.PhotoServerActions
import com.limelight.shared.platform.PhotoServerState
import com.limelight.shared.ui.components.*
import com.limelight.shared.ui.theme.MoonlightColors

@Composable
fun ImmichHomeScreen(
    state: PhotoServerState,
    actions: PhotoServerActions,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit
) {
    var webViewCanGoBack by remember { mutableStateOf(false) }
    var triggerBack by remember { mutableStateOf(false) }

    val baseUrl = state.connectionConfig.baseUrl.trim()
    val isValidUrl = baseUrl.startsWith("http://") || baseUrl.startsWith("https://")

    // Handle system hardware back press
    PlatformBackHandler(enabled = true) {
        if (webViewCanGoBack) {
            triggerBack = true
        } else {
            onBack()
        }
    }

    AetherisScreen {
        Box(modifier = Modifier.fillMaxSize()) {
            if (isValidUrl) {
                PlatformWebView(
                    url = baseUrl,
                    modifier = Modifier.fillMaxSize().background(Color.Black),
                    onBackAvailable = { webViewCanGoBack = it },
                    backTrigger = triggerBack,
                    onBackHandled = { triggerBack = false }
                )

                // Sleek floating glassmorphic Back button
                Box(
                    modifier = Modifier
                        .padding(top = 48.dp, start = 16.dp)
                        .align(Alignment.TopStart)
                ) {
                    IconButton(
                        onClick = {
                            if (webViewCanGoBack) {
                                triggerBack = true
                            } else {
                                onBack()
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .padding(top = 48.dp, end = 16.dp)
                        .align(Alignment.TopEnd)
                ) {
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Ajustes",
                            tint = Color.White
                        )
                    }
                }
            } else {
                // Warning if URL is empty or invalid
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    GlassCard(contentPadding = PaddingValues(24.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Configuración requerida", style = MaterialTheme.typography.headlineMedium, color = MoonlightColors.OnSurface)
                            Text(
                                "Para acceder a la galería, debes configurar una URL base válida en el panel de control del servidor.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MoonlightColors.OnSurfaceVariant,
                                textAlign = TextAlign.Center,
                                maxLines = 4
                            )
                            Spacer(Modifier.height(8.dp))
                            PrimaryGlassButton(
                                text = "Ir a Configuración",
                                icon = Icons.Default.Settings,
                                onClick = onOpenSettings,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

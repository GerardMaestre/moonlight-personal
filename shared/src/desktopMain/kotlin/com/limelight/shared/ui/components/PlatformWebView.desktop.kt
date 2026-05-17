package com.limelight.shared.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limelight.shared.ui.theme.MoonlightColors
import java.awt.Desktop
import java.net.URI

@Composable
actual fun PlatformWebView(
    url: String,
    modifier: Modifier,
    onBackAvailable: (Boolean) -> Unit,
    backTrigger: Boolean,
    onBackHandled: () -> Unit,
    onTokenAcquired: (String) -> Unit
) {
    // Reset back triggers immediately
    LaunchedEffect(backTrigger) {
        if (backTrigger) {
            onBackHandled()
        }
    }

    LaunchedEffect(Unit) {
        onBackAvailable(false)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MoonlightColors.Background),
        contentAlignment = Alignment.Center
    ) {
        GlassCard(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth(0.9f)
                .padding(24.dp),
            contentPadding = PaddingValues(32.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header Icon
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MoonlightColors.PrimaryContainer,
                                    MoonlightColors.SecondaryContainer
                                )
                            )
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Title
                Text(
                    text = "Immich Web Portal",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MoonlightColors.OnSurface,
                    textAlign = TextAlign.Center
                )

                // Status Pill
                StatusPill(
                    text = "DISPONIBLE EN ESCRITORIO",
                    color = MoonlightColors.PrimaryFixedDim
                )

                // Info Box
                GlassPanel(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Dirección del Servidor:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MoonlightColors.OnSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = url,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MoonlightColors.OnSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Description
                Text(
                    text = "Para garantizar la máxima velocidad, estabilidad y seguridad en tu ordenador, Moonlight abre el portal de Immich directamente en tu navegador web predeterminado. Esto te permite disfrutar de la experiencia completa con aceleración gráfica por hardware y soporte nativo de Web Crypto.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MoonlightColors.OnSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Actions
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PrimaryGlassButton(
                        text = "Abrir en el Navegador",
                        icon = Icons.Default.OpenInNew,
                        onClick = {
                            try {
                                if (Desktop.isDesktopSupported()) {
                                    Desktop.getDesktop().browse(URI(url))
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            try {
                                val clipboard = java.awt.Toolkit.getDefaultToolkit().systemClipboard
                                val selection = java.awt.datatransfer.StringSelection(url)
                                clipboard.setContents(selection, selection)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.05f),
                            contentColor = MoonlightColors.OnSurface
                        ),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null)
                        Spacer(Modifier.width(10.dp))
                        Text("COPIAR ENLACE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                    }
                }
            }
        }
    }
}

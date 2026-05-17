package com.limelight.shared.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.limelight.shared.platform.PhotoServerActions
import com.limelight.shared.platform.PhotoServerState
import com.limelight.shared.platform.PhotoServerStatus
import com.limelight.shared.platform.PreviewPhotoServerActions
import com.limelight.shared.ui.components.AetherisScreen
import com.limelight.shared.ui.components.GlassCard
import com.limelight.shared.ui.components.HomeHubTopBar
import com.limelight.shared.ui.theme.MoonlightColors

@Composable
fun PhotoServerScreen(
    state: PhotoServerState,
    actions: PhotoServerActions = PreviewPhotoServerActions,
    onBack: () -> Unit,
    onOpenImmich: () -> Unit,
) {
    val isOnline = state.status is PhotoServerStatus.Running
    val statusColor = when (state.status) {
        PhotoServerStatus.Stopped -> MoonlightColors.Outline
        PhotoServerStatus.Starting -> MoonlightColors.Tertiary
        is PhotoServerStatus.Running -> MoonlightColors.Tertiary
        is PhotoServerStatus.Error -> MoonlightColors.Error
    }

    AetherisScreen(primaryGlowAlignment = Alignment.TopStart, secondaryGlowAlignment = Alignment.BottomEnd) {
        Scaffold(topBar = { HomeHubTopBar(onBack = onBack) }, containerColor = Color.Transparent) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                GlassCard(contentPadding = PaddingValues(24.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("Servidor multimedia", style = MaterialTheme.typography.headlineMedium, color = MoonlightColors.OnSurface)
                        Text(state.healthMessage, style = MaterialTheme.typography.bodyLarge, color = statusColor, textAlign = TextAlign.Center)
                        Text("Comprobación de red con ImmichHealthChecker", style = MaterialTheme.typography.bodySmall, color = MoonlightColors.OnSurfaceVariant, textAlign = TextAlign.Center)
                        state.lastError?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = MoonlightColors.Error, textAlign = TextAlign.Center)
                        }

                        Spacer(Modifier.height(8.dp))
                        androidx.compose.material3.Button(
                            onClick = onOpenImmich,
                            enabled = isOnline,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(999.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MoonlightColors.Tertiary,
                                disabledContainerColor = MoonlightColors.Tertiary.copy(alpha = 0.35f),
                                contentColor = MoonlightColors.OnPrimary,
                                disabledContentColor = MoonlightColors.OnPrimary.copy(alpha = 0.6f),
                            ),
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                            Spacer(Modifier.height(0.dp))
                            Text("Abrir Galería")
                        }
                    }
                }
            }
        }
    }
}

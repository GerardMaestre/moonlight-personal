package com.limelight.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limelight.shared.platform.PhotoServerActions
import com.limelight.shared.platform.PhotoServerState
import com.limelight.shared.platform.PhotoServerStatus
import com.limelight.shared.platform.PreviewPhotoServerActions
import com.limelight.shared.ui.components.*
import com.limelight.shared.ui.theme.MoonlightColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoServerScreen(
    state: PhotoServerState,
    actions: PhotoServerActions = PreviewPhotoServerActions,
    onBack: () -> Unit,
) {
    AetherisScreen(primaryGlowAlignment = Alignment.TopStart, secondaryGlowAlignment = Alignment.BottomEnd) {
        Scaffold(topBar = { HomeHubTopBar(onBack = onBack) }, containerColor = Color.Transparent) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Multimedia", style = MaterialTheme.typography.headlineLarge.copy(fontSize = 42.sp), color = MoonlightColors.OnSurface, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text("Servidor de sincronización de fotos Immich con respaldo automático.", style = MaterialTheme.typography.bodyLarge, color = MoonlightColors.OnSurfaceVariant, textAlign = TextAlign.Center)
                Spacer(Modifier.height(24.dp))

                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    if (maxWidth > 840.dp) {
                        Row(horizontalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.fillMaxWidth()) {
                            ControlCard(state, actions, Modifier.weight(2f))
                            Column(verticalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.weight(1f)) {
                                MetricCard("Almacenamiento", "1.2 TB", "/ 4 TB", "30% Utilizado", Icons.Default.Storage, 0.30f, MoonlightColors.PrimaryContainer)
                                MetricCard("Sincronización", "14,392", "Fotos", "Última sync hace 2 min", Icons.Default.CloudSync, 1f, MoonlightColors.Tertiary)
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(18.dp), modifier = Modifier.fillMaxWidth()) {
                            ControlCard(state, actions)
                            MetricCard("Almacenamiento", "1.2 TB", "/ 4 TB", "30% Utilizado", Icons.Default.Storage, 0.30f, MoonlightColors.PrimaryContainer)
                            MetricCard("Sincronización", "14,392", "Fotos", "Última sync hace 2 min", Icons.Default.CloudSync, 1f, MoonlightColors.Tertiary)
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                ActivityCard(state.recentLogs)
                Spacer(Modifier.height(110.dp))
            }
        }
    }
}

@Composable
private fun ControlCard(state: PhotoServerState, actions: PhotoServerActions, modifier: Modifier = Modifier) {
    val running = state.status is PhotoServerStatus.Running
    val starting = state.status == PhotoServerStatus.Starting
    val accent = when (state.status) {
        PhotoServerStatus.Stopped -> MoonlightColors.Outline
        PhotoServerStatus.Starting -> MoonlightColors.Tertiary
        is PhotoServerStatus.Running -> MoonlightColors.Tertiary
        is PhotoServerStatus.Error -> MoonlightColors.Error
    }
    GlassCard(modifier = modifier.fillMaxWidth(), contentPadding = PaddingValues(28.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(190.dp)) {
                Box(Modifier.size(188.dp).clip(CircleShape).border(1.dp, MoonlightColors.Primary.copy(alpha = 0.24f), CircleShape).background(MoonlightColors.SurfaceContainerHighest.copy(alpha = 0.35f)))
                Box(Modifier.size(150.dp).clip(CircleShape).border(1.dp, MoonlightColors.Tertiary.copy(alpha = 0.22f), CircleShape))
                Icon(if (running) Icons.Default.CloudDone else if (starting) Icons.Default.CloudSync else Icons.Default.CloudOff, null, tint = accent, modifier = Modifier.size(78.dp))
            }
            Spacer(Modifier.height(18.dp))
            StatusPill(if (running) "Immich Core" else if (starting) "Arrancando" else "Standby", accent)
            Spacer(Modifier.height(12.dp))
            Text(if (running) "Servidor Activo" else "Servidor Multimedia", style = MaterialTheme.typography.headlineLarge, color = MoonlightColors.OnSurface)
            Spacer(Modifier.height(8.dp))
            Text(state.healthMessage, style = MaterialTheme.typography.bodyMedium, color = MoonlightColors.OnSurfaceVariant, textAlign = TextAlign.Center)
            state.lastError?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, color = MoonlightColors.Error, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                PrimaryGlassButton(if (running) "Activo" else "Arrancar", Icons.Default.PowerSettingsNew, { actions.startPhotoServer() }, Modifier.weight(1f), enabled = !starting)
                ErrorGlassButton("Apagar", Icons.Default.StopCircle, { actions.stopPhotoServer() }, Modifier.weight(1f), enabled = running || state.status is PhotoServerStatus.Error)
            }
            if (running || state.status is PhotoServerStatus.Error) {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = { actions.restartPhotoServer() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(999.dp)) {
                    Icon(Icons.Default.Refresh, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Reiniciar")
                }
            }
        }
    }
}

@Composable
private fun MetricCard(title: String, value: String, suffix: String, footer: String, icon: androidx.compose.ui.graphics.vector.ImageVector, progress: Float, color: Color) {
    GlassCard(contentPadding = PaddingValues(22.dp), modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column {
                    Text(title.uppercase(), style = MaterialTheme.typography.labelSmall, color = MoonlightColors.OnSurfaceVariant, letterSpacing = 1.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(value, style = MaterialTheme.typography.headlineMedium, color = MoonlightColors.OnSurface)
                    Text(suffix, style = MaterialTheme.typography.bodyMedium, color = MoonlightColors.Outline)
                }
                Icon(icon, null, tint = color, modifier = Modifier.size(34.dp))
            }
            Spacer(Modifier.height(18.dp))
            Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(999.dp)).background(MoonlightColors.SurfaceContainerHighest)) {
                Box(Modifier.fillMaxWidth(progress).fillMaxHeight().clip(RoundedCornerShape(999.dp)).background(color))
            }
            Spacer(Modifier.height(8.dp))
            Text(footer, style = MaterialTheme.typography.labelSmall, color = if (progress >= 1f) MoonlightColors.Tertiary else MoonlightColors.Outline, modifier = Modifier.align(Alignment.End))
        }
    }
}

@Composable
private fun ActivityCard(logs: List<String>) {
    GlassCard(contentPadding = PaddingValues(22.dp), modifier = Modifier.fillMaxWidth()) {
        Column {
            Text("Actividad Reciente", style = MaterialTheme.typography.headlineMedium, color = MoonlightColors.OnSurface)
            Spacer(Modifier.height(4.dp))
            Text("Archivos indexados recientemente", style = MaterialTheme.typography.bodyMedium, color = MoonlightColors.OnSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                listOf("JPG", "MP4", "HEIC", "JPG", "RAW").forEachIndexed { index, type -> GalleryTile(type, index) }
            }
            if (logs.isNotEmpty()) {
                Spacer(Modifier.height(18.dp))
                logs.takeLast(3).forEach { Text("> $it", color = MoonlightColors.OnSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 1) }
            }
        }
    }
}

@Composable
private fun GalleryTile(type: String, index: Int) {
    val colors = listOf(MoonlightColors.Primary, MoonlightColors.Tertiary, MoonlightColors.Secondary, MoonlightColors.PrimaryContainer, MoonlightColors.Error)
    Box(Modifier.size(148.dp).clip(RoundedCornerShape(24.dp)).background(MoonlightColors.SurfaceContainerHighest).border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(24.dp))) {
        Box(Modifier.matchParentSize().background(Brush.radialGradient(listOf(colors[index % colors.size].copy(alpha = 0.24f), Color.Transparent))))
        Text(type, modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp).clip(RoundedCornerShape(8.dp)).background(MoonlightColors.Surface.copy(alpha = 0.82f)).padding(horizontal = 8.dp, vertical = 4.dp), color = MoonlightColors.OnSurface, style = MaterialTheme.typography.labelSmall)
    }
}

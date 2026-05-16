package com.limelight.shared.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import com.limelight.shared.platform.StartCommandResult
import com.limelight.shared.ui.components.*
import com.limelight.shared.ui.theme.MoonlightColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoServerScreen(
    state: PhotoServerState,
    actions: PhotoServerActions = PreviewPhotoServerActions,
    onBack: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize().background(MoonlightColors.Background)) {
        // Background Glows
        AetherisGlow(
            modifier = Modifier.align(Alignment.TopStart).offset(x = (-100).dp, y = (-100).dp),
            color = MoonlightColors.Tertiary
        )
        AetherisGlow(
            modifier = Modifier.align(Alignment.CenterEnd).offset(x = 150.dp, y = 0.dp),
            color = MoonlightColors.Primary
        )

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "MEDIA HUB",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = MoonlightColors.OnSurface)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MoonlightColors.OnSurface,
                    )
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                
                // Icon and Status Header
                StatusHeader(state.status)

                Spacer(modifier = Modifier.height(32.dp))

                // Main Info Card
                InfoCard(state)

                Spacer(modifier = Modifier.height(24.dp))

                // Logs Section
                if (state.recentLogs.isNotEmpty()) {
                    LogsCard(state.recentLogs)
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Actions Section
                ActionButtons(state.status, actions)
                
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
private fun StatusHeader(status: PhotoServerStatus) {
    val (color, icon, text) = when (status) {
        PhotoServerStatus.Stopped -> Triple(MoonlightColors.Outline, Icons.Default.CloudOff, "DISCONNECTED")
        PhotoServerStatus.Starting -> Triple(MoonlightColors.Secondary, Icons.Default.CloudSync, "WAKING UP...")
        is PhotoServerStatus.Running -> Triple(MoonlightColors.Primary, Icons.Default.CloudDone, "SYSTEM ACTIVE")
        is PhotoServerStatus.Error -> Triple(MoonlightColors.Error, Icons.Default.Error, "CONNECTION ERROR")
    }

    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(color.copy(alpha = 0.05f))
                .border(1.dp, color.copy(alpha = 0.1f), RoundedCornerShape(32.dp))
        )
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = color
        )
    }
    
    Spacer(modifier = Modifier.height(20.dp))
    
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = color,
        letterSpacing = 1.sp
    )
}

@Composable
private fun InfoCard(state: PhotoServerState) {
    GlassCard {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = MoonlightColors.Primary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("SERVER INFRASTRUCTURE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MoonlightColors.OnSurfaceVariant)
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            DetailRow("Backend Service", "Immich Engine")
            DetailRow("Network Status", state.healthMessage)
            
            if (state.status is PhotoServerStatus.Running) {
                val s = state.status as PhotoServerStatus.Running
                DetailRow("Host Port", s.port.toString())
                DetailRow("Virtual IP", s.url)
            }
            
            state.lastError?.let {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    it,
                    color = MoonlightColors.Error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MoonlightColors.Error.copy(alpha = 0.05f))
                        .padding(12.dp)
                )
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MoonlightColors.OnSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MoonlightColors.OnSurface)
    }
}

@Composable
private fun LogsCard(logs: List<String>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.Black.copy(alpha = 0.4f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("LIVE TERMINAL LOGS", style = MaterialTheme.typography.labelSmall, color = MoonlightColors.Primary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            logs.takeLast(4).forEach { log ->
                Text(
                    "> $log",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray.copy(alpha = 0.7f),
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    modifier = Modifier.padding(vertical = 3.dp),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun ActionButtons(status: PhotoServerStatus, actions: PhotoServerActions) {
    val isRunning = status is PhotoServerStatus.Running
    val isStarting = status == PhotoServerStatus.Starting

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Button(
            onClick = { actions.startPhotoServer() },
            modifier = Modifier.fillMaxWidth().height(60.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRunning) MoonlightColors.Surface else MoonlightColors.Primary,
                contentColor = if (isRunning) MoonlightColors.OnSurface else MoonlightColors.OnPrimaryContainer
            ),
            enabled = !isStarting
        ) {
            if (isStarting) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MoonlightColors.OnPrimaryContainer, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Text("PROVISIONING DOCKER...", fontWeight = FontWeight.Bold)
            } else {
                Icon(if (isRunning) Icons.Default.CheckCircle else Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(12.dp))
                Text(if (isRunning) "IMMICH IS RUNNING" else "BOOT IMMICH SERVER", fontWeight = FontWeight.Bold)
            }
        }

        if (isRunning || status is PhotoServerStatus.Error) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedButton(
                    onClick = { actions.restartPhotoServer() },
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MoonlightColors.Outline.copy(alpha = 0.3f))
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = MoonlightColors.OnSurface)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("RESTART", fontWeight = FontWeight.Bold, color = MoonlightColors.OnSurface)
                }
                
                Button(
                    onClick = { actions.stopPhotoServer() },
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MoonlightColors.Error.copy(alpha = 0.1f), contentColor = MoonlightColors.Error)
                ) {
                    Icon(Icons.Default.PowerSettingsNew, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SHUTDOWN", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

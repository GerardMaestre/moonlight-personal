package com.limelight.shared.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import com.limelight.shared.ui.theme.MoonlightColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoServerScreen(
    state: PhotoServerState,
    actions: PhotoServerActions = PreviewPhotoServerActions,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "SERVIDOR DE FOTOS",
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.8f),
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun StatusHeader(status: PhotoServerStatus) {
    val (color, icon, text) = when (status) {
        PhotoServerStatus.Stopped -> Triple(MoonlightColors.Outline, Icons.Default.CloudOff, "DETENIDO")
        PhotoServerStatus.Starting -> Triple(MoonlightColors.Secondary, Icons.Default.CloudSync, "INICIANDO...")
        is PhotoServerStatus.Running -> Triple(MoonlightColors.PrimaryContainer, Icons.Default.CloudDone, "EJECUTÁNDOSE")
        is PhotoServerStatus.Error -> Triple(MoonlightColors.ErrorContainer, Icons.Default.Error, "ERROR")
    }

    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(color.copy(alpha = 0.1f))
        )
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = color
        )
    }
    
    Spacer(modifier = Modifier.height(16.dp))
    
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = color
    )
}

@Composable
private fun InfoCard(state: PhotoServerState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = MoonlightColors.PrimaryContainer, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("DETALLES DEL SISTEMA", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            DetailRow("Healthcheck", state.healthMessage)
            
            if (state.status is PhotoServerStatus.Running) {
                val s = state.status as PhotoServerStatus.Running
                DetailRow("Puerto", s.port.toString())
                DetailRow("URL Local", s.url)
            }
            
            state.lastError?.let {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Error: $it",
                    color = MoonlightColors.Error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MoonlightColors.Error.copy(alpha = 0.1f))
                        .padding(8.dp)
                )
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun LogsCard(logs: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.8f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("LOGS DEL SERVIDOR", style = MaterialTheme.typography.labelSmall, color = MoonlightColors.PrimaryContainer, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            logs.takeLast(5).forEach { log ->
                Text(
                    log,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun ActionButtons(status: PhotoServerStatus, actions: PhotoServerActions) {
    val isRunning = status is PhotoServerStatus.Running
    val isStarting = status == PhotoServerStatus.Starting

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(
            onClick = { actions.startPhotoServer() },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MoonlightColors.PrimaryContainer),
            enabled = !isRunning && !isStarting
        ) {
            if (isStarting) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Icon(Icons.Default.PowerSettingsNew, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("ARRANCAR SERVIDOR IMMICH", fontWeight = FontWeight.Bold)
            }
        }

        if (isRunning || status is PhotoServerStatus.Error) {
            Button(
                onClick = { actions.stopPhotoServer() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MoonlightColors.ErrorContainer)
            ) {
                Icon(Icons.Default.Cancel, contentDescription = null, tint = MoonlightColors.OnErrorContainer)
                Spacer(modifier = Modifier.width(8.dp))
                Text("APAGAR SERVIDOR IMMICH", color = MoonlightColors.OnErrorContainer, fontWeight = FontWeight.Bold)
            }
        }
    }
}

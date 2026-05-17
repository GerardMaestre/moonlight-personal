package com.limelight.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limelight.shared.platform.PhotoServerActions
import com.limelight.shared.platform.PhotoServerState
import com.limelight.shared.platform.PhotoServerStatus
import com.limelight.shared.platform.PreviewPhotoServerActions
import com.limelight.shared.ui.components.*
import com.limelight.shared.ui.theme.MoonlightColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoServerScreen(
    state: PhotoServerState,
    actions: PhotoServerActions = PreviewPhotoServerActions,
    onBack: () -> Unit,
    onOpenImmich: (left: Int, top: Int, width: Int, height: Int) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val isOnline = state.status is PhotoServerStatus.Running

    AetherisScreen(primaryGlowAlignment = Alignment.TopStart, secondaryGlowAlignment = Alignment.BottomEnd) {
        Scaffold(topBar = { HomeHubTopBar(onBack = onBack) }, containerColor = Color.Transparent) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                // Title and intro section
                item {
                    Text(
                        text = "Servidor Multimedia",
                        style = MaterialTheme.typography.headlineLarge.copy(fontSize = 38.sp),
                        color = MoonlightColors.OnSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Control centralizado para iniciar y conectar tu galería multimedia de Immich.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MoonlightColors.OnSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                // 🌟 Master Launcher Card sit at the very top for direct click access!
                item {
                    MasterLauncherCard(
                        state = state,
                        onOpenImmich = onOpenImmich
                    )
                }

                // Configuration and control cards placed sequentially below
                item {
                    BoxWithConstraints(Modifier.fillMaxWidth()) {
                        if (maxWidth > 840.dp) {
                            Row(horizontalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.fillMaxWidth()) {
                                Column(verticalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.weight(1f)) {
                                    ControlCard(state, actions)
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.weight(1f)) {
                                    ConnectionCard(state, actions)
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(18.dp), modifier = Modifier.fillMaxWidth()) {
                                ControlCard(state, actions)
                                ConnectionCard(state, actions)
                            }
                        }
                    }
                }

                if (state.recentLogs.isNotEmpty()) {
                    item {
                        LogsCard(logs = state.recentLogs)
                    }
                }

                item { Spacer(Modifier.height(86.dp)) }
            }
        }
    }
}

@Composable
private fun MasterLauncherCard(
    state: PhotoServerState,
    onOpenImmich: (left: Int, top: Int, width: Int, height: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val isOnline = state.status is PhotoServerStatus.Running
    var buttonBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }

    GlassCard(
        contentPadding = PaddingValues(24.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isOnline) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MoonlightColors.Tertiary,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "¡Servidor en Línea!",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MoonlightColors.Tertiary
                )
                Text(
                    text = "La galería multimedia está activa y lista para sincronizar. Accede a tu catálogo completo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MoonlightColors.OnSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Button(
                    onClick = {
                        val bounds = buttonBounds
                        if (bounds != null) {
                            onOpenImmich(bounds.left.toInt(), bounds.top.toInt(), bounds.width.toInt(), bounds.height.toInt())
                        } else {
                            onOpenImmich(0, 0, 0, 0)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .onGloballyPositioned { coordinates ->
                            val position = coordinates.positionInWindow()
                            val size = coordinates.size
                            buttonBounds = androidx.compose.ui.geometry.Rect(
                                position.x,
                                position.y,
                                position.x + size.width,
                                position.y + size.height
                            )
                        }
                        .border(
                            width = 2.dp,
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    MoonlightColors.Tertiary,
                                    MoonlightColors.Primary
                                )
                            ),
                            shape = RoundedCornerShape(20.dp)
                        ),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MoonlightColors.Tertiary.copy(alpha = 0.12f),
                        contentColor = Color.White
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            tint = MoonlightColors.Tertiary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "ABRIR GALERÍA IMMICH",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.5.sp
                            ),
                            color = Color.White
                        )
                    }
                }
            } else {
                Icon(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.size(54.dp)
                )
                Text(
                    text = "Servidor Inactivo",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White.copy(alpha = 0.5f)
                )
                Text(
                    text = "Para navegar por tus álbumes locales, debes encender el servidor multimedia en tu PC.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MoonlightColors.OnSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(Modifier.height(4.dp))

                // Beautiful interactive scroll hint
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Desliza hacia abajo para encenderlo",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = MoonlightColors.Tertiary.copy(alpha = 0.8f)
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MoonlightColors.Tertiary.copy(alpha = 0.8f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ControlCard(
    state: PhotoServerState,
    actions: PhotoServerActions,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val isOnline = state.status is PhotoServerStatus.Running
    val starting = state.status == PhotoServerStatus.Starting
    val accent = when (state.status) {
        PhotoServerStatus.Stopped -> MoonlightColors.Outline
        PhotoServerStatus.Starting -> MoonlightColors.Tertiary
        is PhotoServerStatus.Running -> MoonlightColors.Tertiary
        is PhotoServerStatus.Error -> MoonlightColors.Error
    }

    GlassCard(contentPadding = PaddingValues(28.dp), modifier = modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.22f), CircleShape)
                )
                Icon(
                    imageVector = if (isOnline) Icons.Default.CloudDone else if (starting) Icons.Default.CloudSync else Icons.Default.CloudOff,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(54.dp)
                )
            }
            Spacer(Modifier.height(18.dp))
            StatusPill(if (isOnline) "Servidor Activo" else if (starting) "Conectando" else "Servidor Apagado", accent)
            Spacer(Modifier.height(12.dp))
            Text(if (isOnline) "En Línea" else "Desconectado", style = MaterialTheme.typography.headlineMedium, color = MoonlightColors.OnSurface)
            Spacer(Modifier.height(8.dp))
            Text(state.healthMessage, style = MaterialTheme.typography.bodyMedium, color = MoonlightColors.OnSurfaceVariant, textAlign = TextAlign.Center)
            state.lastError?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, color = MoonlightColors.Error, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                PrimaryGlassButton(if (isOnline) "Recargar" else "Conectar", Icons.Default.PowerSettingsNew, { scope.launch { actions.startPhotoServer() } }, Modifier.weight(1f), enabled = !starting)
                ErrorGlassButton("Apagar", Icons.Default.StopCircle, { actions.stopPhotoServer() }, Modifier.weight(1f), enabled = isOnline || state.status is PhotoServerStatus.Error)
            }
            if (isOnline || state.status is PhotoServerStatus.Error) {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { scope.launch { actions.restartPhotoServer() } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Icon(Icons.Default.Refresh, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Reiniciar Proceso en PC")
                }
            }
        }
    }
}

@Composable
private fun ConnectionCard(state: PhotoServerState, actions: PhotoServerActions, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    GlassCard(contentPadding = PaddingValues(22.dp), modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Conexión Immich", style = MaterialTheme.typography.headlineMedium, color = MoonlightColors.OnSurface)
            Text("Introduce la URL de tu instancia y una API Key con permisos de lectura.", style = MaterialTheme.typography.bodyMedium, color = MoonlightColors.OnSurfaceVariant)
            OutlinedTextField(
                value = state.connectionConfig.baseUrl,
                onValueChange = { actions.onUpdateConnection(it, state.connectionConfig.apiKey) },
                label = { Text("URL base (http://100.67.140.39:2283)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.connectionConfig.apiKey,
                onValueChange = { actions.onUpdateConnection(state.connectionConfig.baseUrl, it) },
                label = { Text("API Key") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            PrimaryGlassButton(
                text = "Guardar y Verificar",
                icon = Icons.Default.Refresh,
                onClick = { scope.launch { actions.refreshImmich() } },
                modifier = Modifier.fillMaxWidth(),
                enabled = state.status != PhotoServerStatus.Starting,
            )
        }
    }
}

@Composable
private fun LogsCard(logs: List<String>, modifier: Modifier = Modifier) {
    GlassCard(contentPadding = PaddingValues(22.dp), modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, null, tint = MoonlightColors.Primary, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text("Registro de Actividad", style = MaterialTheme.typography.titleMedium, color = MoonlightColors.OnSurface)
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.4f))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                logs.takeLast(6).forEach { log ->
                    Text(log, color = MoonlightColors.OnSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                }
            }
        }
    }
}

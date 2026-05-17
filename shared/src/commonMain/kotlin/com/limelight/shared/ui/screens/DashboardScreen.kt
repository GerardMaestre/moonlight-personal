package com.limelight.shared.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.limelight.shared.platform.PlatformActions
import com.limelight.shared.platform.PreviewPlatformActions
import com.limelight.shared.ui.components.*
import com.limelight.shared.ui.theme.MoonlightColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    state: DashboardState,
    actions: PlatformActions = PreviewPlatformActions,
) {
    AetherisScreen {
        Scaffold(
            topBar = {
                HomeHubTopBar(
                    onBack = { actions.onNavigateBack() },
                    onProfileClick = { actions.onOpenSettings() }
                ) {
                    IconButton(onClick = { state.isAddPcDialogOpen = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Añadir PC", tint = MoonlightColors.OnSurface)
                    }
                }
            },
            containerColor = Color.Transparent
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                item {
                    Spacer(Modifier.height(16.dp))
                    Text("Moonlight Gaming", style = MaterialTheme.typography.headlineLarge, color = MoonlightColors.OnSurface)
                    Spacer(Modifier.height(6.dp))
                    Text("Select a host PC to stream games directly to this device.", style = MaterialTheme.typography.bodyMedium, color = MoonlightColors.OnSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    StatusPill("${state.computers.count { it.isOnline }} dispositivos online", MoonlightColors.Tertiary)
                }

                items(items = state.computers, key = { it.id }) { computer ->
                    PcCard(
                        computer = computer,
                        onClick = { actions.onPcClick(computer.id, computer.name) },
                        onWakeOnLan = computer.macAddress?.let { mac -> { actions.onWakeOnLan(mac) } },
                        onPair = { actions.onPair(computer.id) }
                    )
                }

                if (state.computers.isEmpty()) {
                    item {
                        GlassCard(contentPadding = PaddingValues(28.dp)) {
                            Column {
                                Text("Sin hosts todavía", style = MaterialTheme.typography.titleLarge, color = MoonlightColors.OnSurface, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(8.dp))
                                Text("Añade tu PC manualmente o espera a que aparezca en la red.", color = MoonlightColors.OnSurfaceVariant)
                                Spacer(Modifier.height(18.dp))
                                PrimaryGlassButton("Añadir PC", Icons.Default.Add, { state.isAddPcDialogOpen = true }, Modifier.fillMaxWidth())
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(110.dp)) }
            }
        }
    }

    state.lastActionMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { state.clearMessage() },
            title = { Text("Acción ejecutada") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { state.clearMessage() }) { Text("Aceptar") } }
        )
    }

    if (state.isAddPcDialogOpen) {
        var ipAddress by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { state.isAddPcDialogOpen = false },
            title = { Text("Añadir PC (IP Tailscale)") },
            text = {
                Column {
                    Text("Introduce la dirección IP de tu ordenador:")
                    Spacer(Modifier.height(8.dp))
                    TextField(value = ipAddress, onValueChange = { ipAddress = it }, placeholder = { Text("Ej: 100.x.y.z") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (ipAddress.isNotBlank()) {
                        actions.onAddPcManual(ipAddress)
                        state.isAddPcDialogOpen = false
                    }
                }) { Text("Añadir") }
            },
            dismissButton = { TextButton(onClick = { state.isAddPcDialogOpen = false }) { Text("Cancelar") } }
        )
    }
}

package com.limelight.shared.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limelight.shared.model.ComputerInfo
import com.limelight.shared.model.ComputerStatus
import com.limelight.shared.platform.PlatformActions
import com.limelight.shared.platform.PreviewPlatformActions
import com.limelight.shared.ui.components.PcCard
import com.limelight.shared.ui.theme.MoonlightColors
import com.limelight.shared.ui.theme.MoonlightTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    state: DashboardState,
    actions: PlatformActions = PreviewPlatformActions,
) {
    // MoonlightTheme is already provided by the Activity
    Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "MOONLIGHT",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { actions.onNavigateBack() }) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { state.isAddPcDialogOpen = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Add PC")
                        }
                        IconButton(onClick = actions::onOpenSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.8f),
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // ── Section: Gaming PCs ─────────────────────────────
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    SectionHeader(
                        title = "Your Gaming PCs",
                        subtitle = "${state.computers.count { it.isOnline }} online"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(
                    items = state.computers,
                    key = { it.id }
                ) { computer ->
                    PcCard(
                        computer = computer,
                        onClick = {
                            actions.onPcClick(computer.id, computer.name)
                        },
                        onWakeOnLan = computer.macAddress?.let { mac ->
                            { actions.onWakeOnLan(mac) }
                        },
                        onPair = {
                            actions.onPair(computer.id)
                        }
                    )
                }

                // Bottom spacing for FAB
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }

        // Action Feedback Dialog
        state.lastActionMessage?.let { message ->
            AlertDialog(
                onDismissRequest = { state.clearMessage() },
                title = { Text("Acción Ejecutada") },
                text = { Text(message) },
                confirmButton = {
                    TextButton(onClick = { state.clearMessage() }) {
                        Text("Aceptar")
                    }
                }
            )
        }

        // Add PC Dialog
        if (state.isAddPcDialogOpen) {
            var ipAddress by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { state.isAddPcDialogOpen = false },
                title = { Text("Añadir PC (IP Tailscale)") },
                text = {
                    Column {
                        Text("Introduce la dirección IP de tu ordenador:")
                        Spacer(modifier = Modifier.height(8.dp))
                        TextField(
                            value = ipAddress,
                            onValueChange = { ipAddress = it },
                            placeholder = { Text("Ej: 100.x.y.z") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (ipAddress.isNotBlank()) {
                                actions.onAddPcManual(ipAddress)
                                state.isAddPcDialogOpen = false
                            }
                        }
                    ) {
                        Text("Añadir")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { state.isAddPcDialogOpen = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String? = null,
    icon: @Composable (() -> Unit)? = null
) {
    Row {
        if (icon != null) {
            icon()
            Spacer(modifier = Modifier.width(8.dp))
        }
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MoonlightColors.Secondary
                )
            }
        }
    }
}

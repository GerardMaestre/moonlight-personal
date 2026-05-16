package com.limelight.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Hub
import androidx.compose.ui.graphics.Color
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
import com.limelight.shared.ui.components.*
import com.limelight.shared.ui.theme.MoonlightColors
import com.limelight.shared.ui.theme.MoonlightTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    state: DashboardState,
    actions: PlatformActions = PreviewPlatformActions,
) {
    // MoonlightTheme is already provided by the Activity
    Box(modifier = Modifier.fillMaxSize().background(MoonlightColors.Background)) {
        // Background Glows
        AetherisGlow(
            modifier = Modifier.align(Alignment.TopEnd).offset(x = 100.dp, y = (-100).dp),
            color = MoonlightColors.Primary
        )
        AetherisGlow(
            modifier = Modifier.align(Alignment.BottomStart).offset(x = (-150).dp, y = 150.dp),
            color = MoonlightColors.Tertiary
        )

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Hub,
                                contentDescription = null,
                                tint = MoonlightColors.Primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "HOME HUB",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { actions.onNavigateBack() }) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = MoonlightColors.OnSurface
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { state.isAddPcDialogOpen = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Add PC", tint = MoonlightColors.OnSurface)
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    SectionHeader(
                        title = "Your Gaming PCs",
                        subtitle = "${state.computers.count { it.isOnline }} DEVICES ONLINE"
                    )
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
                    Spacer(modifier = Modifier.height(100.dp))
                }
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

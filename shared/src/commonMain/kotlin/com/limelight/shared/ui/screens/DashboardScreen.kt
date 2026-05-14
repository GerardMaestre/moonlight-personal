package com.limelight.shared.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limelight.shared.model.NetworkProfiles
import com.limelight.shared.platform.PlatformActions
import com.limelight.shared.platform.PreviewPlatformActions
import com.limelight.shared.ui.components.NetworkProfileCard
import com.limelight.shared.ui.components.PcCard
import com.limelight.shared.ui.theme.MoonlightColors
import com.limelight.shared.ui.theme.MoonlightTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    state: DashboardState,
    actions: PlatformActions = PreviewPlatformActions,
) {
    MoonlightTheme {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "MOONLIGHT",
                            fontWeight = FontWeight.Black,
                            letterSpacing = 3.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = actions::onNavigateBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = actions::onOpenSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MoonlightColors.Purple
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = actions::onAddPc,
                    containerColor = MoonlightColors.Purple,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add PC")
                }
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
                        }
                    )
                }

                // ── Section: Network Profiles ───────────────────────
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    SectionHeader(
                        title = "Network Profiles",
                        icon = {
                            Icon(
                                Icons.Default.Tune,
                                contentDescription = null,
                                tint = MoonlightColors.Cyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(
                    items = NetworkProfiles.all,
                    key = { it.id }
                ) { profile ->
                    NetworkProfileCard(
                        profile = profile,
                        isSelected = state.selectedProfileId == profile.id,
                        onClick = {
                            state.selectProfile(profile.id)
                            actions.onApplyNetworkProfile(profile.id)
                        }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Bottom spacing for FAB
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
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
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MoonlightColors.Green
                )
            }
        }
    }
}

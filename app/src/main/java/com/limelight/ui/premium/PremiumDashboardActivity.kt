package com.limelight.ui.premium

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.limelight.AppView
import com.limelight.nvstream.http.ComputerDetails
import com.limelight.nvstream.wol.WakeOnLanSender
import com.limelight.preferences.StreamSettings
import kotlin.concurrent.thread
import com.limelight.shared.platform.PlatformActions
import com.limelight.shared.ui.screens.DashboardScreen
import com.limelight.shared.ui.screens.AppNavigation
import com.limelight.shared.ui.screens.AppScreen
import com.limelight.shared.ui.screens.MainMenuScreen
import com.limelight.shared.ui.screens.GameListScreen
import com.limelight.shared.ui.theme.MoonlightTheme
import com.limelight.computers.ComputerManagerService
import com.limelight.preferences.AddComputerManually
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class PremiumDashboardActivity : ComponentActivity() {
    private val viewModel: PremiumDashboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            val nav = androidx.compose.runtime.remember { AppNavigation() }

            MoonlightTheme {
                // Handle system back button
                androidx.activity.compose.BackHandler(enabled = nav.currentScreen != AppScreen.MAIN_MENU) {
                    nav.goBack()
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (nav.currentScreen) {
                        AppScreen.MAIN_MENU -> {
                            MainMenuScreen(
                                onNavigate = { screen -> nav.navigateTo(screen) }
                            )
                        }
                        AppScreen.MOONLIGHT -> {
                            DashboardScreen(
                                state = viewModel.dashboardState,
                                actions = object : PlatformActions {
                                    override fun onAddPc() {}
                                    override fun onAddPcManual(ip: String) { viewModel.addComputer(ip) }
                                    override fun onOpenSettings() {
                                        val intent = Intent(this@PremiumDashboardActivity, StreamSettings::class.java)
                                        startActivity(intent)
                                    }
                                    override fun onPcClick(id: String, name: String) {
                                        viewModel.dashboardState.selectedComputer = viewModel.dashboardState.computers.find { it.id == id }
                                        viewModel.dashboardState.games.clear()
                                        viewModel.dashboardState.games.addAll(listOf(
                                            com.limelight.shared.model.GameInfo(1, "Steam", boxArtUrl = ""),
                                            com.limelight.shared.model.GameInfo(2, "Desktop", boxArtUrl = ""),
                                            com.limelight.shared.model.GameInfo(3, "Epic Games", boxArtUrl = "")
                                        ))
                                        nav.navigateTo(AppScreen.GAME_LIST)
                                    }
                                    override fun onApplyNetworkProfile(profileId: String) {}
                                    override fun onWakeOnLan(macAddress: String) {
                                        thread {
                                            val fakeComputer = ComputerDetails().apply {
                                                this.macAddress = macAddress
                                                this.manualAddress = ComputerDetails.AddressTuple("255.255.255.255", 9)
                                            }
                                            WakeOnLanSender.sendWolPacket(fakeComputer)
                                        }
                                    }
                                    override fun onNavigateBack() {
                                        nav.goBack()
                                    }
                                }
                            )
                        }
                        AppScreen.GAME_LIST -> {
                            GameListScreen(
                                state = viewModel.dashboardState,
                                onBack = { nav.goBack() },
                                onGameClick = { game ->
                                    // Here we would eventually launch the native stream
                                    val intent = Intent(this@PremiumDashboardActivity, AppView::class.java)
                                    intent.putExtra(AppView.UUID_EXTRA, viewModel.dashboardState.selectedComputer?.id)
                                    intent.putExtra(AppView.NAME_EXTRA, viewModel.dashboardState.selectedComputer?.name)
                                    startActivity(intent)
                                }
                            )
                        }
                        AppScreen.POWER_CONTROL -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Modo: Mi PC (Encender/Apagar)", color = MaterialTheme.colorScheme.onBackground)
                                Button(onClick = { nav.goBack() }, modifier = Modifier.padding(top = 100.dp)) {
                                    Text("Volver")
                                }
                            }
                        }
                        AppScreen.PHOTO_SERVER -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Modo: Servidor de Fotos", color = MaterialTheme.colorScheme.onBackground)
                                Button(onClick = { nav.goBack() }, modifier = Modifier.padding(top = 100.dp)) {
                                    Text("Volver")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

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
import com.limelight.shared.platform.PhotoServerActions
import com.limelight.shared.platform.PlatformActions
import com.limelight.shared.ui.screens.DashboardScreen
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
            val controller = viewModel.controller
            val photoServerManager = remember { AndroidPhotoServerManager(controller.photoServerState) }

            MoonlightTheme {
                // Handle system back button
                androidx.activity.compose.BackHandler(enabled = controller.navigation.currentScreen != AppScreen.MAIN_MENU) {
                    controller.navigation.goBack()
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (controller.navigation.currentScreen) {
                        AppScreen.MAIN_MENU -> {
                            MainMenuScreen(
                                onNavigate = { screen -> controller.navigation.navigateTo(screen) }
                            )
                        }
                        AppScreen.MOONLIGHT -> {
                            DashboardScreen(
                                state = controller.dashboardState,
                                actions = object : PlatformActions {
                                    override fun onAddPc() {}
                                    override fun onAddPcManual(ip: String) { viewModel.addComputer(ip) }
                                    override fun onOpenSettings() {
                                        val intent = Intent(this@PremiumDashboardActivity, StreamSettings::class.java)
                                        startActivity(intent)
                                    }
                                     override fun onPcClick(id: String, name: String) {
                                         viewModel.selectComputer(id)
                                         controller.openComputer(id)
                                     }
                                     override fun onPair(id: String) {
                                         viewModel.pair(id, this@PremiumDashboardActivity)
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
                                         controller.navigation.goBack()
                                     }
                                }
                            )
                        }
                        AppScreen.GAME_LIST -> {
                            GameListScreen(
                                state = controller.dashboardState,
                                onBack = { controller.navigation.goBack() },
                                onGameClick = { game ->
                                     val binder = viewModel.getBinder()
                                     val computer = controller.dashboardState.selectedComputer
                                     if (binder != null && computer != null) {
                                         val computerDetails = binder.getComputer(computer.id)
                                         if (computerDetails != null) {
                                             // Convert GameInfo back to NvApp for ServerHelper
                                             val nvApp = com.limelight.nvstream.http.NvApp().apply {
                                                 setAppId(game.id)
                                                 setAppName(game.name)
                                                 // Assume other fields if needed
                                             }
                                             com.limelight.utils.ServerHelper.doStart(
                                                 this@PremiumDashboardActivity,
                                                 nvApp,
                                                 computerDetails,
                                                 binder
                                             )
                                         }
                                     }
                                }
                            )
                        }
                        AppScreen.POWER_CONTROL -> {
                            AlertDialog(
                                onDismissRequest = { controller.navigation.goBack() },
                                title = { Text("Mi PC") },
                                text = { Text(controller.featureMessage ?: "Módulo listo") },
                                confirmButton = {
                                    TextButton(onClick = { controller.navigation.goBack() }) { Text("Volver") }
                                }
                            )
                        }
                        AppScreen.PHOTO_SERVER -> {
                            com.limelight.shared.ui.screens.PhotoServerScreen(
                                state = controller.photoServerState,
                                actions = object : PhotoServerActions {
                                    override fun startPhotoServer() = photoServerManager.start()
                                    override fun stopPhotoServer() = photoServerManager.stop()
                                    override fun restartPhotoServer() = photoServerManager.restart()
                                },
                                onBack = { controller.navigation.goBack() }
                            )
                        }
                    }
                }
            }
        }
    }
}

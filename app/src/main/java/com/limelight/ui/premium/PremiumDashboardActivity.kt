package com.limelight.ui.premium

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.limelight.AppView
import com.limelight.nvstream.http.ComputerDetails
import com.limelight.shared.network.StandardWolSender
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
import com.limelight.shared.network.RemoteScriptClient
import com.limelight.shared.network.UpSnapClient
import com.limelight.shared.network.WakeService
import com.limelight.shared.ui.screens.PowerControlScreen
import com.limelight.shared.platform.StartCommandResult
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
                    Scaffold(
                        bottomBar = {
                            com.limelight.shared.ui.components.BottomNavBar(
                                currentScreen = controller.navigation.currentScreen,
                                onNavigate = { screen ->
                                    if (controller.navigation.currentScreen != screen) {
                                        controller.navigation.navigateTo(screen)
                                    }
                                }
                            )
                        }
                    ) { innerPadding ->
                        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
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

                                             override fun onWakeOnLan(macAddress: String) {
                                                 thread {
                                                     com.limelight.shared.network.StandardWolSender.sendMagicPacketCompat(macAddress)
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
                                    val powerState = controller.powerControlState
                                    
                                    // Initialize from config if not already done
                                    LaunchedEffect(Unit) {
                                        if (!powerState.isConfigured) {
                                            val config = UpSnapConfig.getInstance(this@PremiumDashboardActivity)
                                            powerState.isConfigured = config.isConfigured
                                            powerState.serverUrl = config.serverUrl
                                            powerState.username = config.getUsername() ?: ""
                                            powerState.password = config.getPassword() ?: ""
                                            powerState.deviceId = config.deviceId
                                            powerState.isEnabled = config.isEnabled
                                        }
                                    }

                                    PowerControlScreen(
                                        state = powerState,
                                        onBack = { controller.navigation.goBack() },
                                        onSaveConfig = { url, user, pass, deviceId ->
                                            val config = UpSnapConfig.getInstance(this@PremiumDashboardActivity)
                                            config.serverUrl = url
                                            config.deviceId = deviceId
                                            config.saveCredentials(user, pass)
                                            config.isEnabled = true
                                            
                                            powerState.isConfigured = true
                                            powerState.serverUrl = url
                                            powerState.username = user
                                            powerState.password = pass
                                            powerState.deviceId = deviceId
                                            powerState.isEnabled = true
                                            powerState.showConfig = false
                                            powerState.statusMessage = "Configuración guardada ✓"
                                        },
                                        onWake = {
                                            powerState.isWaking = true
                                            powerState.statusMessage = null
                                            thread {
                                                val client = UpSnapClient(
                                                    powerState.serverUrl,
                                                    powerState.username,
                                                    powerState.password
                                                )
                                                // deviceId path (UpSnap) + optional UDP fallback (MAC not configured in this screen yet)
                                                val result = WakeService.wakeWithFallback(
                                                    upSnapClient = client,
                                                    deviceId = powerState.deviceId,
                                                    macAddress = null,
                                                )
                                                runOnUiThread {
                                                    powerState.isWaking = false
                                                    when (result) {
                                                        is WakeService.WakeOutcome.Success -> {
                                                            powerState.statusMessage = "Señal enviada correctamente ✓"
                                                        }
                                                        is WakeService.WakeOutcome.Failure -> {
                                                            powerState.statusMessage = result.reason
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                        onClearConfig = {
                                            UpSnapConfig.getInstance(this@PremiumDashboardActivity).clear()
                                            powerState.isConfigured = false
                                            powerState.showConfig = true
                                        },
                                        onTestConnection = { url, user, password ->
                                            powerState.isTestingConnection = true
                                            powerState.statusMessage = null
                                            thread {
                                                try {
                                                    val client = UpSnapClient(url, user, password)
                                                    val devices = client.listDevices()
                                                    runOnUiThread {
                                                        powerState.isTestingConnection = false
                                                        if (devices.isNotEmpty()) {
                                                            powerState.availableDevices.clear()
                                                            powerState.availableDevices.addAll(devices)
                                                            powerState.statusMessage = "¡Dispositivos encontrados! ✓"
                                                        } else {
                                                            powerState.statusMessage = "No se encontraron dispositivos."
                                                        }
                                                    }
                                                } catch (e: Exception) {
                                                    runOnUiThread {
                                                        powerState.isTestingConnection = false
                                                        powerState.statusMessage = "Error: ${e.message}"
                                                    }
                                                }
                                            }
                                        }
                                    )
                                }
                                AppScreen.PHOTO_SERVER -> {
                                    val config = remember { RemoteScriptConfig.getInstance(this@PremiumDashboardActivity) }
                                    val remoteClient = remember(config.serverUrl, config.token) { 
                                        RemoteScriptClient(config.serverUrl, config.token) 
                                    }
                                    com.limelight.shared.ui.screens.PhotoServerScreen(
                                        state = controller.photoServerState,
                                        actions = object : PhotoServerActions {
                                            override fun startPhotoServer(): StartCommandResult {
                                                thread { remoteClient.runScript("07_Personalizacion", "fotos.bat") }
                                                return StartCommandResult.Success
                                            }
                                            override fun stopPhotoServer() {
                                                thread { remoteClient.runScript("07_Personalizacion", "cerrar_fotos.bat") }
                                            }
                                            override fun restartPhotoServer() {
                                                thread {
                                                    remoteClient.runScript("07_Personalizacion", "cerrar_fotos.bat")
                                                    Thread.sleep(2000)
                                                    remoteClient.runScript("07_Personalizacion", "fotos.bat")
                                                }
                                            }
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
    }
}

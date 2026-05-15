package com.limelight.ui.premium

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
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
import com.limelight.shared.network.UpSnapClient
import com.limelight.shared.network.RemoteScriptClient
import com.limelight.shared.ui.screens.PowerControlState
import com.limelight.shared.ui.screens.PowerControlScreen
import com.limelight.shared.ui.screens.PhotoServerScreen
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
            val nav = remember { AppNavigation() }
            val powerState = remember { PowerControlState() }

            // Load UpSnap config on first composition
            LaunchedEffect(Unit) {
                val config = UpSnapConfig.getInstance(this@PremiumDashboardActivity)
                powerState.isConfigured = config.isConfigured
                powerState.serverUrl = config.serverUrl
                powerState.username = config.getUsername() ?: ""
                // We load the password from secure storage so it stays in the fields
                powerState.password = config.getPassword() ?: "" 
                powerState.deviceId = config.deviceId
                powerState.isEnabled = config.isEnabled
            }

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
                                         viewModel.selectComputer(id)
                                         nav.navigateTo(AppScreen.GAME_LIST)
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
                                     val binder = viewModel.getBinder()
                                     val computer = viewModel.dashboardState.selectedComputer
                                     if (binder != null && computer != null) {
                                         val computerDetails = binder.getComputer(computer.id)
                                         if (computerDetails != null) {
                                             val nvApp = com.limelight.nvstream.http.NvApp().apply {
                                                 setAppId(game.id)
                                                 setAppName(game.name)
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
                            PowerControlScreen(
                                state = powerState,
                                onBack = { nav.goBack() },
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
                                    val config = UpSnapConfig.getInstance(this@PremiumDashboardActivity)
                                    if (!config.isConfigured) return@PowerControlScreen
                                    
                                    powerState.isWaking = true
                                    powerState.statusMessage = null
                                    
                                    thread {
                                        val client = UpSnapClient(
                                            config.serverUrl, 
                                            config.getUsername() ?: "", 
                                            config.getPassword() ?: ""
                                        )
                                        val result = client.wakeDevice(config.deviceId)
                                        runOnUiThread {
                                            powerState.isWaking = false
                                            when (result) {
                                                is UpSnapClient.WakeResult.Success -> {
                                                    powerState.statusMessage = "Señal enviada correctamente ✓"
                                                }
                                                is UpSnapClient.WakeResult.Error -> {
                                                    powerState.statusMessage = result.message
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
                                        val client = UpSnapClient(url, user, password)
                                        val devices = client.listDevices()
                                        runOnUiThread {
                                            powerState.isTestingConnection = false
                                            if (devices.isNotEmpty()) {
                                                powerState.availableDevices.clear()
                                                powerState.availableDevices.addAll(devices)
                                                powerState.statusMessage = "¡Dispositivos encontrados! ✓"
                                            } else {
                                                powerState.statusMessage = "Error: Revisa los datos."
                                            }
                                        }
                                    }
                                }
                            )
                        }
                        AppScreen.PHOTO_SERVER -> {
                            var status by remember { mutableStateOf<String?>(null) }
                            val client = remember { RemoteScriptClient("http://100.67.140.39:3000", "CasaGerard") }

                            PhotoServerScreen(
                                statusMessage = status,
                                onBack = { nav.goBack() },
                                onStartServer = {
                                    status = "Iniciando servidor..."
                                    thread {
                                        val ok = client.runScript("07_Personalizacion", "fotos.bat")
                                        runOnUiThread { status = if (ok) "¡Servidor arrancado! ✓" else "Error de conexión." }
                                    }
                                },
                                onStopServer = {
                                    status = "Deteniendo servidor..."
                                    thread {
                                        val ok = client.runScript("07_Personalizacion", "cerrar_fotos.bat")
                                        runOnUiThread { status = if (ok) "¡Servidor detenido! ✓" else "Error de conexión." }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

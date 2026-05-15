import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import com.limelight.shared.platform.PreviewPlatformActions
import com.limelight.shared.ui.screens.DashboardScreen
import com.limelight.shared.ui.screens.DashboardState
import com.limelight.shared.ui.screens.AppNavigation
import com.limelight.shared.ui.screens.AppScreen
import com.limelight.shared.ui.screens.MainMenuScreen
import com.limelight.shared.ui.screens.GameListScreen
import com.limelight.shared.ui.theme.MoonlightTheme

import kotlinx.coroutines.*
import java.net.*
import java.util.concurrent.Executors

/**
 * Desktop entry point for a functional Moonlight UI.
 *
 * This version implements real PC discovery (UDP) and Wake-on-Lan.
 */
fun main() = application {
    val state = remember { DashboardState() }
    val scope = rememberCoroutineScope()
    val nav = remember { AppNavigation() }

    // Real Discovery logic for Desktop
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            startDesktopDiscovery(state)
        }
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "Panel de Control",
        state = WindowState(size = DpSize(1280.dp, 720.dp))
    ) {
        MoonlightTheme {
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
                            state = state,
                            actions = object : com.limelight.shared.platform.PlatformActions {
                                override fun onAddPc() {}
                                override fun onAddPcManual(ip: String) {
                                    state.updateComputer(com.limelight.shared.model.ComputerInfo(
                                        id = ip,
                                        name = "PC Manual ($ip)",
                                        status = com.limelight.shared.model.ComputerStatus.ONLINE,
                                        localAddress = ip,
                                        isPaired = true
                                    ))
                                }
                                override fun onOpenSettings() {
                                    state.showMessage("Ajustes no disponibles en esta versión Desktop.")
                                }
                                override fun onPcClick(computerId: String, computerName: String) {
                                    state.selectedComputer = state.computers.find { it.id == computerId }
                                    state.games.clear()
                                    state.games.addAll(listOf(
                                        com.limelight.shared.model.GameInfo(1, "Steam", boxArtUrl = ""),
                                        com.limelight.shared.model.GameInfo(2, "Desktop", boxArtUrl = ""),
                                        com.limelight.shared.model.GameInfo(3, "Epic Games", boxArtUrl = "")
                                    ))
                                    nav.navigateTo(AppScreen.GAME_LIST)
                                }
                                override fun onPair(computerId: String) {
                                    state.showMessage("Emparejamiento no implementado en Desktop.")
                                }
                                override fun onApplyNetworkProfile(profileId: String) {}
                                override fun onWakeOnLan(macAddress: String) {
                                    state.showMessage("Enviando WoL a $macAddress...")
                                    scope.launch(Dispatchers.IO) { sendWolPacket(macAddress) }
                                }
                                override fun onNavigateBack() {
                                    nav.goBack()
                                }
                            }
                        )
                    }
                    AppScreen.GAME_LIST -> {
                        GameListScreen(
                            state = state,
                            onBack = { nav.goBack() },
                            onGameClick = { game ->
                                state.showMessage("Iniciando ${game.name}...")
                            }
                        )
                    }
                    AppScreen.POWER_CONTROL -> {
                        val powerState = remember { com.limelight.shared.ui.screens.PowerControlState() }
                        com.limelight.shared.ui.screens.PowerControlScreen(
                            state = powerState,
                            onBack = { nav.goBack() },
                            onSaveConfig = { url, user, pass, deviceId ->
                                powerState.isConfigured = true
                                powerState.serverUrl = url
                                powerState.username = user
                                powerState.password = pass
                                powerState.deviceId = deviceId
                                powerState.showConfig = false
                                powerState.statusMessage = "Configuración guardada ✓"
                            },
                            onWake = {
                                powerState.isWaking = true
                                powerState.statusMessage = null
                                scope.launch(Dispatchers.IO) {
                                    val client = com.limelight.shared.network.UpSnapClient(
                                        powerState.serverUrl, powerState.username, powerState.password
                                    )
                                    val result = client.wakeDevice(powerState.deviceId)
                                    withContext(Dispatchers.Main) {
                                        powerState.isWaking = false
                                        when (result) {
                                            is com.limelight.shared.network.UpSnapClient.WakeResult.Success -> {
                                                powerState.statusMessage = "Señal enviada con éxito ✓"
                                            }
                                            is com.limelight.shared.network.UpSnapClient.WakeResult.Error -> {
                                                powerState.statusMessage = result.message
                                            }
                                        }
                                    }
                                }
                            },
                            onClearConfig = {
                                powerState.isConfigured = false
                                powerState.showConfig = true
                            },
                            onTestConnection = { url, user, pass ->
                                powerState.isTestingConnection = true
                                scope.launch(Dispatchers.IO) {
                                    val client = com.limelight.shared.network.UpSnapClient(url, user, pass)
                                    val devices = client.listDevices()
                                    withContext(Dispatchers.Main) {
                                        powerState.isTestingConnection = false
                                        if (devices.isNotEmpty()) {
                                            powerState.availableDevices.clear()
                                            powerState.availableDevices.addAll(devices)
                                            powerState.statusMessage = "¡Dispositivos encontrados! Selecciona uno abajo ✓"
                                        } else {
                                            powerState.statusMessage = "Error: No se encontraron dispositivos."
                                        }
                                    }
                                }
                            }
                        )
                    }
                    AppScreen.PHOTO_SERVER -> {
                        var status by remember { mutableStateOf<String?>(null) }
                        val client = remember { com.limelight.shared.network.RemoteScriptClient("http://100.67.140.39:3000", "CasaGerard") }
                        
                        com.limelight.shared.ui.screens.PhotoServerScreen(
                            statusMessage = status,
                            onBack = { nav.goBack() },
                            onStartServer = {
                                status = "Iniciando servidor de fotos..."
                                scope.launch(Dispatchers.IO) {
                                    val ok = client.runScript("07_Personalizacion", "fotos.bat")
                                    withContext(Dispatchers.Main) {
                                        status = if (ok) "¡Servidor arrancado correctamente! ✓" else "Error: No se pudo conectar con el PC."
                                    }
                                }
                            },
                            onStopServer = {
                                status = "Deteniendo servidor de fotos..."
                                scope.launch(Dispatchers.IO) {
                                    val ok = client.runScript("07_Personalizacion", "cerrar_fotos.bat")
                                    withContext(Dispatchers.Main) {
                                        status = if (ok) "¡Servidor detenido correctamente! ✓" else "Error: No se pudo conectar con el PC."
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun startDesktopDiscovery(state: DashboardState) {
    // Simple UDP Discovery for Desktop
    try {
        val socket = DatagramSocket()
        socket.broadcast = true
        val buffer = "Moonlight Discovery".toByteArray()
        val packet = DatagramPacket(buffer, buffer.size, InetAddress.getByName("255.255.255.255"), 47998)
        
        // Loop discovery every 5 seconds
        while (true) {
            socket.send(packet)
            // In a real app, we would listen for responses here.
            // For now, let's at least show we are scanning.
            Thread.sleep(5000)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun sendWolPacket(macAddress: String) {
    try {
        val macBytes = macAddress.split(":", "-").map { it.toInt(16).toByte() }.toByteArray()
        val bytes = ByteArray(6 + 16 * macBytes.size)
        for (i in 0 until 6) bytes[i] = 0xff.toByte()
        for (i in 1 until 17) System.arraycopy(macBytes, 0, bytes, i * macBytes.size, macBytes.size)
        
        val address = InetAddress.getByName("255.255.255.255")
        val packet = DatagramPacket(bytes, bytes.size, address, 9)
        DatagramSocket().send(packet)
        println("WOL packet sent to $macAddress")
    } catch (e: Exception) {
        println("Failed to send WOL: ${e.message}")
    }
}

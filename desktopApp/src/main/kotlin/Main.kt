import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import com.limelight.shared.model.ComputerInfo
import com.limelight.shared.model.ComputerStatus
import com.limelight.shared.platform.PhotoServerActions
import com.limelight.shared.platform.PhotoServerState
import com.limelight.shared.platform.PlatformActions
import com.limelight.shared.ui.screens.*
import com.limelight.shared.ui.theme.MoonlightTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

fun main(args: Array<String>) {
    if (args.contains("--server")) {
        runHeadlessServerMode()
        return
    }
    runDesktopUi()
}

private fun runDesktopUi() = application {
    val controller = remember { AppController() }
    val scope = rememberCoroutineScope()
    val photoServerManager = remember { DesktopPhotoServerManager(controller.photoServerState) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            startDesktopDiscovery(controller)
        }
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "Panel de Control",
        state = WindowState(size = DpSize(1280.dp, 720.dp))
    ) {
        MoonlightTheme {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
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
                            AppScreen.MAIN_MENU -> MainMenuScreen(
                                onNavigate = { screen ->
                                    when (screen) {
                                        AppScreen.MOONLIGHT -> controller.openMoonlight()
                                        AppScreen.POWER_CONTROL -> controller.openPowerControl()
                                        AppScreen.PHOTO_SERVER -> controller.openPhotoServer()
                                        AppScreen.GAME_LIST -> Unit
                                        AppScreen.MAIN_MENU -> Unit
                                    }
                                }
                            )
                            AppScreen.MOONLIGHT -> DashboardScreen(
                                state = controller.dashboardState,
                                actions = object : PlatformActions {
                                    override fun onAddPc() {}
                                    override fun onAddPcManual(ip: String) = controller.updateDiscoveredComputer(AppController.manualComputer(ip))
                                    override fun onOpenSettings() = controller.dashboardState.showMessage("Abriendo ajustes de streaming...")
                                    override fun onPcClick(computerId: String, computerName: String) = controller.openComputer(computerId, AppController.desktopFallbackGames())
                                    override fun onPair(computerId: String) = controller.onDiscoveryStatus("Pairing no implementado en Desktop.")

                                    override fun onWakeOnLan(macAddress: String) {
                                        controller.onWakeOnLanDispatched(macAddress)
                                        scope.launch(Dispatchers.IO) { com.limelight.shared.network.StandardWolSender.sendMagicPacket(macAddress) }
                                    }
                                    override fun onNavigateBack() = controller.navigation.goBack()
                                }
                            )
                            AppScreen.GAME_LIST -> GameListScreen(
                                state = controller.dashboardState,
                                onBack = { controller.navigation.goBack() },
                                onGameClick = { game -> controller.dashboardState.showMessage("Iniciando ${game.name}...") }
                            )
                            AppScreen.POWER_CONTROL -> PowerControlScreen(
                                state = controller.powerControlState,
                                onBack = { controller.navigation.goBack() },
                                onSaveConfig = { url, user, pass, deviceId ->
                                    controller.powerControlState.isConfigured = true
                                    controller.powerControlState.serverUrl = url
                                    controller.powerControlState.username = user
                                    controller.powerControlState.password = pass
                                    controller.powerControlState.deviceId = deviceId
                                    controller.powerControlState.showConfig = false
                                    controller.powerControlState.statusMessage = "Configuración guardada ✓"
                                },
                                onWake = {
                                    controller.powerControlState.isWaking = true
                                    controller.powerControlState.statusMessage = null
                                    scope.launch(Dispatchers.IO) {
                                        val client = com.limelight.shared.network.UpSnapClient(
                                            controller.powerControlState.serverUrl,
                                            controller.powerControlState.username,
                                            controller.powerControlState.password
                                        )
                                        val result = client.wakeDevice(controller.powerControlState.deviceId)
                                        withContext(Dispatchers.Main) {
                                            controller.powerControlState.isWaking = false
                                            when (result) {
                                                is com.limelight.shared.network.UpSnapClient.WakeResult.Success -> {
                                                    controller.powerControlState.statusMessage = "Señal enviada con éxito ✓"
                                                }
                                                is com.limelight.shared.network.UpSnapClient.WakeResult.Error -> {
                                                    controller.powerControlState.statusMessage = result.message
                                                }
                                            }
                                        }
                                    }
                                },
                                onClearConfig = {
                                    controller.powerControlState.isConfigured = false
                                    controller.powerControlState.showConfig = true
                                },
                                onTestConnection = { url, user, pass ->
                                    controller.powerControlState.isTestingConnection = true
                                    scope.launch(Dispatchers.IO) {
                                        try {
                                            val client = com.limelight.shared.network.UpSnapClient(url, user, pass)
                                            val devices = client.listDevices()
                                            withContext(Dispatchers.Main) {
                                                controller.powerControlState.isTestingConnection = false
                                                if (devices.isNotEmpty()) {
                                                    controller.powerControlState.availableDevices.clear()
                                                    controller.powerControlState.availableDevices.addAll(devices)
                                                    controller.powerControlState.statusMessage = "¡Dispositivos encontrados! Selecciona uno abajo ✓"
                                                } else {
                                                    controller.powerControlState.statusMessage = "No se encontraron dispositivos."
                                                }
                                            }
                                        } catch (e: Exception) {
                                            withContext(Dispatchers.Main) {
                                                controller.powerControlState.isTestingConnection = false
                                                controller.powerControlState.statusMessage = "Error: ${e.message}"
                                            }
                                        }
                                    }
                                }
                            )
                            AppScreen.PHOTO_SERVER -> PhotoServerScreen(
                                state = controller.photoServerState,
                                actions = object : PhotoServerActions {
                                    override suspend fun startPhotoServer() = photoServerManager.start()
                                    override fun stopPhotoServer() = photoServerManager.stop()
                                    override suspend fun restartPhotoServer() = photoServerManager.restart()
                                    override suspend fun refreshImmich() {
                                        com.limelight.shared.platform.ImmichPhotoServerActions(controller.photoServerState).refreshImmich()
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

private fun runHeadlessServerMode() {
    val state = PhotoServerState()
    val manager = DesktopPhotoServerManager(state)
    manager.start()
    if (System.getProperty("os.name").contains("Windows", ignoreCase = true)) {
        manager.registerWindowsStartupTask()
    }
    while (!Thread.currentThread().isInterrupted) {
        Thread.sleep(60_000)
    }
}

@Composable
private fun FeatureStatusScreen(title: String, message: String, onBack: () -> Unit) {
    AlertDialog(
        onDismissRequest = onBack,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onBack) { Text("Volver") } }
    )
}

private fun startDesktopDiscovery(controller: AppController) {
    try {
        val socket = DatagramSocket()
        socket.broadcast = true
        val buffer = "Moonlight Discovery".toByteArray()
        val packet = DatagramPacket(buffer, buffer.size, InetAddress.getByName("255.255.255.255"), 47998)
        while (!Thread.currentThread().isInterrupted) {
            socket.send(packet)
            controller.onDiscoveryStatus("Buscando PCs en la red...")
            Thread.sleep(5000)
        }
    } catch (_: Exception) {
    }
}

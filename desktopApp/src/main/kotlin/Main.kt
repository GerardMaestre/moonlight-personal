import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
                            override fun onApplyNetworkProfile(profileId: String) {}
                            override fun onWakeOnLan(macAddress: String) {
                                controller.onWakeOnLanDispatched(macAddress)
                                scope.launch(Dispatchers.IO) { sendWolPacket(macAddress) }
                            }
                            override fun onNavigateBack() = controller.navigation.goBack()
                        }
                    )
                    AppScreen.GAME_LIST -> GameListScreen(
                        state = controller.dashboardState,
                        onBack = { controller.navigation.goBack() },
                        onGameClick = { game -> controller.dashboardState.showMessage("Iniciando ${game.name}...") }
                    )
                    AppScreen.POWER_CONTROL -> FeatureStatusScreen(
                        title = "Mi PC",
                        message = controller.featureMessage ?: "Módulo listo",
                        onBack = { controller.navigation.goBack() }
                    )
                    AppScreen.PHOTO_SERVER -> PhotoServerScreen(
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

private fun runHeadlessServerMode() {
    val state = PhotoServerState()
    val manager = DesktopPhotoServerManager(state)
    manager.start()
    if (System.getProperty("os.name").contains("Windows", ignoreCase = true)) {
        manager.registerWindowsStartupTask()
    }
    while (true) {
        Thread.sleep(60_000)
    }
}

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
        while (true) {
            socket.send(packet)
            controller.onDiscoveryStatus("Buscando PCs en la red...")
            Thread.sleep(5000)
        }
    } catch (_: Exception) {
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
    } catch (_: Exception) {
    }
}

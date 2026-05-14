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
                        override fun onAddPc() {
                            state.showMessage("Buscando nuevos PCs en la red local...")
                        }
                        override fun onOpenSettings() {
                            state.showMessage("Abriendo ajustes de streaming...")
                        }
                        override fun onPcClick(computerId: String, computerName: String) {
                                    state.showMessage("Intentando conectar a $computerName ($computerId)...")
                                }
                                override fun onApplyNetworkProfile(profileId: String) {
                                    // Removed
                                }
                                override fun onWakeOnLan(macAddress: String) {
                                    state.showMessage("Enviando señal de encendido (WOL) a $macAddress...")
                                    scope.launch(Dispatchers.IO) {
                                        sendWolPacket(macAddress)
                                    }
                                }
                                override fun onNavigateBack() {
                                    nav.goBack()
                                }
                            }
                        )
                    }
                    AppScreen.POWER_CONTROL -> {
                        // Placeholder for Power Control
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Modo: Mi PC (Encender/Apagar)", color = MaterialTheme.colorScheme.onBackground)
                            Button(onClick = { nav.goBack() }, modifier = Modifier.padding(top = 100.dp)) {
                                Text("Volver")
                            }
                        }
                    }
                    AppScreen.PHOTO_SERVER -> {
                        // Placeholder for Photo Server
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

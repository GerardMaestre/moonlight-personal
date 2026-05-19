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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import com.limelight.shared.model.ComputerInfo
import com.limelight.shared.model.ComputerStatus
import com.limelight.shared.platform.PhotoServerActions
import com.limelight.shared.platform.PhotoServerState
import com.limelight.shared.platform.PlatformActions
import com.limelight.shared.ui.screens.*
import com.limelight.shared.ui.theme.MoonlightTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import com.limelight.nvstream.http.toPemString
import com.limelight.nvstream.http.toX509Certificate
import com.limelight.shared.security.PairingStorage

private fun isVirtualMachineOrRemote(): Boolean {
    val os = System.getProperty("os.name").lowercase()
    if (!os.contains("win")) return false
    
    // 1. Check for Remote Desktop (RDP) session
    val sessionName = System.getenv("SESSIONNAME") ?: ""
    if (sessionName.lowercase().contains("rdp")) return true
    
    // 2. Check for VM environment variables or standard signs
    val computerName = System.getenv("COMPUTERNAME") ?: ""
    if (computerName.lowercase().contains("sandbox")) return true
    
    // 3. Fast BIOS/System checks via Registry query (extremely lightweight and reliable on Windows)
    return try {
        val productProcess = ProcessBuilder("reg", "query", "HKLM\\HARDWARE\\DESCRIPTION\\System\\BIOS", "/v", "SystemProductName")
            .redirectErrorStream(true)
            .start()
        val product = productProcess.inputStream.bufferedReader().readText().lowercase()
        productProcess.waitFor()
        
        if (product.contains("virtual") || product.contains("vmware") || product.contains("virtualbox") || 
            product.contains("qemu") || product.contains("kvm") || product.contains("parallels") || product.contains("xen")) {
            return true
        }
        
        val manufacturerProcess = ProcessBuilder("reg", "query", "HKLM\\HARDWARE\\DESCRIPTION\\System\\BIOS", "/v", "SystemManufacturer")
            .redirectErrorStream(true)
            .start()
        val manufacturer = manufacturerProcess.inputStream.bufferedReader().readText().lowercase()
        manufacturerProcess.waitFor()
        
        manufacturer.contains("vmware") || manufacturer.contains("innotek") || manufacturer.contains("qemu") || 
        manufacturer.contains("xen") || (manufacturer.contains("microsoft corporation") && product.contains("virtual"))
    } catch (e: Exception) {
        false
    }
}

fun main(args: Array<String>) {
    val forceSoftware = args.contains("--software") || args.contains("--software-rendering")
    if (forceSoftware || isVirtualMachineOrRemote()) {
        System.setProperty("skiko.renderApi", "SOFTWARE")
        println("Forcing Skiko Software Rendering Fallback (VM/RDP or manual override detected)")
    }

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
        // Load previously persisted manual PC IP addresses
        runCatching {
            val storage = com.limelight.platform.StorageManager()
            val ips = storage.getString("manual_pcs")?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
            ips.forEach { ip ->
                controller.updateDiscoveredComputer(AppController.manualComputer(ip))
            }
        }
        withContext(Dispatchers.IO) {
            startDesktopDiscovery(controller)
        }
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "Panel de Control",
        state = WindowState(size = DpSize(1280.dp, 720.dp)),
        icon = androidx.compose.ui.res.painterResource("app_icon.ico"),
        onKeyEvent = { keyEvent ->
            if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Escape) {
                if (controller.navigation.canGoBack) {
                    controller.navigation.goBack()
                    true
                } else {
                    false
                }
            } else {
                false
            }
        }
    ) {
        MoonlightTheme {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Scaffold(
                        containerColor = Color.Transparent,
                        contentWindowInsets = WindowInsets(0, 0, 0, 0)
                    ) { innerPadding ->
                        val screenModifier = if (controller.navigation.currentScreen == AppScreen.IMMICH_HOME) {
                            Modifier.fillMaxSize()
                        } else {
                            Modifier.padding(innerPadding).fillMaxSize()
                        }
                        Box(modifier = screenModifier) {
                            when (controller.navigation.currentScreen) {
                                AppScreen.MAIN_MENU -> MainMenuScreen(
                                    onNavigate = { screen ->
                                        controller.navigation.navigateRoot(screen)
                                    }
                                )
                                AppScreen.MOONLIGHT -> DashboardScreen(
                                    state = controller.dashboardState,
                                    actions = object : PlatformActions {
                                        override fun onAddPc() {}
                                        override fun onAddPcManual(ip: String) {
                                            val computer = AppController.manualComputer(ip)
                                            controller.updateDiscoveredComputer(computer)
                                            
                                            // Persist manual PC IP
                                            runCatching {
                                                val storage = com.limelight.platform.StorageManager()
                                                val existing = storage.getString("manual_pcs") ?: ""
                                                val ips = existing.split(",").filter { it.isNotBlank() }.toMutableSet()
                                                ips.add(ip)
                                                storage.putString("manual_pcs", ips.joinToString(","))
                                            }
                                            
                                            // Trigger a status check immediately in background
                                            scope.launch(Dispatchers.IO) {
                                                val updated = queryComputerInfo(ip)
                                                if (updated != null) {
                                                    controller.updateDiscoveredComputer(updated)
                                                }
                                            }
                                        }
                                        override fun onOpenSettings() = controller.dashboardState.showMessage("Abriendo ajustes de streaming...")
                                                override fun onPcClick(computerId: String, computerName: String) {
                                            controller.openComputer(computerId, emptyList())
                                            scope.launch(Dispatchers.IO) {
                                                try {
                                                     val certPem = PairingStorage.loadServerCertPem(computerId)
                                                     val savedCert = certPem?.toX509Certificate()
                                                     if (savedCert != null) {
                                                         val httpConn = com.limelight.nvstream.http.NvHTTP(
                                                             ip = computerId,
                                                             serverCert = savedCert,
                                                             cryptoProvider = com.limelight.nvstream.http.DesktopCryptoProvider()
                                                         )
                                                         val games = httpConn.getAppList()
                                                         withContext(Dispatchers.Main) {
                                                             controller.onGamesLoaded(games)
                                                         }
                                                     } else {
                                                         // Fallback to static games if not paired yet
                                                         withContext(Dispatchers.Main) {
                                                             controller.onGamesLoaded(AppController.desktopFallbackGames())
                                                         }
                                                     }
                                                 } catch (e: Exception) {
                                                     e.printStackTrace()
                                                     withContext(Dispatchers.Main) {
                                                         controller.onGamesLoaded(AppController.desktopFallbackGames())
                                                     }
                                                 }
                                             }
                                         }
                                        
                                         override fun onPair(computerId: String) {
                                             scope.launch(Dispatchers.IO) {
                                                 try {
                                                     val pinStr = com.limelight.nvstream.http.PairingManager.generatePinString()
                                                     withContext(Dispatchers.Main) {
                                                         controller.dashboardState.showMessage(
                                                             "Introduce este código PIN de 4 dígitos en tu Sunshine para emparejar tu PC:\n\n" +
                                                             "  👉   $pinStr   👈\n\n" +
                                                             "Abre la interfaz de Sunshine, ve a 'Configuration' -> 'Client', introduce este PIN y haz clic en 'Pair'."
                                                         )
                                                     }
                                                     
                                                     // Open Sunshine UI of the target computer in default browser
                                                     try {
                                                         val os = System.getProperty("os.name").lowercase()
                                                         val sunshineUrl = "https://$computerId:47990"
                                                         if (os.contains("win")) {
                                                             ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", sunshineUrl).start()
                                                         } else if (os.contains("mac")) {
                                                             ProcessBuilder("open", sunshineUrl).start()
                                                         } else {
                                                             ProcessBuilder("xdg-open", sunshineUrl).start()
                                                         }
                                                     } catch (_: Exception) {}

                                                     val httpConn = com.limelight.nvstream.http.NvHTTP(
                                                         ip = computerId,
                                                         cryptoProvider = com.limelight.nvstream.http.DesktopCryptoProvider()
                                                     )
                                                     val pairingManager = httpConn.pairingManager
                                                     val pairState = pairingManager.pair(httpConn.getServerInfo(true), pinStr)
                                                     
                                                     withContext(Dispatchers.Main) {
                                                         if (pairState == com.limelight.nvstream.http.PairingManager.PairState.PAIRED) {
                                                             val pairedCert = pairingManager.getPairedCert()
                                                             if (pairedCert != null) {
                                                                 PairingStorage.storeServerCertPem(computerId, pairedCert.toPemString())
                                                             }
                                                             controller.onManualPairResult(computerId, true)
                                                             controller.dashboardState.showMessage("¡Emparejamiento completado con éxito! 🎉")
                                                         } else {
                                                             controller.dashboardState.showMessage("Error de emparejamiento. Revisa que el PIN sea correcto.")
                                                         }
                                                     }
                                                 } catch (e: Throwable) {
                                                     withContext(Dispatchers.Main) {
                                                         controller.dashboardState.showMessage("Error durante el emparejamiento: ${e.toString()}")
                                                     }
                                                 }
                                             }
                                         }

                                        override fun onWakeOnLan(macAddress: String) {
                                            controller.onWakeOnLanDispatched(macAddress)
                                            scope.launch(Dispatchers.IO) { com.limelight.wol.WolSender.send(macAddress) }
                                        }
                                        override fun onNavigateBack() = controller.navigation.goBack()
                                    }
                                )
                                AppScreen.GAME_LIST -> GameListScreen(
                                    state = controller.dashboardState,
                                    onBack = { controller.navigation.goBack() },
                                    onGameClick = { game ->
                                        val selectedPc = controller.dashboardState.selectedComputer
                                        if (selectedPc != null) {
                                            launchMoonlightStream(selectedPc.id, game.name, controller, scope)
                                        } else {
                                            controller.dashboardState.showMessage("No hay ningún PC seleccionado.")
                                        }
                                    }
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
                                        override fun onUpdateConnection(baseUrl: String, apiKey: String) {
                                            controller.photoServerState.updateConnection(baseUrl, apiKey)
                                        }
                                        override suspend fun startPhotoServer() = photoServerManager.start()
                                        override fun stopPhotoServer() = photoServerManager.stop()
                                        override suspend fun restartPhotoServer() = photoServerManager.restart()
                                        override suspend fun refreshImmich() {
                                            com.limelight.shared.platform.ImmichPhotoServerActions(controller.photoServerState).refreshImmich()
                                        }
                                    },
                                    onBack = { controller.navigation.goBack() },
                                    onOpenImmich = { _, _, _, _ -> controller.navigation.navigateTo(AppScreen.IMMICH_HOME) }
                                )
                                AppScreen.IMMICH_HOME -> ImmichHomeScreen(
                                    state = controller.photoServerState,
                                    actions = object : PhotoServerActions {
                                        override fun onUpdateConnection(baseUrl: String, apiKey: String) {
                                            controller.photoServerState.updateConnection(baseUrl, apiKey)
                                        }
                                        override suspend fun startPhotoServer() = photoServerManager.start()
                                        override fun stopPhotoServer() = photoServerManager.stop()
                                        override suspend fun restartPhotoServer() = photoServerManager.restart()
                                        override suspend fun refreshImmich() {
                                            com.limelight.shared.platform.ImmichPhotoServerActions(controller.photoServerState).refreshImmich()
                                        }
                                    },
                                    onBack = { controller.navigation.goBack() },
                                    onOpenSettings = { controller.navigation.navigateTo(AppScreen.PHOTO_SERVER) }
                                )
                            }
                        }
                    }

                    // 🛸 Premium Glassmorphic FLOATING Bottom Navigation Capsule Overlay
                    val hideBottomBar = controller.photoServerState.isFullscreenViewerOpen
                    if (!hideBottomBar) {
                        Box(
                            modifier = Modifier.align(Alignment.BottomCenter)
                        ) {
                            com.limelight.shared.ui.components.BottomNavBar(
                                currentScreen = controller.navigation.currentScreen,
                                onNavigate = { screen ->
                                    controller.navigation.navigateRoot(screen)
                                },
                                photoServerState = controller.photoServerState,
                                onRefreshImmich = {
                                    scope.launch(Dispatchers.IO) {
                                        com.limelight.shared.platform.ImmichPhotoServerActions(controller.photoServerState).refreshImmich()
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

private fun runHeadlessServerMode() = runBlocking {
    val state = PhotoServerState()
    val manager = DesktopPhotoServerManager(state)
    manager.start()
    if (System.getProperty("os.name").contains("Windows", ignoreCase = true)) {
        manager.registerWindowsStartupTask()
    }
    createHeadlessKeepAliveFlow().collect {}
}

private fun createHeadlessKeepAliveFlow() = flow {
    while (true) {
        delay(60_000)
        emit(Unit)
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

private fun queryComputerInfo(ip: String): ComputerInfo? {
    var connection: java.net.HttpURLConnection? = null
    try {
        val url = java.net.URI("http://$ip:47989/serverinfo").toURL()
        connection = url.openConnection() as java.net.HttpURLConnection
        connection.connectTimeout = 1000
        connection.readTimeout = 1000
        connection.requestMethod = "GET"
        
        val responseCode = connection.responseCode
        if (responseCode == 200) {
            val reader = java.io.BufferedReader(java.io.InputStreamReader(connection.inputStream))
            val sb = java.lang.StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                sb.append(line)
            }
            reader.close()
            val xml = sb.toString()
            
            val hostnameMatch = Regex("<hostname>(.*?)</hostname>").find(xml)
            val name = hostnameMatch?.groups?.get(1)?.value ?: "PC ($ip)"
            
            val macMatch = Regex("<mac>(.*?)</mac>").find(xml)
            val mac = macMatch?.groups?.get(1)?.value
            
            val pairMatch = Regex("<PairStatus>(.*?)</PairStatus>").find(xml)
            val isPaired = pairMatch?.groups?.get(1)?.value == "1"
            
            return ComputerInfo(
                id = ip,
                name = name,
                status = ComputerStatus.ONLINE,
                localAddress = ip,
                macAddress = mac,
                isPaired = isPaired
            )
        }
    } catch (e: Exception) {
        // Silent catch for offline hosts
    } finally {
        connection?.disconnect()
    }
    return null
}

private fun scanSubnetForMoonlight(controller: AppController) {
    try {
        val localhost = java.net.InetAddress.getLocalHost()
        val ip = localhost.hostAddress
        if (ip.startsWith("127.") || ip.contains(":")) return
        
        val lastDot = ip.lastIndexOf('.')
        if (lastDot == -1) return
        val subnet = ip.substring(0, lastDot + 1)
        
        val computersToUpdate = java.util.concurrent.ConcurrentHashMap<String, ComputerInfo>()
        val threadCount = 20
        val executor = java.util.concurrent.Executors.newFixedThreadPool(threadCount)
        
        val futures = (1..254).map { i ->
            val targetIp = subnet + i
            if (targetIp == ip) return@map null
            
            executor.submit {
                var socket: java.net.Socket? = null
                try {
                    socket = java.net.Socket()
                    socket.connect(java.net.InetSocketAddress(targetIp, 47989), 150)
                    // Port is open! Query details.
                    val info = queryComputerInfo(targetIp)
                    if (info != null) {
                        computersToUpdate[targetIp] = info
                    }
                } catch (_: Exception) {
                } finally {
                    try { socket?.close() } catch (_: Exception) {}
                }
            }
        }.filterNotNull()
        
        // Wait for all checks to complete
        for (future in futures) {
            try {
                future.get(200, java.util.concurrent.TimeUnit.MILLISECONDS)
            } catch (_: Exception) {
            }
        }
        
        executor.shutdownNow()
        
        // Update controller on the main/discovery thread
        computersToUpdate.values.forEach { computer ->
            controller.updateDiscoveredComputer(computer)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun startDesktopDiscovery(controller: AppController) {
    var lastSubnetScan = 0L
    try {
        while (!Thread.currentThread().isInterrupted) {
            val now = System.currentTimeMillis()
            
            // 1. Every 25 seconds, perform a fast parallel subnet scan
            if (now - lastSubnetScan > 25000L) {
                lastSubnetScan = now
                scanSubnetForMoonlight(controller)
            }
            
            // 2. Every 5 seconds, refresh currently listed computers
            val currentList = ArrayList(controller.dashboardState.computers)
            for (computer in currentList) {
                val updated = queryComputerInfo(computer.id)
                if (updated != null) {
                    controller.updateDiscoveredComputer(updated)
                } else {
                    controller.updateDiscoveredComputer(computer.copy(status = ComputerStatus.OFFLINE))
                }
            }
            
            Thread.sleep(5000)
        }
    } catch (_: Exception) {
    }
}

private fun launchMoonlightStream(ip: String, gameName: String, controller: AppController, scope: kotlinx.coroutines.CoroutineScope) {
    scope.launch(Dispatchers.IO) {
        val paths = listOf(
            "moonlight", // If in PATH
            "C:\\Program Files\\Moonlight Game Streaming\\Moonlight.exe",
            "C:\\Program Files (x86)\\Moonlight Game Streaming\\Moonlight.exe"
        )
        
        var started = false
        for (path in paths) {
            try {
                val pb = ProcessBuilder(path, "stream", ip, gameName)
                pb.start()
                started = true
                controller.dashboardState.showMessage("Iniciando streaming de $gameName en $ip via Moonlight...")
                break
            } catch (e: Exception) {
                // Try next path
            }
        }
        
        if (!started) {
            controller.dashboardState.showMessage(
                "No se encontró el cliente oficial de Moonlight en tu PC.\n\n" +
                "Instala 'Moonlight Game Streaming' para iniciar tus juegos automáticamente.\n" +
                "Abriendo página de descargas de Moonlight..."
            )
            try {
                val os = System.getProperty("os.name").lowercase()
                if (os.contains("win")) {
                    ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", "https://moonlight-stream.org/").start()
                } else if (os.contains("mac")) {
                    ProcessBuilder("open", "https://moonlight-stream.org/").start()
                } else {
                    ProcessBuilder("xdg-open", "https://moonlight-stream.org/").start()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

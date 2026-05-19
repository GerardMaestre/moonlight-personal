package com.limelight.ui.premium

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.WindowInsets
import com.limelight.di.UpSnapClientFactory
import com.limelight.preferences.StreamSettings
import com.limelight.shared.network.RemoteScriptClient
import com.limelight.shared.network.WakeService
import com.limelight.shared.platform.PhotoServerActions
import com.limelight.shared.platform.PlatformActions
import com.limelight.shared.platform.StartCommandResult
import com.limelight.shared.ui.screens.AppScreen
import com.limelight.shared.ui.screens.DashboardScreen
import com.limelight.shared.ui.screens.GameListScreen
import com.limelight.shared.ui.screens.ImmichHomeScreen
import com.limelight.shared.ui.screens.MainMenuScreen
import com.limelight.shared.ui.screens.PhotoServerScreen
import com.limelight.shared.ui.screens.PowerControlScreen
import com.limelight.shared.ui.screens.PowerControlState
import com.limelight.shared.ui.theme.MoonlightTheme
import org.koin.android.ext.android.inject
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.limelight.shared.network.immich.ImmichApiClient
import kotlin.concurrent.thread

class PremiumDashboardActivity : ComponentActivity() {
    private val upSnapClientFactory: UpSnapClientFactory by inject()
    private val viewModel: PremiumDashboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        com.limelight.utils.UiHelper.setImmersiveMode(this)

        setContent {
            val controller = viewModel.controller
            MoonlightTheme {
                androidx.activity.compose.BackHandler(enabled = controller.navigation.currentScreen != AppScreen.MAIN_MENU) {
                    if (controller.navigation.currentScreen == AppScreen.POWER_CONTROL) {
                        handlePowerBack(controller.powerControlState)
                    } else {
                        navigateBackOrHome()
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
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
                                AppScreen.MAIN_MENU -> {
                                    MainMenuScreen(
                                        onNavigate = { screen -> controller.navigation.navigateRoot(screen) }
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
                                            }
                                            override fun onPair(id: String) {
                                                viewModel.pair(id, this@PremiumDashboardActivity)
                                            }
                                            override fun onWakeOnLan(macAddress: String) {
                                                controller.onWakeOnLanDispatched(macAddress)
                                                thread { WakeService.wakeUdp(macAddress) }
                                            }
                                            override fun onNavigateBack() {
                                                navigateBackOrHome()
                                            }
                                        }
                                    )
                                }
                                AppScreen.GAME_LIST -> {
                                    GameListScreen(
                                        state = controller.dashboardState,
                                        onBack = { navigateBackOrHome() },
                                        onGameClick = { game ->
                                            val binder = viewModel.getBinder()
                                            val computer = controller.dashboardState.selectedComputer
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
                                    val powerState = controller.powerControlState

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
                                        onBack = { handlePowerBack(powerState) },
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
                                            val serverUrl = powerState.serverUrl
                                            val username = powerState.username
                                            val password = powerState.password
                                            val deviceId = powerState.deviceId
                                            val macAddress = controller.dashboardState.selectedComputer?.macAddress
                                            powerState.isWaking = true
                                            powerState.statusMessage = null
                                            thread {
                                                val result = try {
                                                    val client = upSnapClientFactory.create(serverUrl, username, password)
                                                    WakeService.wakeWithFallback(
                                                        upSnapClient = client,
                                                        deviceId = deviceId,
                                                        macAddress = macAddress,
                                                    )
                                                } catch (e: Exception) {
                                                    WakeService.WakeOutcome.Failure("Error al enviar WOL: ${e.message ?: "desconocido"}")
                                                }
                                                postToUiIfActive {
                                                    powerState.isWaking = false
                                                    powerState.statusMessage = when (result) {
                                                        is WakeService.WakeOutcome.Success -> "Señal enviada correctamente ✓"
                                                        is WakeService.WakeOutcome.Failure -> result.reason
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
                                                    val client = upSnapClientFactory.create(url, user, password)
                                                    val devices = client.listDevices()
                                                    postToUiIfActive {
                                                        powerState.isTestingConnection = false
                                                        powerState.availableDevices.clear()
                                                        if (devices.isNotEmpty()) {
                                                            powerState.availableDevices.addAll(devices)
                                                            powerState.statusMessage = "¡Dispositivos encontrados! ✓"
                                                        } else {
                                                            powerState.statusMessage = "No se encontraron dispositivos."
                                                        }
                                                    }
                                                } catch (e: Exception) {
                                                    println("UpSnap Test Error: ${e.message}")
                                                    postToUiIfActive {
                                                        powerState.isTestingConnection = false
                                                        powerState.statusMessage = "Error: ${e.message ?: "desconocido"}"
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
                                    val photoManager = remember {
                                        AndroidPhotoServerManager(controller.photoServerState) { update ->
                                            postToUiIfActive(update)
                                        }
                                    }
                                    val pcIp = remember(config.serverUrl) {
                                        try {
                                            java.net.URL(config.serverUrl).host
                                        } catch (_: Exception) {
                                            "100.67.140.39"
                                        }
                                    }

                                    LaunchedEffect(Unit) {
                                        val immichConfig = ImmichConfig.getInstance(this@PremiumDashboardActivity)
                                        controller.photoServerState.updateConnection(
                                            baseUrl = if (immichConfig.baseUrl.isBlank()) "http://$pcIp:2283" else immichConfig.baseUrl,
                                            apiKey = immichConfig.apiKey
                                        )
                                        thread { photoManager.checkHealth(pcIp) }
                                    }

                                    PhotoServerScreen(
                                        state = controller.photoServerState,
                                        actions = object : PhotoServerActions {
                                            override fun onUpdateConnection(baseUrl: String, apiKey: String) {
                                                controller.photoServerState.updateConnection(baseUrl, apiKey)
                                                val immichConfig = ImmichConfig.getInstance(this@PremiumDashboardActivity)
                                                immichConfig.baseUrl = baseUrl
                                                immichConfig.apiKey = apiKey
                                            }
                                            override suspend fun startPhotoServer(): StartCommandResult {
                                                thread { photoManager.start(remoteClient, pcIp) }
                                                return StartCommandResult.Success
                                            }
                                            override fun stopPhotoServer() {
                                                photoManager.stop(remoteClient)
                                            }
                                            override suspend fun restartPhotoServer(): StartCommandResult {
                                                thread { photoManager.restart(remoteClient, pcIp) }
                                                return StartCommandResult.Success
                                            }
                                            override suspend fun refreshImmich() {
                                                thread { photoManager.refreshImmich() }
                                            }
                                        },
                                        onBack = { navigateBackOrHome() },
                                        onOpenImmich = { left, top, width, height ->
                                            controller.navigation.navigateTo(AppScreen.IMMICH_HOME)
                                        }
                                    )
                                }
                                AppScreen.IMMICH_HOME -> {
                                    val config = remember { RemoteScriptConfig.getInstance(this@PremiumDashboardActivity) }
                                    val remoteClient = remember(config.serverUrl, config.token) {
                                        RemoteScriptClient(config.serverUrl, config.token)
                                    }
                                    val photoManager = remember {
                                        AndroidPhotoServerManager(controller.photoServerState) { update ->
                                            postToUiIfActive(update)
                                        }
                                    }
                                    val pcIp = remember(config.serverUrl) {
                                        try {
                                            java.net.URL(config.serverUrl).host
                                        } catch (_: Exception) {
                                            "100.67.140.39"
                                        }
                                    }

                                    val context = this@PremiumDashboardActivity
                                    val coroutineScope = rememberCoroutineScope()
                                    var onProgressCallback by remember { mutableStateOf<((String?) -> Unit)?>(null) }

                                    val pickMedia = androidx.activity.compose.rememberLauncherForActivityResult(
                                         contract = androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia(100),
                                         onResult = { uris ->
                                             if (!uris.isNullOrEmpty()) {
                                                 val callback = onProgressCallback
                                                 callback?.invoke("Preparando subida...")

                                                 // Perform sequential stream reading and network upload in background safely to prevent OOM memory issues!
                                                 coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                                     var uploadedCount = 0
                                                     var failedCount = 0
                                                     val totalCount = uris.size
                                                     
                                                     withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                         callback?.invoke("Subiendo 0 de $totalCount fotos...")
                                                         android.widget.Toast.makeText(
                                                             context,
                                                             "Preparando la subida de $totalCount fotos...",
                                                             android.widget.Toast.LENGTH_SHORT
                                                         ).show()
                                                     }
                                                     
                                                     try {
                                                         val client = ImmichApiClient()
                                                         val contentResolver = context.contentResolver
                                                         
                                                         uris.forEachIndexed { index, uri ->
                                                             // Update the floating capsule text immediately on main thread for real-time progress!
                                                             withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                                 callback?.invoke("Subiendo ${index + 1} de $totalCount fotos...")
                                                             }
                                                             
                                                             var name = "upload_${System.currentTimeMillis()}.jpg"
                                                             var dateTaken: Long = 0L
                                                             var mimeType = "image/jpeg"
                                                             
                                                             try {
                                                                 // 1. Read metadata (name, date_taken) in a highly lightweight way
                                                                 val cursor = contentResolver.query(uri, null, null, null, null)
                                                                 cursor?.use { c ->
                                                                     if (c.moveToFirst()) {
                                                                         val nameIndex = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                                                                         if (nameIndex != -1) {
                                                                             name = c.getString(nameIndex)
                                                                         }
                                                                         
                                                                         val dateTakenIndex = c.getColumnIndex("datetaken")
                                                                         if (dateTakenIndex != -1) {
                                                                             dateTaken = c.getLong(dateTakenIndex)
                                                                         }
                                                                         if (dateTaken <= 0L) {
                                                                             val dateModifiedIndex = c.getColumnIndex("date_modified")
                                                                             if (dateModifiedIndex != -1) {
                                                                                 dateTaken = c.getLong(dateModifiedIndex) * 1000L
                                                                             }
                                                                         }
                                                                     }
                                                                 }
                                                                 mimeType = contentResolver.getType(uri) ?: "image/jpeg"
                                                             } catch (e: Exception) {
                                                                 android.util.Log.e("ImmichUpload", "Error reading metadata for URI: $uri", e)
                                                             }
                                                             
                                                             // 2. Format epoch millis to ISO-8601 for Immich using standard Java API
                                                             val createdAtStr = if (dateTaken > 0L) {
                                                                 try {
                                                                     java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
                                                                         timeZone = java.util.TimeZone.getTimeZone("UTC")
                                                                     }.format(java.util.Date(dateTaken))
                                                                 } catch (e: Exception) {
                                                                     null
                                                                 }
                                                             } else null
                                                             
                                                             // 3. Read bytes of ONLY the current photo into memory
                                                             var bytes: ByteArray? = null
                                                             try {
                                                                 bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                                                             } catch (e: Exception) {
                                                                 android.util.Log.e("ImmichUpload", "Error reading stream for URI: $uri", e)
                                                             }
                                                             
                                                             // 4. Upload photo if bytes are present, and immediately null out reference to free memory!
                                                             if (bytes != null) {
                                                                 try {
                                                                     val uploadSuccess = client.uploadAsset(
                                                                         config = controller.photoServerState.connectionConfig,
                                                                         fileName = name,
                                                                         fileBytes = bytes,
                                                                         mimeType = mimeType,
                                                                         createdAt = createdAtStr
                                                                     )
                                                                     if (uploadSuccess) {
                                                                         uploadedCount++
                                                                         // Refresh dynamically after each upload so photos pop up in gallery one-by-one!
                                                                         photoManager.refreshImmich()
                                                                     } else {
                                                                         failedCount++
                                                                     }
                                                                     android.util.Log.d("ImmichUpload", "Upload success: $uploadSuccess for $name with date $createdAtStr (${index + 1}/$totalCount)")
                                                                 } catch (e: Exception) {
                                                                     failedCount++
                                                                     android.util.Log.e("ImmichUpload", "Failed to upload photo: $name", e)
                                                                 } finally {
                                                                     // Crucial OOM Prevention: Discard byte array immediately so Garbage Collector cleans it up!
                                                                     bytes = null
                                                                 }
                                                             } else {
                                                                 failedCount++
                                                             }
                                                         }
                                                     } catch (e: Exception) {
                                                         android.util.Log.e("ImmichUpload", "Error in sequential upload loop", e)
                                                     } finally {
                                                         withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                             if (failedCount > 0) {
                                                                 android.widget.Toast.makeText(
                                                                     context,
                                                                     "Subidas $uploadedCount fotos ($failedCount fallidas)",
                                                                     android.widget.Toast.LENGTH_LONG
                                                                 ).show()
                                                             } else {
                                                                 android.widget.Toast.makeText(
                                                                     context,
                                                                     "Â¡Se han subido las $uploadedCount fotos con Ã©xito y metadatos preservados!",
                                                                     android.widget.Toast.LENGTH_LONG
                                                                 ).show()
                                                             }
                                                             // Reset progress capsule to null to hide it!
                                                             callback?.invoke(null)
                                                         }
                                                     }
                                                 }
                                             }
                                         }
                                     )

                                    ImmichHomeScreen(
                                        state = controller.photoServerState,
                                        actions = object : PhotoServerActions {
                                            override fun onUpdateConnection(baseUrl: String, apiKey: String) {
                                                controller.photoServerState.updateConnection(baseUrl, apiKey)
                                                val immichConfig = ImmichConfig.getInstance(this@PremiumDashboardActivity)
                                                immichConfig.baseUrl = baseUrl
                                                immichConfig.apiKey = apiKey
                                            }
                                            override suspend fun startPhotoServer(): StartCommandResult {
                                                thread { photoManager.start(remoteClient, pcIp) }
                                                return StartCommandResult.Success
                                            }
                                            override fun stopPhotoServer() {
                                                photoManager.stop(remoteClient)
                                            }
                                            override suspend fun refreshImmich() {
                                                thread { photoManager.refreshImmich() }
                                            }
                                        },
                                        onBack = { navigateBackOrHome() },
                                        onOpenSettings = { controller.navigation.navigateTo(AppScreen.PHOTO_SERVER) },
                                        onPickAndUploadPhoto = { progressCallback ->
                                            onProgressCallback = progressCallback
                                            pickMedia.launch(androidx.activity.result.PickVisualMediaRequest(
                                                androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                                            ))
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // 🛸 Premium Glassmorphic FLOATING Bottom Navigation Capsule Overlay
                    val hideBottomBar = controller.photoServerState.isFullscreenViewerOpen
                    if (!hideBottomBar) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                        ) {
                            com.limelight.shared.ui.components.BottomNavBar(
                                currentScreen = controller.navigation.currentScreen,
                                onNavigate = { screen -> controller.navigation.navigateRoot(screen) },
                                photoServerState = controller.photoServerState,
                                onRefreshImmich = {
                                    val config = RemoteScriptConfig.getInstance(this@PremiumDashboardActivity)
                                    val pcIp = try { java.net.URL(config.serverUrl).host } catch (_: Exception) { "100.67.140.39" }
                                    thread {
                                        val manager = AndroidPhotoServerManager(controller.photoServerState) { runOnUiThread(it) }
                                        manager.refreshImmich()
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

    private fun postToUiIfActive(update: () -> Unit) {
        if (!isFinishing && !isDestroyed) {
            runOnUiThread(update)
        }
    }

    private fun handlePowerBack(powerState: PowerControlState) {
        if (powerState.showConfig && powerState.isConfigured) {
            powerState.showConfig = false
        } else {
            navigateBackOrHome()
        }
    }

    private fun navigateBackOrHome() {
        val navigation = viewModel.controller.navigation
        if (navigation.canGoBack) {
            navigation.goBack()
        } else {
            navigation.navigateRoot(AppScreen.MAIN_MENU)
        }
    }
}

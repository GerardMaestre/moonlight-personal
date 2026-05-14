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
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Cancel
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
                                    powerState.password = pass // Keep it in state
                                    powerState.deviceId = deviceId
                                    powerState.isEnabled = true
                                    powerState.showConfig = false
                                    powerState.statusMessage = "Configuración guardada ✓"
                                },
                                onWake = {
                                    val config = UpSnapConfig.getInstance(this@PremiumDashboardActivity)
                                    if (!config.isConfigured) {
                                        powerState.statusMessage = "Error: UpSnap no configurado"
                                        return@PowerControlScreen
                                    }
                                    
                                    powerState.isWaking = true
                                    powerState.statusMessage = null
                                    
                                    thread {
                                        val username = config.getUsername() ?: ""
                                        val password = config.getPassword() ?: ""
                                        val client = UpSnapClient(config.serverUrl, username, password)
                                        val result = client.wakeDevice(config.deviceId)
                                        
                                        runOnUiThread {
                                            powerState.isWaking = false
                                            when (result) {
                                                is UpSnapClient.WakeResult.Success -> {
                                                    powerState.statusMessage = "Señal WoL enviada correctamente ✓"
                                                }
                                                is UpSnapClient.WakeResult.Error -> {
                                                    powerState.statusMessage = result.message
                                                }
                                            }
                                        }
                                    }
                                },
                                onClearConfig = {
                                    val config = UpSnapConfig.getInstance(this@PremiumDashboardActivity)
                                    config.clear()
                                    powerState.isConfigured = false
                                    powerState.serverUrl = ""
                                    powerState.username = ""
                                    powerState.password = ""
                                    powerState.deviceId = ""
                                    powerState.isEnabled = false
                                    powerState.statusMessage = null
                                    powerState.showConfig = true
                                },
                                onTestConnection = { url, user, password ->
                                    powerState.isTestingConnection = true
                                    powerState.statusMessage = null
                                    powerState.availableDevices.clear()
                                    
                                    thread {
                                        val client = UpSnapClient(url, user, password)
                                        val devices = client.listDevices()
                                        
                                        runOnUiThread {
                                            powerState.isTestingConnection = false
                                            if (devices.isNotEmpty()) {
                                                powerState.availableDevices.addAll(devices)
                                                powerState.statusMessage = "¡Dispositivos encontrados! Selecciona el tuyo abajo ✓"
                                            } else {
                                                // If list is empty, it could be wrong auth or wrong URL
                                                powerState.statusMessage = "Error: No se han encontrado dispositivos. Revisa usuario, contraseña y URL."
                                            }
                                        }
                                    }
                                }
                            )
                        }
                        AppScreen.PHOTO_SERVER -> {
                            var immichStatus by remember { mutableStateOf<String?>(null) }
                            
                            Column(
                                modifier = Modifier.fillMaxSize().padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.CloudSync,
                                    contentDescription = "Immich",
                                    modifier = Modifier.size(80.dp),
                                    tint = com.limelight.shared.ui.theme.MoonlightColors.Cyan
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    "Servidor de Fotos",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "Arranca el stack de contenedores de Immich en el ordenador remoto sin mostrar ventanas visibles.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(48.dp))
                                
                                FilledTonalButton(
                                    onClick = {
                                        immichStatus = "Iniciando Immich en el servidor..."
                                        thread {
                                            try {
                                                // IP directa del PC con Stream Deck (Tailscale)
                                                val streamDeckUrl = "http://100.67.140.39:3000/api/run-script"
                                                
                                                val json = org.json.JSONObject()
                                                json.put("carpeta", "07_Personalizacion")
                                                json.put("archivo", "fotos.bat")
                                                
                                                val mediaType = "application/json; charset=utf-8".toMediaType()
                                                val body = json.toString().toRequestBody(mediaType)
                                                
                                                val request = okhttp3.Request.Builder()
                                                    .url(streamDeckUrl)
                                                    .post(body)
                                                    .addHeader("Authorization", "Bearer CasaGerard")
                                                    .build()
                                                    
                                                val client = okhttp3.OkHttpClient()
                                                val response = client.newCall(request).execute()
                                                
                                                runOnUiThread {
                                                    if (response.isSuccessful) {
                                                        immichStatus = "¡Servidor Immich arrancado correctamente! ✓"
                                                    } else {
                                                        immichStatus = "Error al iniciar Immich: HTTP ${response.code}"
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                                runOnUiThread {
                                                    immichStatus = "Error de red: No se pudo contactar con Stream Deck local (puerto 3000)."
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = com.limelight.shared.ui.theme.MoonlightColors.Cyan.copy(alpha = 0.2f),
                                        contentColor = com.limelight.shared.ui.theme.MoonlightColors.Cyan
                                    ),
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                                ) {
                                    Icon(androidx.compose.material.icons.Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                FilledTonalButton(
                                    onClick = {
                                        immichStatus = "Deteniendo Immich en el servidor..."
                                        thread {
                                            try {
                                                val streamDeckUrl = "http://100.67.140.39:3000/api/run-script"
                                                
                                                val json = org.json.JSONObject()
                                                json.put("carpeta", "07_Personalizacion")
                                                json.put("archivo", "cerrar_fotos.bat")
                                                
                                                val mediaType = "application/json; charset=utf-8".toMediaType()
                                                val body = json.toString().toRequestBody(mediaType)
                                                
                                                val request = okhttp3.Request.Builder()
                                                    .url(streamDeckUrl)
                                                    .post(body)
                                                    .addHeader("Authorization", "Bearer CasaGerard")
                                                    .build()
                                                    
                                                val client = okhttp3.OkHttpClient()
                                                val response = client.newCall(request).execute()
                                                
                                                runOnUiThread {
                                                    if (response.isSuccessful) {
                                                        immichStatus = "¡Servidor Immich detenido correctamente! ✓"
                                                    } else {
                                                        immichStatus = "Error al detener Immich: HTTP ${response.code}"
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                                runOnUiThread {
                                                    immichStatus = "Error de red: No se pudo contactar con Stream Deck local (puerto 3000)."
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = com.limelight.shared.ui.theme.MoonlightColors.Red.copy(alpha = 0.2f),
                                        contentColor = com.limelight.shared.ui.theme.MoonlightColors.Red
                                    ),
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                                ) {
                                    Icon(androidx.compose.material.icons.Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("APAGAR SERVIDOR IMMICH", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                }
                                
                                immichStatus?.let { msg ->
                                    Spacer(modifier = Modifier.height(24.dp))
                                    val isError = msg.startsWith("Error")
                                    androidx.compose.material3.Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                                        colors = androidx.compose.material3.CardDefaults.cardColors(
                                            containerColor = if (isError) com.limelight.shared.ui.theme.MoonlightColors.Red.copy(alpha = 0.1f) else com.limelight.shared.ui.theme.MoonlightColors.Green.copy(alpha = 0.1f)
                                        )
                                    ) {
                                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                if (isError) androidx.compose.material.icons.Icons.Default.Error else androidx.compose.material.icons.Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = if (isError) com.limelight.shared.ui.theme.MoonlightColors.Red else com.limelight.shared.ui.theme.MoonlightColors.Green,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(msg, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(48.dp))
                                TextButton(onClick = { nav.goBack() }) {
                                    Text("Volver", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

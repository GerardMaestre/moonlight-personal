package com.limelight.ui.premium

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limelight.shared.ui.theme.MoonlightColors

/**
 * State holder for the Power Control screen.
 */
class PowerControlState {
    var isConfigured by mutableStateOf(false)
    var serverUrl by mutableStateOf("")
    var username by mutableStateOf("")
    var password by mutableStateOf("")
    var deviceId by mutableStateOf("")
    var deviceName by mutableStateOf("")
    var isWaking by mutableStateOf(false)
    var statusMessage by mutableStateOf<String?>(null)
    var showConfig by mutableStateOf(false)
    var isEnabled by mutableStateOf(false)
    
    // New: List of devices found on the server
    var availableDevices = mutableStateListOf<Pair<String, String>>()
    var isTestingConnection by mutableStateOf(false)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PowerControlScreen(
    state: PowerControlState,
    onBack: () -> Unit,
    onSaveConfig: (url: String, user: String, pass: String, deviceId: String) -> Unit,
    onWake: () -> Unit,
    onClearConfig: () -> Unit,
    onTestConnection: (url: String, user: String, pass: String) -> Unit,
    onStartImmich: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "MI PC",
                        fontWeight = FontWeight.Black,
                        letterSpacing = 3.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { state.showConfig = !state.showConfig }) {
                        Icon(
                            if (state.showConfig) Icons.Default.Close else Icons.Default.Settings,
                            contentDescription = "Configuración"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MoonlightColors.Purple,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (state.showConfig) {
                ConfigSection(state, onSaveConfig, onClearConfig, onTestConnection)
            } else {
                WakeSection(state, onWake, onStartImmich)
            }
        }
    }
}

@Composable
private fun WakeSection(
    state: PowerControlState,
    onWake: () -> Unit,
    onStartImmich: () -> Unit
) {
    Spacer(modifier = Modifier.height(48.dp))

    if (!state.isConfigured) {
        // Not configured state
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            )
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.SettingsSuggest,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MoonlightColors.Outline
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "UpSnap no configurado",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Pulsa el icono de ajustes ⚙ arriba para configurar tu servidor UpSnap y poder encender tu PC de forma remota.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    // Big power button
    val pulseAnim = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulseAnim.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val buttonColor by animateColorAsState(
        targetValue = if (state.isWaking) MoonlightColors.Amber else MoonlightColors.Green,
        animationSpec = tween(500),
        label = "buttonColor"
    )

    Text(
        state.deviceName.ifEmpty { "Mi PC" },
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        "Control de Energía vía UpSnap",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(48.dp))

    // Power button
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(200.dp)
            .scale(if (!state.isWaking) pulseScale else 1f)
    ) {
        // Outer glow ring
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            buttonColor.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Main button
        FilledIconButton(
            onClick = { if (!state.isWaking) onWake() },
            modifier = Modifier.size(140.dp),
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = buttonColor.copy(alpha = 0.2f),
                contentColor = buttonColor
            )
        ) {
            if (state.isWaking) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = buttonColor,
                    strokeWidth = 3.dp
                )
            } else {
                Icon(
                    Icons.Default.PowerSettingsNew,
                    contentDescription = "Encender PC",
                    modifier = Modifier.size(64.dp)
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    Text(
        if (state.isWaking) "Enviando señal de encendido..." else "Pulsa para encender",
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Medium,
        color = if (state.isWaking) MoonlightColors.Amber else MaterialTheme.colorScheme.onSurfaceVariant
    )

    // Status message
    state.statusMessage?.let { msg ->
        Spacer(modifier = Modifier.height(24.dp))
        val isError = msg.startsWith("Error") || msg.startsWith("No se") || msg.contains("incorrecta")
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isError)
                    MoonlightColors.Red.copy(alpha = 0.1f)
                else
                    MoonlightColors.Green.copy(alpha = 0.1f)
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (isError) Icons.Default.Error else Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (isError) MoonlightColors.Red else MoonlightColors.Green,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    msg,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(32.dp))

    // Start Immich Button
    FilledTonalButton(
        onClick = onStartImmich,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MoonlightColors.Cyan.copy(alpha = 0.2f),
            contentColor = MoonlightColors.Cyan
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Icon(
            Icons.Default.CloudSync,
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            "ARRANCAR SERVIDOR IMMICH",
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Security info
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                tint = MoonlightColors.Cyan,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                "Conexión segura vía Tailscale. Credenciales cifradas con Android Keystore.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ConfigSection(
    state: PowerControlState,
    onSave: (url: String, user: String, pass: String, deviceId: String) -> Unit,
    onClear: () -> Unit,
    onTestConnection: (url: String, user: String, pass: String) -> Unit
) {
    var url by remember(state.serverUrl) { mutableStateOf(state.serverUrl) }
    var user by remember(state.username) { mutableStateOf(state.username) }
    var pass by remember(state.password) { mutableStateOf(state.password) }
    var devId by remember(state.deviceId) { mutableStateOf(state.deviceId) }
    var showPassword by remember { mutableStateOf(false) }

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        "CONFIGURACIÓN UPSNAP",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Black,
        letterSpacing = 2.sp,
        color = MoonlightColors.Purple
    )

    Spacer(modifier = Modifier.height(24.dp))

    // Help box
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MoonlightColors.Purple.copy(alpha = 0.05f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                "GUÍA RÁPIDA:",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MoonlightColors.Purple
            )
            Text(
                "1. Pon la IP de tu servidor UpSnap.\n" +
                "2. Pon tu usuario y contraseña.\n" +
                "3. Pulsa 'BUSCAR MIS DISPOSITIVOS'.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    // Server URL
    OutlinedTextField(
        value = url,
        onValueChange = { url = it },
        label = { Text("URL del servidor") },
        placeholder = { Text("Ej: 100.69.149.17:8090") },
        leadingIcon = { Icon(Icons.Default.Dns, contentDescription = null) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
    )

    Spacer(modifier = Modifier.height(12.dp))

    // Username
    OutlinedTextField(
        value = user,
        onValueChange = { user = it },
        label = { Text("Usuario") },
        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        singleLine = true
    )

    Spacer(modifier = Modifier.height(12.dp))

    // Password
    OutlinedTextField(
        value = pass,
        onValueChange = { pass = it },
        label = { Text("Contraseña") },
        leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
        trailingIcon = {
            IconButton(onClick = { showPassword = !showPassword }) {
                Icon(
                    if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = "Toggle password"
                )
            }
        },
        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
    )

    Spacer(modifier = Modifier.height(24.dp))

    // Test connection / Fetch devices button
    Button(
        onClick = { onTestConnection(url.trim(), user.trim(), pass) },
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MoonlightColors.Purple
        ),
        enabled = !state.isTestingConnection && url.isNotBlank() && user.isNotBlank() && pass.isNotBlank()
    ) {
        if (state.isTestingConnection) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
        } else {
            Icon(Icons.Default.Search, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("BUSCAR MIS DISPOSITIVOS", fontWeight = FontWeight.Black)
        }
    }

    // Device selector (if devices found)
    AnimatedVisibility(
        visible = state.availableDevices.isNotEmpty(),
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Column(modifier = Modifier.padding(top = 24.dp)) {
            Text(
                "SELECCIONA TU PC:",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MoonlightColors.Green
            )
            Spacer(modifier = Modifier.height(12.dp))
            state.availableDevices.forEach { (id, name) ->
                val isSelected = devId == id
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { 
                            devId = id
                            state.deviceId = id
                            state.deviceName = name
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) 
                            MoonlightColors.Green.copy(alpha = 0.15f) 
                        else 
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (isSelected) MoonlightColors.Green else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(name, fontWeight = FontWeight.Bold)
                            Text("ID: $id", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(32.dp))

    // Final Save button
    Button(
        onClick = { onSave(url.trim(), user.trim(), pass, devId.trim()) },
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MoonlightColors.Green
        ),
        enabled = url.isNotBlank() && user.isNotBlank() && pass.isNotBlank() && devId.isNotBlank()
    ) {
        Icon(Icons.Default.Check, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("GUARDAR Y FINALIZAR", fontWeight = FontWeight.Black)
    }

    if (state.isConfigured) {
        Spacer(modifier = Modifier.height(16.dp))

        // Clear configuration
        TextButton(
            onClick = onClear,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.textButtonColors(
                contentColor = MoonlightColors.Red
            )
        ) {
            Icon(Icons.Default.DeleteForever, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Borrar toda la configuración")
        }
    }

    Spacer(modifier = Modifier.height(32.dp))
}

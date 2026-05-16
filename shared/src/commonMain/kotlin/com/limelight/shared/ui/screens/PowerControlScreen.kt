package com.limelight.shared.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.limelight.shared.ui.components.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limelight.shared.network.UpSnapUrlValidator
import com.limelight.shared.ui.theme.MoonlightColors

/**
 * State holder for the Power Control screen.
 */
@Stable
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
    
    // List of devices found on the server
    val availableDevices = mutableStateListOf<Pair<String, String>>()
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
    onTestConnection: (url: String, user: String, pass: String) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(MoonlightColors.Background)) {
        // Background Glows
        AetherisGlow(
            modifier = Modifier.align(Alignment.BottomEnd).offset(x = 100.dp, y = 100.dp),
            color = MoonlightColors.Secondary
        )

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "POWER HUB",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MoonlightColors.OnSurface)
                        }
                    },
                    actions = {
                        IconButton(onClick = { state.showConfig = !state.showConfig }) {
                            Icon(
                                if (state.showConfig) Icons.Default.Close else Icons.Default.Settings,
                                contentDescription = "Configuración",
                                tint = MoonlightColors.OnSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MoonlightColors.OnSurface,
                    )
                )
            },
            containerColor = Color.Transparent
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
                    WakeSection(state, onWake)
                }
            }
        }
    }
}

@Composable
private fun WakeSection(
    state: PowerControlState,
    onWake: () -> Unit
) {
    Spacer(modifier = Modifier.height(24.dp))

    if (!state.isConfigured) {
        GlassCard(modifier = Modifier.padding(top = 24.dp)) {
            Column(
                modifier = Modifier.padding(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.SettingsSuggest,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MoonlightColors.Outline.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "PROVISIONING REQUIRED",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MoonlightColors.OnSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Please configure your UpSnap credentials to enable remote power orchestration.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MoonlightColors.OnSurfaceVariant,
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
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val buttonColor = if (state.isWaking) MoonlightColors.Secondary else MoonlightColors.Primary

    Text(
        state.deviceName.uppercase(),
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = MoonlightColors.OnSurface,
        letterSpacing = 1.sp
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        "UPSNAP REMOTE ORCHESTRATOR",
        style = MaterialTheme.typography.labelSmall,
        color = MoonlightColors.Primary,
        fontWeight = FontWeight.Bold
    )

    Spacer(modifier = Modifier.height(64.dp))

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(240.dp)
            .scale(if (!state.isWaking) pulseScale else 1f)
    ) {
        // Outer Glow
        Box(
            modifier = Modifier
                .size(220.dp)
                .blur(40.dp)
                .background(buttonColor.copy(alpha = 0.15f), CircleShape)
        )
        
        Box(
            modifier = Modifier
                .size(180.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.05f))
                .border(2.dp, buttonColor.copy(alpha = 0.3f), CircleShape)
                .clickable { if (!state.isWaking) onWake() },
            contentAlignment = Alignment.Center
        ) {
            if (state.isWaking) {
                CircularProgressIndicator(
                    modifier = Modifier.size(64.dp),
                    color = buttonColor,
                    strokeWidth = 4.dp
                )
            } else {
                Icon(
                    Icons.Default.PowerSettingsNew,
                    contentDescription = "Encender PC",
                    modifier = Modifier.size(80.dp),
                    tint = buttonColor
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(64.dp))

    Text(
        if (state.isWaking) "EMITTING WOL PACKET..." else "READY TO BOOT SYSTEM",
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Bold,
        color = if (state.isWaking) MoonlightColors.Secondary else MoonlightColors.OnSurfaceVariant
    )

    state.statusMessage?.let { msg ->
        Spacer(modifier = Modifier.height(32.dp))
        val isError = msg.startsWith("Error") || msg.startsWith("No se") || msg.contains("incorrecta")
        GlassCard {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (isError) Icons.Default.Error else Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (isError) MoonlightColors.Error else MoonlightColors.Secondary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    msg,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MoonlightColors.OnSurface
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(48.dp))

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Icon(
            Icons.Default.Shield,
            contentDescription = null,
            tint = MoonlightColors.Secondary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            "SECURED VIA TAILSCALE MESH NETWORK",
            style = MaterialTheme.typography.labelSmall,
            color = MoonlightColors.OnSurfaceVariant.copy(alpha = 0.6f)
        )
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
    val isHttpsUrlValid = UpSnapUrlValidator.isValidServerUrl(url)

    Spacer(modifier = Modifier.height(16.dp))

    GlassCard {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                "UPSNAP AUTHENTICATION",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                color = MoonlightColors.Primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("Server URL") },
                placeholder = { Text("https://100.x.y.z:8090") },
                leadingIcon = { Icon(Icons.Default.Dns, contentDescription = null, tint = MoonlightColors.Primary) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                isError = url.isNotBlank() && !isHttpsUrlValid
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = user,
                onValueChange = { user = it },
                label = { Text("Username") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = MoonlightColors.Primary) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = pass,
                onValueChange = { pass = it },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = MoonlightColors.Primary) },
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
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { onTestConnection(url.trim(), user.trim(), pass) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MoonlightColors.Primary),
                enabled = !state.isTestingConnection && isHttpsUrlValid && user.isNotBlank() && pass.isNotBlank()
            ) {
                if (state.isTestingConnection) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MoonlightColors.OnPrimaryContainer, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Search, contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("SCAN FOR DEVICES", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    AnimatedVisibility(
        visible = state.availableDevices.isNotEmpty(),
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Column(modifier = Modifier.padding(top = 24.dp)) {
            Text(
                "SELECT TARGET INFRASTRUCTURE:",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MoonlightColors.Secondary
            )
            Spacer(modifier = Modifier.height(12.dp))
            state.availableDevices.forEach { (id, name) ->
                val isSelected = devId == id
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable { 
                            devId = id
                            state.deviceId = id
                            state.deviceName = name
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MoonlightColors.Secondary.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.03f)
                    ),
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, MoonlightColors.Secondary.copy(alpha = 0.3f)) else null
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (isSelected) MoonlightColors.Secondary else MoonlightColors.OnSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(name, fontWeight = FontWeight.Bold, color = MoonlightColors.OnSurface)
                            Text("NODE ID: $id", style = MaterialTheme.typography.labelSmall, color = MoonlightColors.OnSurfaceVariant)
                        }
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(32.dp))

    Button(
        onClick = { onSave(url.trim(), user.trim(), pass, devId.trim()) },
        modifier = Modifier.fillMaxWidth().height(60.dp),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MoonlightColors.Secondary),
        enabled = isHttpsUrlValid && user.isNotBlank() && pass.isNotBlank() && devId.isNotBlank()
    ) {
        Icon(Icons.Default.Check, contentDescription = null)
        Spacer(modifier = Modifier.width(12.dp))
        Text("COMMIT CONFIGURATION", fontWeight = FontWeight.Bold)
    }

    if (state.isConfigured) {
        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = onClear,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.textButtonColors(contentColor = MoonlightColors.Error)
        ) {
            Icon(Icons.Default.DeleteSweep, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("WIPE LOCAL DATA")
        }
    }

    Spacer(modifier = Modifier.height(100.dp))
}

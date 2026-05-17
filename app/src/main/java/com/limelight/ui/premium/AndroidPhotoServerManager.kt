package com.limelight.ui.premium

import android.app.Activity
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import com.limelight.shared.network.ImmichHealthChecker
import com.limelight.shared.network.immich.ImmichApiClient
import com.limelight.shared.network.RemoteScriptClient
import com.limelight.shared.platform.PhotoServerState
import com.limelight.shared.platform.PhotoServerStatus
import com.limelight.shared.platform.StartCommandResult
import com.limelight.shared.data.immich.ImmichGalleryState
import kotlinx.coroutines.runBlocking

import com.limelight.ui.premium.ImmichFlutterEngineWrapper

/**
 * Manages the Immich photo server lifecycle via the StreamDeck API.
 */
class AndroidPhotoServerManager(
    private val state: PhotoServerState,
    private val dispatchState: (() -> Unit) -> Unit = { update -> update() }
) {

    companion object {
        private const val IMMICH_PORT = 2283
        private const val SCRIPT_FOLDER = "07_Personalizacion"
        private const val START_SCRIPT = "fotos.bat"
        private const val STOP_SCRIPT = "cerrar_fotos.bat"
        private const val MAX_STARTUP_WAIT_SECONDS = 200 // Docker Engine can take up to 3 minutes
        private const val IMMICH_PACKAGE = "app.alextran.immich"
    }

    fun isFlutterAvailable(): Boolean {
        return try {
            Class.forName("io.flutter.embedding.android.FlutterActivity")
            true
        } catch (e: Exception) {
            false
        }
    }

    fun openImmichGallery(context: Context, left: Int = 0, top: Int = 0, width: Int = 0, height: Int = 0) {
        runCatching {
            // Launch the integrated Flutter engine activity
            val intent = ImmichFlutterEngineWrapper.getLaunchIntent(context).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }.onFailure { e ->
            android.util.Log.e("ImmichManager", "Fallo al abrir galería integrada: ${e.message}", e)
            Toast.makeText(context, "Error al abrir la galería integrada: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun start(remoteClient: RemoteScriptClient, pcIp: String): StartCommandResult {
        updateState {
            state.updateStatus(PhotoServerStatus.Starting)
            state.healthMessage = "Iniciando proceso..."
        }
        addLog("→ Solicitando ejecución de fotos.bat...")

        return try {
            // === Envío ÚNICO del script ===
            // El StreamDeck devolverá 200 OK casi al instante, pero el script
            // seguirá ejecutándose en el PC abriendo Docker y levantando contenedores.
            val scriptResult = remoteClient.runScript(SCRIPT_FOLDER, START_SCRIPT)
            if (!scriptResult) {
                val msg = "No se pudo conectar al StreamDeck. ¿Está el PC encendido?"
                updateState {
                    state.updateStatus(PhotoServerStatus.Error(msg))
                }
                addLog("✗ $msg")
                StartCommandResult.Failed(msg)
            } else {
                waitForImmich(pcIp)
            }
        } catch (e: Exception) {
            val msg = "Error: ${e.message ?: "desconocido"}"
            updateState {
                state.updateStatus(PhotoServerStatus.Error(msg))
            }
            addLog("✗ $msg")
            StartCommandResult.Failed(msg)
        }
    }

    fun stop(remoteClient: RemoteScriptClient) {
        addLog("→ Apagando contenedores...")
        updateState {
            state.healthMessage = "Apagando Immich..."
        }
        try {
            if (remoteClient.runScript(SCRIPT_FOLDER, STOP_SCRIPT)) {
                updateState {
                    state.updateStatus(PhotoServerStatus.Stopped)
                    state.healthMessage = "Immich apagado"
                }
                addLog("✓ Immich apagado")
            } else {
                updateState {
                    state.healthMessage = "Fallo al apagar"
                }
                addLog("✗ Error conectando al PC")
            }
        } catch (e: Exception) {
            addLog("✗ Error: ${e.message ?: "desconocido"}")
        }
    }

    fun restart(remoteClient: RemoteScriptClient, pcIp: String): StartCommandResult {
        stop(remoteClient)
        Thread.sleep(3000)
        return start(remoteClient, pcIp)
    }

    fun checkHealth(pcIp: String) {
        val url = "http://$pcIp:$IMMICH_PORT"
        state.updateConnection(baseUrl = url, apiKey = state.connectionConfig.apiKey)
        val result = runBlocking { ImmichHealthChecker.check(state.connectionConfig) }
        updateState {
            state.healthMessage = result.message
            if (result.isHealthy) {
                state.updateStatus(PhotoServerStatus.Running(IMMICH_PORT, url))
                appendLog("✓ Immich activo: $url")
                refreshImmich()
            } else {
                if (state.status is PhotoServerStatus.Running) {
                    state.updateStatus(PhotoServerStatus.Stopped)
                }
                appendLog("ℹ ${result.message}")
            }
        }
    }

    private fun waitForImmich(pcIp: String): StartCommandResult {
        addLog("✓ Comando aceptado por el PC.")
        updateState {
            state.healthMessage = "Esperando a que Docker y contenedores arranquen..."
        }

        // === Polling de salud de Immich ===
        // Simplemente esperamos hasta que el puerto responda. El script del PC
        // ya se encarga de esperar a Docker y hacer el docker compose up.
        val url = "http://$pcIp:$IMMICH_PORT"
        updateState { state.updateConnection(baseUrl = url, apiKey = state.connectionConfig.apiKey) }
        val health = runBlocking {
            ImmichHealthChecker.waitForReady(
                config = state.connectionConfig,
                maxWaitSeconds = MAX_STARTUP_WAIT_SECONDS,
                pollIntervalMs = 5000,
                onProgress = { msg ->
                    updateState { state.healthMessage = msg }
                }
            )
        }
        updateState {
            state.updateStatus(PhotoServerStatus.Running(IMMICH_PORT, url))
            state.healthMessage = if (health.isHealthy) health.message else "⚠ Immich tardando demasiado"
        }
        if (health.isHealthy) {
            addLog("✓ ¡Immich en línea! → $url")
            refreshImmich()
        } else {
            addLog("⚠ No responde aún tras varios minutos.")
            addLog("ℹ Si Docker Desktop se ha quedado colgado en el PC, reinícialo.")
        }
        return StartCommandResult.Success
    }

    fun refreshImmich() {
        android.util.Log.d("ImmichManager", "Refrescando galería. Config: URL=${state.connectionConfig.baseUrl}, KeyLength=${state.connectionConfig.apiKey.length}")
        updateState { state.updateGallery(ImmichGalleryState.Loading) }
        
        // Spawn a background thread to make the network request so it NEVER blocks the main thread
        kotlin.concurrent.thread {
            runCatching {
                kotlinx.coroutines.runBlocking {
                    ImmichApiClient().loadOverview(state.connectionConfig)
                }
            }
            .onSuccess { overview ->
                android.util.Log.d("ImmichManager", "Carga exitosa: ${overview.photos.size} fotos")
                updateState {
                    state.updateGallery(ImmichGalleryState.Success(overview.summary, overview.photos))
                    state.recentLogs = overview.photos.take(3).map { "${it.name} sincronizada desde Immich" }
                }
            }
            .onFailure { error ->
                android.util.Log.e("ImmichManager", "Error cargando Immich: ${error.message}", error)
                updateState { state.updateGallery(ImmichGalleryState.Error(error.message ?: "No se pudo cargar Immich")) }
            }
        }
    }

    private fun addLog(message: String) {
        updateState { appendLog(message) }
    }

    private fun appendLog(message: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
            .format(java.util.Date())
        val logLine = "[$timestamp] $message"
        state.recentLogs = (state.recentLogs + logLine).takeLast(10)
    }

    private fun updateState(update: () -> Unit) {
        dispatchState(update)
    }
}

package com.limelight.ui.premium

import com.limelight.shared.network.ImmichHealthChecker
import com.limelight.shared.network.RemoteScriptClient
import com.limelight.shared.platform.PhotoServerState
import com.limelight.shared.platform.PhotoServerStatus
import com.limelight.shared.platform.StartCommandResult

/**
 * Manages the Immich photo server lifecycle via the StreamDeck API.
 */
class AndroidPhotoServerManager(private val state: PhotoServerState) {

    companion object {
        private const val IMMICH_PORT = 2283
        private const val SCRIPT_FOLDER = "07_Personalizacion"
        private const val START_SCRIPT = "fotos.bat"
        private const val STOP_SCRIPT = "cerrar_fotos.bat"
        private const val MAX_STARTUP_WAIT_SECONDS = 200 // Docker Engine can take up to 3 minutes
    }

    fun start(remoteClient: RemoteScriptClient, pcIp: String): StartCommandResult {
        state.updateStatus(PhotoServerStatus.Starting)
        state.healthMessage = "Iniciando proceso..."
        addLog("→ Solicitando ejecución de fotos.bat...")

        return try {
            // === Envío ÚNICO del script ===
            // El StreamDeck devolverá 200 OK casi al instante, pero el script
            // seguirá ejecutándose en el PC abriendo Docker y levantando contenedores.
            val scriptResult = remoteClient.runScript(SCRIPT_FOLDER, START_SCRIPT)
            if (!scriptResult) {
                val msg = "No se pudo conectar al StreamDeck. ¿Está el PC encendido?"
                state.updateStatus(PhotoServerStatus.Error(msg))
                addLog("✗ $msg")
                return StartCommandResult.Failed(msg)
            }
            addLog("✓ Comando aceptado por el PC.")
            state.healthMessage = "Esperando a que Docker y contenedores arranquen..."

            // === Polling de salud de Immich ===
            // Simplemente esperamos hasta que el puerto responda. El script del PC
            // ya se encarga de esperar a Docker y hacer el docker compose up.
            val health = ImmichHealthChecker.waitForReady(
                serverIp = pcIp,
                port = IMMICH_PORT,
                maxWaitSeconds = MAX_STARTUP_WAIT_SECONDS,
                pollIntervalMs = 5000,
                onProgress = { msg ->
                    state.healthMessage = msg
                }
            )

            if (health.isHealthy) {
                val url = "http://$pcIp:$IMMICH_PORT"
                state.updateStatus(PhotoServerStatus.Running(IMMICH_PORT, url))
                state.healthMessage = health.message
                addLog("✓ ¡Immich en línea! → $url")
                StartCommandResult.Success
            } else {
                val url = "http://$pcIp:$IMMICH_PORT"
                state.updateStatus(PhotoServerStatus.Running(IMMICH_PORT, url))
                state.healthMessage = "⚠ Immich tardando demasiado"
                addLog("⚠ No responde aún tras varios minutos.")
                addLog("ℹ Si Docker Desktop se ha quedado colgado en el PC, reinícialo.")
                StartCommandResult.Success
            }

        } catch (e: Exception) {
            val msg = "Error: ${e.message}"
            state.updateStatus(PhotoServerStatus.Error(msg))
            addLog("✗ $msg")
            StartCommandResult.Failed(msg)
        }
    }

    fun stop(remoteClient: RemoteScriptClient) {
        addLog("→ Apagando contenedores...")
        state.healthMessage = "Apagando Immich..."
        try {
            if (remoteClient.runScript(SCRIPT_FOLDER, STOP_SCRIPT)) {
                state.updateStatus(PhotoServerStatus.Stopped)
                state.healthMessage = "Immich apagado"
                addLog("✓ Immich apagado")
            } else {
                state.healthMessage = "Fallo al apagar"
                addLog("✗ Error conectando al PC")
            }
        } catch (e: Exception) {
            addLog("✗ Error: ${e.message}")
        }
    }

    fun restart(remoteClient: RemoteScriptClient, pcIp: String): StartCommandResult {
        stop(remoteClient)
        Thread.sleep(3000)
        return start(remoteClient, pcIp)
    }

    fun checkHealth(pcIp: String) {
        val result = ImmichHealthChecker.check(pcIp, IMMICH_PORT)
        state.healthMessage = result.message
        if (result.isHealthy) {
            val url = "http://$pcIp:$IMMICH_PORT"
            state.updateStatus(PhotoServerStatus.Running(IMMICH_PORT, url))
            addLog("✓ Immich activo: $url")
        } else {
            if (state.status is PhotoServerStatus.Running) {
                state.updateStatus(PhotoServerStatus.Stopped)
            }
            addLog("ℹ ${result.message}")
        }
    }

    private fun addLog(message: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
            .format(java.util.Date())
        val logLine = "[$timestamp] $message"
        state.recentLogs = (state.recentLogs + logLine).takeLast(10)
    }
}

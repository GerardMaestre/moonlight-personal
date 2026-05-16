package com.limelight.shared.network

import java.net.HttpURLConnection
import java.net.URL

/**
 * Health checker for the Immich photo server.
 * Pings the Immich API to verify the server is up and responding.
 */
object ImmichHealthChecker {

    data class HealthResult(
        val isHealthy: Boolean,
        val message: String,
        val responseTimeMs: Long = 0
    )

    /**
     * Check if the Immich server is responding at the given base URL.
     * Default port is 2283.
     */
    fun check(serverIp: String, port: Int = 2283, timeoutMs: Int = 5000): HealthResult {
        val start = System.currentTimeMillis()
        return try {
            val url = URL("http://$serverIp:$port/api/server/ping")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = timeoutMs
            conn.readTimeout = timeoutMs

            try {
                val code = conn.responseCode
                val elapsed = System.currentTimeMillis() - start

                if (code == 200) {
                    HealthResult(true, "Immich OK (${elapsed}ms)", elapsed)
                } else {
                    HealthResult(false, "Immich respondió con código $code", elapsed)
                }
            } finally {
                conn.disconnect()
            }
        } catch (e: java.net.SocketTimeoutException) {
            HealthResult(false, "Timeout - Immich no responde")
        } catch (e: java.net.ConnectException) {
            HealthResult(false, "No se puede conectar a Immich")
        } catch (e: Exception) {
            HealthResult(false, "Error: ${e.message ?: "desconocido"}")
        }
    }

    /**
     * Poll the Immich server until it responds or timeout is reached.
     * Useful after starting Docker containers.
     */
    fun waitForReady(
        serverIp: String,
        port: Int = 2283,
        maxWaitSeconds: Int = 120,
        pollIntervalMs: Long = 3000,
        onProgress: (String) -> Unit = {}
    ): HealthResult {
        val deadline = System.currentTimeMillis() + (maxWaitSeconds * 1000L)
        var attempts = 0

        while (System.currentTimeMillis() < deadline) {
            attempts++
            onProgress("Verificando Immich... (intento $attempts)")

            val result = check(serverIp, port, 3000)
            if (result.isHealthy) {
                return result
            }

            try {
                Thread.sleep(pollIntervalMs)
            } catch (_: InterruptedException) {
                return HealthResult(false, "Verificación cancelada")
            }
        }

        return HealthResult(false, "Timeout esperando a Immich (${maxWaitSeconds}s)")
    }
}

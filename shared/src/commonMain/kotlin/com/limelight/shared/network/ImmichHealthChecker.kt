package com.limelight.shared.network

import com.limelight.shared.data.immich.ImmichConnectionConfig
import com.limelight.shared.network.immich.ImmichApiClient
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/**
 * Health checker for the Immich photo server.
 * Uses the real Immich REST API instead of local mock data.
 */
object ImmichHealthChecker {

    data class HealthResult(
        val isHealthy: Boolean,
        val message: String,
        val responseTimeMs: Long = 0,
    )

    /**
     * Check if the Immich server is responding at the given base URL.
     */
    suspend fun check(
        config: ImmichConnectionConfig,
        client: ImmichApiClient = ImmichApiClient(),
    ): HealthResult {
        val mark = TimeSource.Monotonic.markNow()
        return runCatching {
            client.ping(config)
            val elapsed = mark.elapsedNow().inWholeMilliseconds
            HealthResult(true, "Immich OK (${elapsed}ms)", elapsed)
        }.getOrElse { error ->
            val elapsed = mark.elapsedNow().inWholeMilliseconds
            HealthResult(false, error.message ?: "No se puede conectar a Immich", elapsed)
        }
    }

    /**
     * Poll the Immich server until it responds or timeout is reached.
     * Useful after starting Docker containers.
     */
    suspend fun waitForReady(
        config: ImmichConnectionConfig,
        maxWaitSeconds: Int = 120,
        pollIntervalMs: Long = 3000,
        client: ImmichApiClient = ImmichApiClient(),
        onProgress: (String) -> Unit = {},
    ): HealthResult {
        val waitMark = TimeSource.Monotonic.markNow()
        val maxWait = maxWaitSeconds.seconds
        var attempts = 0
        var result = HealthResult(false, "Sin comprobaciones")

        while (waitMark.elapsedNow() < maxWait && !result.isHealthy) {
            attempts += 1
            onProgress("Verificando Immich... (intento $attempts)")
            result = check(config, client)
            if (!result.isHealthy) {
                delay(pollIntervalMs)
            }
        }

        return when {
            result.isHealthy -> result
            else -> HealthResult(false, "Timeout esperando a Immich (${maxWaitSeconds}s)")
        }
    }
}

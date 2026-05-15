package com.limelight.shared.network

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class WakeServiceTest {
    @Test
    fun returnsFailureWhenNoUpsnapAndNoMac() {
        val result = WakeService.wakeWithFallback(
            upSnapClient = null,
            deviceId = null,
            macAddress = null,
        )
        assertTrue(result is WakeService.WakeOutcome.Failure)
    }

    @Test
    fun usesUdpPathWhenMacProvidedAndNoUpsnap() {
        var called = false
        val result = WakeService.wakeWithFallback(
            upSnapClient = null,
            deviceId = null,
            macAddress = "11:22:33:44:55:66",
            udpWake = { _, _, _ ->
                called = true
                Result.success(Unit)
            },
        )

        assertTrue(called)
        assertTrue(result is WakeService.WakeOutcome.Success)
    }

    @Test
    fun returnsFailureWhenUdpFallbackFails() {
        val result = WakeService.wakeWithFallback(
            upSnapClient = null,
            deviceId = null,
            macAddress = "11:22:33:44:55:66",
            udpWake = { _, _, _ -> Result.failure(IllegalStateException("boom")) },
        )

        assertTrue(result is WakeService.WakeOutcome.Failure)
        result as WakeService.WakeOutcome.Failure
        assertEquals("No se pudo enviar paquete WoL UDP", result.reason)
    }
}

package com.limelight.shared.network

import kotlin.test.Test
import kotlin.test.assertTrue

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
}

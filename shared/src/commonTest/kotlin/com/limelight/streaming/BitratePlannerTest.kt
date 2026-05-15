package com.limelight.streaming

import kotlin.test.Test
import kotlin.test.assertEquals

class BitratePlannerTest {
    @Test
    fun wifiProfileDefaultsTo1080p() {
        val profile = BitratePlanner.suggestProfile(isWifi = true)
        assertEquals(1920, profile.width)
        assertEquals(1080, profile.height)
    }
}

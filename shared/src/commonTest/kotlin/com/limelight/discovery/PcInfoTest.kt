package com.limelight.discovery

import kotlin.test.Test
import kotlin.test.assertEquals

class PcInfoTest {
    @Test
    fun storesComputerName() {
        val pc = PcInfo(name = "Gaming PC", localAddress = "192.168.1.100")
        assertEquals("Gaming PC", pc.name)
    }
}

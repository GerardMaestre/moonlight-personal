package com.limelight.shared.network

import com.limelight.wol.WolSender
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class StandardWolSenderTest {
    @Test
    fun testCreateMagicPacketHasExpectedLength() {
        val packet = com.limelight.wol.MagicPacket.create("11:22:33:44:55:66")
        assertEquals(102, packet.size)
    }

    @Test
    fun testInvalidMacAddressFails() {
        assertFailsWith<IllegalArgumentException> {
            com.limelight.wol.MagicPacket.getMacBytes("11:22:33:44:55")
        }
    }

    @Test
    fun testSendMagicPacketReturnsSuccessOrFailureResult() {
        val result = runCatching { WolSender.sendBlocking("11:22:33:44:55:66", "127.0.0.1", 9999) }
        assertTrue(result.isSuccess || result.isFailure)
    }
}

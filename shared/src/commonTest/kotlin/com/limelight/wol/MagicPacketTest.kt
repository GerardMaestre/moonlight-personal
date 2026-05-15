package com.limelight.wol

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MagicPacketTest {
    @Test
    fun magicPacketHasValidLength() {
        val packet = MagicPacket.create("FF:FF:FF:FF:FF:FF")
        assertEquals(102, packet.size)
    }

    @Test
    fun magicPacketStartsWithSixFFBytes() {
        val packet = MagicPacket.create("11:22:33:44:55:66")
        assertTrue(packet.take(6).all { it == 0xFF.toByte() })
    }
}

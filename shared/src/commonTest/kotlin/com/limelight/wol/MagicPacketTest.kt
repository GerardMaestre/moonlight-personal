package com.limelight.wol

import kotlin.test.Test
import kotlin.test.assertEquals

class MagicPacketTest {
    @Test
    fun magicPacketHasValidLength() {
        val packet = MagicPacket.create("FF:FF:FF:FF:FF:FF")
        assertEquals(102, packet.size)
    }
}

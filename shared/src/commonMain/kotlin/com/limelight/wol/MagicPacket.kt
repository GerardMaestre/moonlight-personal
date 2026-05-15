package com.limelight.wol

object MagicPacket {
    fun create(mac: String): ByteArray {
        val cleanMac = mac.replace("-", ":").split(":")
        require(cleanMac.size == 6) { "Invalid MAC format" }
        val macBytes = cleanMac.map { it.toInt(16).toByte() }.toByteArray()
        val packet = ByteArray(6 + 16 * 6)
        repeat(6) { packet[it] = 0xFF.toByte() }
        for (i in 0 until 16) {
            macBytes.copyInto(packet, destinationOffset = 6 + i * 6)
        }
        return packet
    }
}

package com.limelight.shared.network

import com.limelight.wol.MagicPacket
import com.limelight.wol.WolSender
import kotlinx.coroutines.runBlocking

object StandardWolSender {
    @JvmStatic
    fun sendMagicPacket(macAddress: String, broadcastIp: String = "255.255.255.255", port: Int = 9): Result<Unit> {
        return runCatching {
            runBlocking {
                WolSender.send(macAddress, broadcastIp, port)
            }
        }
    }

    @JvmStatic
    fun sendMagicPacketCompat(macAddress: String, broadcastIp: String = "255.255.255.255", port: Int = 9): Boolean {
        return sendMagicPacket(macAddress, broadcastIp, port).isSuccess
    }

    fun createMagicPacket(macAddress: String): ByteArray {
        return MagicPacket.create(macAddress)
    }

    fun getMacBytes(macStr: String): ByteArray {
        val hexStrings = macStr.split(":", "-")
        require(hexStrings.size == 6) { "Invalid MAC address format: $macStr" }
        return hexStrings.map { it.toInt(16).toByte() }.toByteArray()
    }
}

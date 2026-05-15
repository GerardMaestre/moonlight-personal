package com.limelight.shared.network

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import com.limelight.wol.MagicPacket

/**
 * Platform-agnostic standard Wake-on-LAN Magic Packet sender.
 * Note: Uses java.net which currently restricts it to JVM targets (Android/Desktop).
 */
object StandardWolSender {
    @JvmStatic
    fun sendMagicPacket(macAddress: String, broadcastIp: String = "255.255.255.255", port: Int = 9): Result<Unit> {
        return runCatching {
            require(port in 1..65535) { "Invalid port: $port" }
            val bytes = createMagicPacket(macAddress)
            val address = InetAddress.getByName(broadcastIp)
            val packet = DatagramPacket(bytes, bytes.size, address, port)

            DatagramSocket().use { socket ->
                socket.broadcast = true
                socket.send(packet)
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

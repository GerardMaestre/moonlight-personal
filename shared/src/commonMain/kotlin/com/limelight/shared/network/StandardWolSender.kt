package com.limelight.shared.network

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

/**
 * Platform-agnostic standard Wake-on-LAN Magic Packet sender.
 * Note: Uses java.net which currently restricts it to JVM targets (Android/Desktop).
 */
object StandardWolSender {
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

    fun createMagicPacket(macAddress: String): ByteArray {
        val macBytes = getMacBytes(macAddress)
        val bytes = ByteArray(6 + 16 * macBytes.size)
        repeat(6) { bytes[it] = 0xFF.toByte() }
        for (i in 0 until 16) {
            System.arraycopy(macBytes, 0, bytes, 6 + i * macBytes.size, macBytes.size)
        }
        return bytes
    }

    fun getMacBytes(macStr: String): ByteArray {
        val hexStrings = macStr.split(":", "-")
        require(hexStrings.size == 6) { "Invalid MAC address format: $macStr" }
        return hexStrings.map { it.toInt(16).toByte() }.toByteArray()
    }
}

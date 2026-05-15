package com.limelight.shared.network

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

/**
 * Platform-agnostic standard Wake-on-LAN Magic Packet sender.
 * Note: Uses java.net which currently restricts it to JVM targets (Android/Desktop).
 */
object StandardWolSender {
    
    fun sendMagicPacket(macAddress: String, broadcastIp: String = "255.255.255.255", port: Int = 9) {
        try {
            val macBytes = getMacBytes(macAddress)
            val bytes = ByteArray(6 + 16 * macBytes.size)
            
            // First 6 bytes are 0xFF
            for (i in 0 until 6) {
                bytes[i] = 0xff.toByte()
            }
            
            // Followed by 16 repetitions of the MAC address
            for (i in 1 until 17) {
                System.arraycopy(macBytes, 0, bytes, i * macBytes.size, macBytes.size)
            }
            
            val address = InetAddress.getByName(broadcastIp)
            val packet = DatagramPacket(bytes, bytes.size, address, port)
            
            DatagramSocket().use { socket ->
                socket.send(packet)
            }
        } catch (e: Exception) {
            println("[StandardWolSender] Failed to send WOL packet: ${e.message}")
        }
    }

    private fun getMacBytes(macStr: String): ByteArray {
        val hexStrings = macStr.split(":", "-")
        require(hexStrings.size == 6) { "Invalid MAC address format: $macStr" }
        return hexStrings.map { it.toInt(16).toByte() }.toByteArray()
    }
}

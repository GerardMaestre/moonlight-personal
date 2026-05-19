package com.limelight.wol

import com.limelight.wol.MagicPacket
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.*
import io.ktor.utils.io.core.ByteReadPacket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlin.io.use

object WolSender {
    suspend fun send(macAddress: String, broadcastAddress: String = "255.255.255.255", port: Int = 9) {
        require(port in 1..65535) { "Invalid port: $port" }

        val packetBytes = MagicPacket.create(macAddress)
        val address = InetSocketAddress(broadcastAddress, port)

        SelectorManager(Dispatchers.Default).use { selector ->
            aSocket(selector).udp().bind().use { socket ->
                socket.send(Datagram(ByteReadPacket(packetBytes), address))
            }
        }
    }

    @JvmStatic
    fun sendBlocking(macAddress: String, broadcastAddress: String = "255.255.255.255", port: Int = 9) {
        runBlocking { send(macAddress, broadcastAddress, port) }
    }
}

package com.limelight.wol

import com.limelight.shared.network.StandardWolSender

class StandardWolSenderAdapter : WolSender {
    override suspend fun send(mac: String, broadcastAddress: String, port: Int) {
        StandardWolSender.sendMagicPacket(mac, broadcastAddress, port).getOrThrow()
    }
}

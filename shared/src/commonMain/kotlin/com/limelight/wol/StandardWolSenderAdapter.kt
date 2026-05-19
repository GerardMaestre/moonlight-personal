package com.limelight.wol

class StandardWolSenderAdapter {
    suspend fun send(mac: String, broadcastAddress: String, port: Int) {
        WolSender.send(mac, broadcastAddress, port)
    }
}

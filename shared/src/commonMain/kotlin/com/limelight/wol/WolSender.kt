package com.limelight.wol

interface WolSender {
    suspend fun send(mac: String, broadcastAddress: String, port: Int = 9)
}

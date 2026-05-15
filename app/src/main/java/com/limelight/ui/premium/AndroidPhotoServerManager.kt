package com.limelight.ui.premium

import com.limelight.shared.platform.PhotoServerState
import com.limelight.shared.platform.PhotoServerStatus
import com.limelight.utils.LanAddressResolver
import fi.iki.elonen.NanoHTTPD

class AndroidPhotoServerManager(private val state: PhotoServerState) {
    private var server: NanoHTTPD? = null

    @Synchronized
    fun start() {
        if (server != null) return
        state.updateStatus(PhotoServerStatus.Starting)
        runCatching {
            val http = object : NanoHTTPD(0) {
                override fun serve(session: IHTTPSession): Response {
                    return newFixedLengthResponse("Moonlight Photo Server OK")
                }
            }
            http.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
            server = http
            val port = http.listeningPort
            val lanIp = LanAddressResolver.getActiveLanIpv4Address()
            val url = if (lanIp != null) {
                "http://$lanIp:$port/"
            } else {
                "http://127.0.0.1:$port/ (sin IP LAN activa; fallback local)"
            }
            state.updateStatus(PhotoServerStatus.Running(port, url))
        }.onFailure {
            state.updateStatus(PhotoServerStatus.Error(it.message ?: "No se pudo iniciar el servidor"))
        }
    }

    @Synchronized
    fun stop() {
        runCatching { server?.stop() }
        server = null
        state.updateStatus(PhotoServerStatus.Stopped)
    }

    fun restart() {
        stop()
        start()
    }
}

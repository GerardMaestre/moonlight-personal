package com.limelight.ui.premium

import com.limelight.shared.platform.PhotoServerState
import com.limelight.shared.platform.PhotoServerStatus
import com.limelight.shared.platform.StartCommandResult
import com.limelight.utils.LanAddressResolver
import fi.iki.elonen.NanoHTTPD

class AndroidPhotoServerManager(private val state: PhotoServerState) {
    private var server: NanoHTTPD? = null

    @Synchronized
    fun start(): StartCommandResult {
        if (server != null) return StartCommandResult.Success
        state.updateStatus(PhotoServerStatus.Starting)
        return runCatching {
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
            StartCommandResult.Success
        }.getOrElse {
            val msg = it.message ?: "No se pudo iniciar el servidor"
            state.updateStatus(PhotoServerStatus.Error(msg))
            StartCommandResult.Failed(msg)
        }
    }

    @Synchronized
    fun stop() {
        runCatching { server?.stop() }
        server = null
        state.updateStatus(PhotoServerStatus.Stopped)
    }

    fun restart(): StartCommandResult {
        stop()
        return start()
    }
}

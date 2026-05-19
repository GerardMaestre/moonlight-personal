package com.limelight.shared.network

import javax.jmdns.JmDNS
import javax.jmdns.ServiceEvent
import javax.jmdns.ServiceListener
import java.net.InetAddress

actual class PlatformMdnsScanner actual constructor(
    private val onHostDiscovered: (DiscoveredHost) -> Unit
) {
    private var jmdns: JmDNS? = null
    private var serviceListener: ServiceListener? = null

    actual fun startScanning() {
        Thread {
            try {
                val localhost = InetAddress.getLocalHost()
                if (localhost.isLoopbackAddress) return@Thread
                
                jmdns = JmDNS.create(localhost).apply {
                    serviceListener = object : ServiceListener {
                        override fun serviceAdded(event: ServiceEvent?) {}
                        override fun serviceRemoved(event: ServiceEvent?) {}
                        override fun serviceResolved(event: ServiceEvent?) {
                            val ev = event ?: return
                            val info = ev.info ?: return
                            val addresses = info.inetAddresses
                            val hostIp = addresses.firstOrNull()?.hostAddress ?: ""
                            if (hostIp.isNotEmpty()) {
                                onHostDiscovered(
                                    DiscoveredHost(
                                        id = hostIp,
                                        name = ev.name ?: "Moonlight Host",
                                        port = info.port,
                                        uuid = ""
                                    )
                                )
                            }
                        }
                    }
                    addServiceListener("_nvstream_gfe._tcp.local.", serviceListener)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    actual fun stopScanning() {
        try {
            serviceListener?.let { jmdns?.removeServiceListener("_nvstream_gfe._tcp.local.", it) }
            jmdns?.close()
        } catch (_: Exception) {}
        serviceListener = null
        jmdns = null
    }
}

package com.limelight.shared.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo

actual class PlatformMdnsScanner actual constructor(
    private val onHostDiscovered: (DiscoveredHost) -> Unit
) {
    private var nsdManager: NsdManager? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    
    companion object {
        var appContext: Context? = null
    }

    actual fun startScanning() {
        val context = appContext ?: return
        nsdManager = (context.getSystemService(Context.NSD_SERVICE) as NsdManager).apply {
            discoveryListener = object : NsdManager.DiscoveryListener {
                override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {}
                override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {}
                override fun onDiscoveryStarted(serviceType: String?) {}
                override fun onDiscoveryStopped(serviceType: String?) {}

                override fun onServiceFound(serviceInfo: NsdServiceInfo?) {
                    val info = serviceInfo ?: return
                    // Service type match for GeForce Experience/Sunshine hosts
                    if (info.serviceType.contains("_nvstream_gfe")) {
                        nsdManager?.resolveService(info, object : NsdManager.ResolveListener {
                            override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {}
                            override fun onServiceResolved(resolvedInfo: NsdServiceInfo?) {
                                val res = resolvedInfo ?: return
                                val hostIp = res.host?.hostAddress ?: ""
                                if (hostIp.isNotEmpty()) {
                                    onHostDiscovered(
                                        DiscoveredHost(
                                            id = hostIp,
                                            name = res.serviceName ?: "Moonlight Host",
                                            port = res.port,
                                            uuid = ""
                                        )
                                    )
                                }
                            }
                        })
                    }
                }

                override fun onServiceLost(serviceInfo: NsdServiceInfo?) {}
            }
            discoverServices("_nvstream_gfe._tcp.", NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        }
    }

    actual fun stopScanning() {
        try {
            discoveryListener?.let { nsdManager?.stopServiceDiscovery(it) }
        } catch (_: Exception) {}
            discoveryListener = null
            nsdManager = null
    }
}

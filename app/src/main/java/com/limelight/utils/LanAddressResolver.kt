package com.limelight.utils

import java.net.Inet4Address
import java.net.NetworkInterface

object LanAddressResolver {
    private val preferredInterfacePrefixes = listOf("wlan", "eth", "en")

    fun getActiveLanIpv4Address(): String? {
        return runCatching {
            val interfaces = NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
                .filter { iface ->
                    iface.isUp && !iface.isLoopback && !iface.isVirtual
                }

            val sortedInterfaces = interfaces.sortedByDescending { iface ->
                preferredInterfacePrefixes.any { prefix ->
                    iface.name.startsWith(prefix, ignoreCase = true)
                }
            }

            val preferredAddress = sortedInterfaces.asSequence()
                .flatMap { it.inetAddresses.toList().asSequence() }
                .filterIsInstance<Inet4Address>()
                .firstOrNull { addr ->
                    !addr.isLoopbackAddress && !addr.isLinkLocalAddress && addr.isSiteLocalAddress
                }

            preferredAddress?.hostAddress ?: sortedInterfaces.asSequence()
                .flatMap { it.inetAddresses.toList().asSequence() }
                .filterIsInstance<Inet4Address>()
                .firstOrNull { addr ->
                    !addr.isLoopbackAddress && !addr.isLinkLocalAddress
                }
                ?.hostAddress
        }.getOrNull()
    }
}

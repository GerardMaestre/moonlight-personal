package com.limelight.shared.network

/**
 * Unified wake service that can use UpSnap first and optionally fallback to UDP WoL.
 */
object WakeService {
    sealed class WakeOutcome {
        data object Success : WakeOutcome()
        data class Failure(val reason: String) : WakeOutcome()
    }

    fun wakeWithFallback(
        upSnapClient: UpSnapClient?,
        deviceId: String?,
        macAddress: String?,
        broadcastIp: String = "255.255.255.255",
        port: Int = 9,
    ): WakeOutcome {
        if (upSnapClient != null && !deviceId.isNullOrBlank()) {
            return when (val result = upSnapClient.wakeDevice(deviceId)) {
                is UpSnapClient.WakeResult.Success -> WakeOutcome.Success
                is UpSnapClient.WakeResult.Error -> {
                    if (!macAddress.isNullOrBlank()) {
                        val fallback = StandardWolSender.sendMagicPacket(macAddress, broadcastIp, port)
                        if (fallback.isSuccess) WakeOutcome.Success else WakeOutcome.Failure(result.message)
                    } else {
                        WakeOutcome.Failure(result.message)
                    }
                }
            }
        }

        if (!macAddress.isNullOrBlank()) {
            val udp = StandardWolSender.sendMagicPacket(macAddress, broadcastIp, port)
            return if (udp.isSuccess) WakeOutcome.Success else WakeOutcome.Failure("No se pudo enviar paquete WoL UDP")
        }

        return WakeOutcome.Failure("Configuración incompleta: falta deviceId o MAC")
    }
}

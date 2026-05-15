package com.limelight.connection

sealed interface ConnectionError {
    data object Timeout : ConnectionError
    data object AuthFailed : ConnectionError
    data object CodecUnsupported : ConnectionError
    data class Unknown(val reason: String) : ConnectionError
}

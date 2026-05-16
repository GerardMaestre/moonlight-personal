package com.limelight.shared.network

/**
 * Shared URL validator for UpSnap endpoints.
 */
object UpSnapUrlValidator {
    fun isValidServerUrl(rawValue: String, allowHttp: Boolean = true): Boolean {
        val value = rawValue.trim().trimEnd('/')
        if (value.isBlank()) return false

        val scheme = value.substringBefore("://", missingDelimiterValue = "")
        if (scheme.isBlank()) return false
        if (scheme != "https" && !(allowHttp && scheme == "http")) return false

        val hostPortPath = value.substringAfter("://", missingDelimiterValue = "")
        val host = hostPortPath.substringBefore('/').substringBefore(':')
        return host.isNotBlank()
    }
}

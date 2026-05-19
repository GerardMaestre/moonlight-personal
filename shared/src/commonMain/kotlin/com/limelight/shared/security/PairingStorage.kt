package com.limelight.shared.security

import com.limelight.platform.StorageManager

object PairingStorage {
    private const val SERVER_CERT_KEY_PREFIX = "immich.pairing.server_cert."
    private val storage = StorageManager()

    fun storeServerCertPem(computerId: String, pem: String) {
        storage.putString(SERVER_CERT_KEY_PREFIX + computerId, pem)
    }

    fun loadServerCertPem(computerId: String): String? {
        return storage.getString(SERVER_CERT_KEY_PREFIX + computerId)
    }

    fun removeServerCert(computerId: String) {
        storage.remove(SERVER_CERT_KEY_PREFIX + computerId)
    }
}

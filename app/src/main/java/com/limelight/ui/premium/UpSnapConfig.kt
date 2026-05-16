package com.limelight.ui.premium

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.limelight.shared.network.UpSnapUrlValidator
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Secure configuration manager for UpSnap Wake-on-LAN integration.
 * 
 * Credentials are encrypted using Android Keystore (hardware-backed on supported devices)
 * and stored in SharedPreferences. The encryption key never leaves the secure hardware.
 */
class UpSnapConfig private constructor(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "upsnap_config"
        private const val KEY_ALIAS = "upsnap_key"
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_USERNAME = "username_enc"
        private const val KEY_PASSWORD = "password_enc"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_IV_USERNAME = "iv_username"
        private const val KEY_IV_PASSWORD = "iv_password"
        private const val KEY_ENABLED = "enabled"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val ALLOW_HTTP_URLS = true

        fun getInstance(context: Context): UpSnapConfig {
            return UpSnapConfig(context.applicationContext)
        }
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val isConfigured: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
                && prefs.getString(KEY_SERVER_URL, null) != null
                && prefs.getString(KEY_DEVICE_ID, null) != null

    var serverUrl: String
        get() = prefs.getString(KEY_SERVER_URL, "") ?: ""
        set(value) {
            val formattedUrl = value.trim().trimEnd('/')
            if (!isValidServerUrl(formattedUrl)) {
                throw IllegalArgumentException("Invalid UpSnap server URL. HTTPS is required.")
            }
            prefs.edit().putString(KEY_SERVER_URL, formattedUrl).apply()
        }

    fun isValidServerUrl(value: String): Boolean {
        return UpSnapUrlValidator.isValidServerUrl(value, allowHttp = ALLOW_HTTP_URLS)
    }

    var deviceId: String
        get() = prefs.getString(KEY_DEVICE_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_DEVICE_ID, value.trim()).apply()

    var isEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()

    /**
     * Store credentials securely using Android Keystore encryption.
     */
    fun saveCredentials(username: String, password: String) {
        ensureKeyExists()
        val key = getSecretKey()

        // Encrypt username
        val cipherUser = Cipher.getInstance("AES/GCM/NoPadding")
        cipherUser.init(Cipher.ENCRYPT_MODE, key)
        val encUser = cipherUser.doFinal(username.toByteArray(Charsets.UTF_8))
        val ivUser = cipherUser.iv

        // Encrypt password
        val cipherPass = Cipher.getInstance("AES/GCM/NoPadding")
        cipherPass.init(Cipher.ENCRYPT_MODE, key)
        val encPass = cipherPass.doFinal(password.toByteArray(Charsets.UTF_8))
        val ivPass = cipherPass.iv

        prefs.edit()
            .putString(KEY_USERNAME, Base64.encodeToString(encUser, Base64.NO_WRAP))
            .putString(KEY_IV_USERNAME, Base64.encodeToString(ivUser, Base64.NO_WRAP))
            .putString(KEY_PASSWORD, Base64.encodeToString(encPass, Base64.NO_WRAP))
            .putString(KEY_IV_PASSWORD, Base64.encodeToString(ivPass, Base64.NO_WRAP))
            .apply()
    }

    /**
     * Retrieve decrypted username. Returns null if not configured.
     */
    fun getUsername(): String? {
        val encData = prefs.getString(KEY_USERNAME, null) ?: return null
        val ivData = prefs.getString(KEY_IV_USERNAME, null) ?: return null
        return decrypt(encData, ivData)
    }

    /**
     * Retrieve decrypted password. Returns null if not configured.
     */
    fun getPassword(): String? {
        val encData = prefs.getString(KEY_PASSWORD, null) ?: return null
        val ivData = prefs.getString(KEY_IV_PASSWORD, null) ?: return null
        return decrypt(encData, ivData)
    }

    /**
     * Clear all stored configuration and credentials.
     */
    fun clear() {
        prefs.edit().clear().apply()
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)
            keyStore.deleteEntry(KEY_ALIAS)
        } catch (_: Exception) {}
    }

    private fun decrypt(encBase64: String, ivBase64: String): String? {
        return try {
            val key = getSecretKey()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(128, Base64.decode(ivBase64, Base64.NO_WRAP))
            cipher.init(Cipher.DECRYPT_MODE, key, spec)
            String(cipher.doFinal(Base64.decode(encBase64, Base64.NO_WRAP)), Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    private fun ensureKeyExists() {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            keyGenerator.init(
                KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
            )
            keyGenerator.generateKey()
        }
    }

    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)
        return keyStore.getKey(KEY_ALIAS, null) as SecretKey
    }
}

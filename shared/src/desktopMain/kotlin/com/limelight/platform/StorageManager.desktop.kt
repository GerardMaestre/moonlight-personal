package com.limelight.platform

import java.util.prefs.Preferences

actual class StorageManager {
    private val prefs = Preferences.userNodeForPackage(StorageManager::class.java)

    actual fun putString(key: String, value: String) {
        prefs.put(key, value)
        runCatching { prefs.flush() }
    }

    actual fun getString(key: String): String? {
        val value = prefs.get(key, null)
        return if (value.isNullOrBlank()) null else value
    }
}

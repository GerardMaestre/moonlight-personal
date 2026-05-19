package com.limelight.platform

import android.content.Context
import androidx.core.content.edit

actual class StorageManager {
    companion object {
        private lateinit var appContext: Context

        fun initialize(context: Context) {
            appContext = context.applicationContext
        }
    }

    private val prefs by lazy {
        appContext.getSharedPreferences("com.limelight.shared", Context.MODE_PRIVATE)
    }

    actual fun putString(key: String, value: String) {
        prefs.edit { putString(key, value) }
    }

    actual fun getString(key: String): String? {
        return prefs.getString(key, null)?.takeIf { it.isNotBlank() }
    }

    actual fun remove(key: String) {
        prefs.edit { remove(key) }
    }
}

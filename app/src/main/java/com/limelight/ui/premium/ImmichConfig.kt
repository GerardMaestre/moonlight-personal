package com.limelight.ui.premium

import android.content.Context

/**
 * Persists Immich connection settings (API Key, Base URL).
 */
class ImmichConfig private constructor(context: Context) {

    companion object {
        private const val PREFS_NAME = "immich_config"
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_API_KEY = "api_key"

        fun getInstance(context: Context): ImmichConfig {
            return ImmichConfig(context.applicationContext)
        }
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var baseUrl: String
        get() = prefs.getString(KEY_BASE_URL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_BASE_URL, value.trim().trimEnd('/')).apply()

    var apiKey: String
        get() = prefs.getString(KEY_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_API_KEY, value.trim()).apply()

    val hasCredentials: Boolean get() = apiKey.isNotBlank()
}

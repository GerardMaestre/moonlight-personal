package com.limelight.ui.premium

import android.content.Context

/**
 * Configuration manager for Remote Script Execution (Stream Deck).
 */
class RemoteScriptConfig private constructor(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "remote_script_config"
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_TOKEN = "auth_token"

        fun getInstance(context: Context): RemoteScriptConfig {
            return RemoteScriptConfig(context.applicationContext)
        }
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var serverUrl: String
        get() = prefs.getString(KEY_SERVER_URL, "http://100.67.140.39:3000") ?: "http://100.67.140.39:3000"
        set(value) = prefs.edit().putString(KEY_SERVER_URL, value.trim().trimEnd('/')).apply()

    var token: String
        get() = prefs.getString(KEY_TOKEN, "CasaGerard") ?: "CasaGerard"
        set(value) = prefs.edit().putString(KEY_TOKEN, value.trim()).apply()
}

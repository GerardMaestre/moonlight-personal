package com.limelight.platform

actual class StorageManager {
    private val data = mutableMapOf<String, String>()

    actual fun putString(key: String, value: String) {
        data[key] = value
    }

    actual fun getString(key: String): String? = data[key]
}

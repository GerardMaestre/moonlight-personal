package com.limelight.platform

expect class StorageManager() {
    fun putString(key: String, value: String)
    fun getString(key: String): String?
}

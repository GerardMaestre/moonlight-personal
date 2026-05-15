package com.limelight.platform

expect class Logger() {
    fun debug(tag: String, message: String)
    fun error(tag: String, message: String, exception: Throwable? = null)
}

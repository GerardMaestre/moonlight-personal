package com.limelight.platform

actual class Logger {
    actual fun debug(tag: String, message: String) {
        println("[DEBUG] $tag: $message")
    }

    actual fun error(tag: String, message: String, exception: Throwable?) {
        System.err.println("[ERROR] $tag: $message")
        exception?.printStackTrace()
    }
}

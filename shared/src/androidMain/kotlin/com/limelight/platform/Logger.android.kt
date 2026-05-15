package com.limelight.platform

import android.util.Log

actual class Logger {
    actual fun debug(tag: String, message: String) {
        Log.d(tag, message)
    }

    actual fun error(tag: String, message: String, exception: Throwable?) {
        Log.e(tag, message, exception)
    }
}

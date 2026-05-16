package com.limelight.shared.data.remote.repository

sealed class DataError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class Network(cause: Throwable? = null) : DataError("Network error", cause)
    class Unauthorized(cause: Throwable? = null) : DataError("Unauthorized", cause)
    class NotFound(cause: Throwable? = null) : DataError("Not found", cause)
    class Serialization(cause: Throwable? = null) : DataError("Serialization error", cause)
    class Validation(message: String) : DataError(message)
    class Unknown(cause: Throwable? = null) : DataError("Unknown data error", cause)
}

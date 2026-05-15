package com.limelight.connection

import com.limelight.discovery.PcInfo

interface StreamConnection {
    suspend fun connect(target: PcInfo): Result<Unit>
    suspend fun disconnect()
}

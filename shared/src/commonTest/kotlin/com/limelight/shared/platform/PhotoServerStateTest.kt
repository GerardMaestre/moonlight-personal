package com.limelight.shared.platform

import kotlin.test.Test
import kotlin.test.assertEquals

class PhotoServerStateTest {
    @Test
    fun `error status updates current status and last error`() {
        val state = PhotoServerState()

        state.updateStatus(PhotoServerStatus.Error("Puerto ocupado"))

        assertEquals(PhotoServerStatus.Error("Puerto ocupado"), state.status)
        assertEquals("Puerto ocupado", state.lastError)
    }

    @Test
    fun `running status does not clear previous error`() {
        val state = PhotoServerState()

        state.updateStatus(PhotoServerStatus.Error("Fallo inicial"))
        state.updateStatus(PhotoServerStatus.Running(port = 47990, url = "http://127.0.0.1:47990"))

        assertEquals(
            PhotoServerStatus.Running(port = 47990, url = "http://127.0.0.1:47990"),
            state.status
        )
        assertEquals("Fallo inicial", state.lastError)
    }
}

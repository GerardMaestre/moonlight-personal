package com.limelight.shared.platform

/**
 * Platform abstraction layer. Each target provides its own implementation.
 */
expect fun getPlatformName(): String

/**
 * Callbacks for platform-specific actions triggered from shared UI.
 */
interface PlatformActions {
    fun onAddPc()
    fun onAddPcManual(ip: String)
    fun onOpenSettings()
    fun onPcClick(computerId: String, computerName: String)
    fun onPair(computerId: String)

    fun onWakeOnLan(macAddress: String)
    fun onNavigateBack()
}

/**
 * No-op implementation for previews and testing.
 */
object PreviewPlatformActions : PlatformActions {
    override fun onAddPc() {}
    override fun onAddPcManual(ip: String) {}
    override fun onOpenSettings() {}
    override fun onPcClick(computerId: String, computerName: String) {}
    override fun onPair(computerId: String) {}

    override fun onWakeOnLan(macAddress: String) {}
    override fun onNavigateBack() {}
}

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
    fun onOpenSettings()
    fun onPcClick(computerId: String, computerName: String)
    fun onApplyNetworkProfile(profileId: String)
    fun onWakeOnLan(macAddress: String)
    fun onNavigateBack()
}

/**
 * No-op implementation for previews and testing.
 */
object PreviewPlatformActions : PlatformActions {
    override fun onAddPc() {}
    override fun onOpenSettings() {}
    override fun onPcClick(computerId: String, computerName: String) {}
    override fun onApplyNetworkProfile(profileId: String) {}
    override fun onWakeOnLan(macAddress: String) {}
    override fun onNavigateBack() {}
}

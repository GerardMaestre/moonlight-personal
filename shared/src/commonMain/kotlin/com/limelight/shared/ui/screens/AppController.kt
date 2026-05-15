package com.limelight.shared.ui.screens

import com.limelight.shared.model.ComputerInfo
import com.limelight.shared.model.ComputerStatus
import com.limelight.shared.model.GameInfo
import com.limelight.shared.platform.PhotoServerState

/**
 * Shared source of truth for non-streaming dashboard state + intents.
 */
class AppController(
    val dashboardState: DashboardState = DashboardState(),
    val navigation: AppNavigation = AppNavigation()
) {
    val photoServerState: PhotoServerState = PhotoServerState()
    val powerControlState: PowerControlState = PowerControlState()
    var featureMessage: String? = null
        private set

    fun openMoonlight() = navigation.navigateTo(AppScreen.MOONLIGHT)
    fun openPowerControl() {
        navigation.navigateTo(AppScreen.POWER_CONTROL)
    }
    fun openPhotoServer() {
        navigation.navigateTo(AppScreen.PHOTO_SERVER)
    }

    fun openComputer(computerId: String, fallbackGames: List<GameInfo> = emptyList()) {
        dashboardState.selectedComputer = dashboardState.computers.find { it.id == computerId }
        if (dashboardState.games.isEmpty() && fallbackGames.isNotEmpty()) {
            dashboardState.games.clear()
            dashboardState.games.addAll(fallbackGames)
        }
        navigation.navigateTo(AppScreen.GAME_LIST)
    }

    fun updateDiscoveredComputer(computer: ComputerInfo) = dashboardState.updateComputer(computer)

    fun onManualPairResult(computerId: String, paired: Boolean) {
        val computer = dashboardState.computers.find { it.id == computerId } ?: return
        updateDiscoveredComputer(computer.copy(isPaired = paired))
    }

    fun onGamesLoaded(games: List<GameInfo>) {
        dashboardState.games.clear()
        dashboardState.games.addAll(games)
    }

    fun onWakeOnLanDispatched(macAddress: String) {
        dashboardState.showMessage("Enviando señal de encendido (WOL) a $macAddress...")
    }

    fun onDiscoveryStatus(message: String) {
        dashboardState.showMessage(message)
    }

    fun closeFeatureMessage() {
        featureMessage = null
    }

    companion object {
        fun desktopFallbackGames() = listOf(
            GameInfo(1, "Steam", boxArtUrl = ""),
            GameInfo(2, "Desktop", boxArtUrl = ""),
            GameInfo(3, "Epic Games", boxArtUrl = "")
        )

        fun manualComputer(ip: String) = ComputerInfo(
            id = ip,
            name = "PC Manual ($ip)",
            status = ComputerStatus.ONLINE,
            localAddress = ip,
            isPaired = true
        )
    }
}

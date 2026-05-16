package com.limelight.shared.ui.screens

import com.limelight.shared.model.ComputerInfo
import com.limelight.shared.model.ComputerStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppControllerTest {
    @Test
    fun `discovery upserts same computer id`() {
        val controller = AppController()

        controller.updateDiscoveredComputer(
            ComputerInfo(
                id = "pc-1",
                name = "Gaming PC",
                status = ComputerStatus.OFFLINE,
                isPaired = false
            )
        )
        controller.updateDiscoveredComputer(
            ComputerInfo(
                id = "pc-1",
                name = "Gaming PC",
                status = ComputerStatus.ONLINE,
                isPaired = true
            )
        )

        assertEquals(1, controller.dashboardState.computers.size)
        assertEquals(ComputerStatus.ONLINE, controller.dashboardState.computers.single().status)
        assertTrue(controller.dashboardState.computers.single().isPaired)
    }

    @Test
    fun `wol dispatch publishes feedback message`() {
        val controller = AppController()

        controller.onWakeOnLanDispatched("AA:BB:CC:DD:EE:FF")

        assertEquals(
            "Enviando señal de encendido (WOL) a AA:BB:CC:DD:EE:FF...",
            controller.dashboardState.lastActionMessage
        )
    }

    @Test
    fun `navigation avoids duplicate destinations`() {
        val controller = AppController()

        controller.openMoonlight()
        controller.openMoonlight()
        controller.navigation.goBack()

        assertEquals(AppScreen.MAIN_MENU, controller.navigation.currentScreen)
    }

    @Test
    fun `root navigation replaces nested stack`() {
        val navigation = AppNavigation()

        navigation.navigateTo(AppScreen.MOONLIGHT)
        navigation.navigateTo(AppScreen.GAME_LIST)
        navigation.navigateRoot(AppScreen.POWER_CONTROL)
        navigation.goBack()

        assertEquals(AppScreen.POWER_CONTROL, navigation.currentScreen)
    }
}

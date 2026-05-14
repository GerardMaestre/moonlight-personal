package com.limelight.shared.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class AppScreen {
    MAIN_MENU,
    MOONLIGHT,
    POWER_CONTROL,
    PHOTO_SERVER
}

class AppNavigation {
    var currentScreen by mutableStateOf(AppScreen.MAIN_MENU)
        private set

    fun navigateTo(screen: AppScreen) {
        currentScreen = screen
    }

    fun goBack() {
        if (currentScreen != AppScreen.MAIN_MENU) {
            currentScreen = AppScreen.MAIN_MENU
        }
    }
}

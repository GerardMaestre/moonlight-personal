package com.limelight.shared.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue

enum class AppScreen {
    MAIN_MENU,
    MOONLIGHT,
    GAME_LIST,
    POWER_CONTROL,
    PHOTO_SERVER
}

class AppNavigation {
    private val screenStack = mutableStateListOf(AppScreen.MAIN_MENU)
    
    val currentScreen: AppScreen
        get() = screenStack.last()

    fun navigateTo(screen: AppScreen) {
        screenStack.add(screen)
    }

    fun goBack() {
        if (screenStack.size > 1) {
            screenStack.removeAt(screenStack.size - 1)
        }
    }
}

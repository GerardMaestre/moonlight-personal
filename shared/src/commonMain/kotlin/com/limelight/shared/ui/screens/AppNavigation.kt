package com.limelight.shared.ui.screens

import androidx.compose.runtime.mutableStateListOf

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

    val canGoBack: Boolean
        get() = screenStack.size > 1

    fun navigateTo(screen: AppScreen) {
        if (currentScreen != screen) {
            screenStack.add(screen)
        }
    }

    fun navigateRoot(screen: AppScreen) {
        screenStack.clear()
        screenStack.add(screen)
    }

    fun goBack() {
        if (canGoBack) {
            screenStack.removeAt(screenStack.lastIndex)
        }
    }
}

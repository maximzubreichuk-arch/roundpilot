package com.example.myintervaltimerproject.ui.navigation

sealed class AppRoute(
    val route: String
) {

    data object Home :
        AppRoute("home")

    data object Workout :
        AppRoute("workout")

    data object PresetEditor :
        AppRoute("preset_editor")

    data object Settings :
        AppRoute("settings")
}
package com.epic.aiexpensevoice.presentation.navigation

sealed class Route(val route: String) {
    data object Splash : Route("splash")
    data object Login : Route("login")
    data object Register : Route("register")
    data object Shell : Route("shell")
    data object Chat : Route("chat")
    data object Dashboard : Route("dashboard")
    data object Expenes : Route("expenes")
    data object Profile : Route("profile")
}

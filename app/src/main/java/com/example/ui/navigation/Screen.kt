package com.example.ui.navigation

/**
 * Rutas y destinos de navegación de la aplicación del navegador web.
 * Permite la navegación desacoplada entre pantallas dedicadas (navegador, pestañas, marcadores, historial y ajustes).
 */
sealed class Screen(val route: String) {
    data object Browser : Screen("browser")
    data object Tabs : Screen("tabs")
    data object Bookmarks : Screen("bookmarks")
    data object History : Screen("history")
    data object Settings : Screen("settings")
    data object Downloads : Screen("downloads")
    data object Cookies : Screen("cookies")
}

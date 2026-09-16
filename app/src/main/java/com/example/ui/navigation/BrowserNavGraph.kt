package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.bookmarks.BookmarksScreen
import com.example.ui.browser.BrowserScreen
import com.example.ui.downloads.DownloadsScreen
import com.example.ui.history.HistoryScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.tabs.TabsScreen
import com.example.viewmodel.BrowserViewModel

/**
 * Grafo de navegación principal de la aplicación del navegador web.
 * Administra el enrutamiento desacoplado entre las distintas pantallas:
 * Navegador, Pestañas, Marcadores, Historial, Descargas y Ajustes.
 */
@Composable
fun BrowserNavGraph(
    viewModel: BrowserViewModel,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Browser.route
    ) {
        composable(Screen.Browser.route) {
            BrowserScreen(
                viewModel = viewModel,
                onNavigateToTabs = { navController.navigate(Screen.Tabs.route) },
                onNavigateToBookmarks = { navController.navigate(Screen.Bookmarks.route) },
                onNavigateToHistory = { navController.navigate(Screen.History.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToDownloads = { navController.navigate(Screen.Downloads.route) }
            )
        }

        composable(Screen.Tabs.route) {
            TabsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Bookmarks.route) {
            BookmarksScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.History.route) {
            HistoryScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Downloads.route) {
            DownloadsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

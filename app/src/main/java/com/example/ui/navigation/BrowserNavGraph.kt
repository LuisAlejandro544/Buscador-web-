package com.example.ui.navigation

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.account.AccountsScreen
import com.example.ui.bookmarks.BookmarksScreen
import com.example.ui.browser.BrowserScreen
import com.example.ui.cookies.CookiesScreen
import com.example.ui.debug.CrashInspectorActivity
import com.example.ui.downloads.DownloadsScreen
import com.example.ui.extension.ExtensionsScreen
import com.example.ui.history.HistoryScreen
import com.example.ui.onboarding.OnboardingScreen
import com.example.ui.permissions.SitePermissionsScreen
import com.example.ui.reader.ReaderScreen
import com.example.ui.security.SecurityThreatScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.tabs.TabsScreen
import com.example.viewmodel.BrowserViewModel

/**
 * Grafo de navegación principal de la aplicación del navegador web.
 * Administra el enrutamiento desacoplado entre las distintas pantallas dedicadas:
 * Navegador, Pestañas, Marcadores, Historial, Descargas, Cookies, Ajustes, Cuentas,
 * Permisos por Sitio, Extensiones y Onboarding de bienvenida inicial.
 */
@Composable
fun BrowserNavGraph(
    viewModel: BrowserViewModel,
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val isOnboardingCompleted by viewModel.isOnboardingCompleted.collectAsStateWithLifecycle()

    NavHost(
        navController = navController,
        startDestination = if (isOnboardingCompleted) Screen.Browser.route else Screen.Onboarding.route
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                viewModel = viewModel,
                onCompleteOnboarding = {
                    navController.navigate(Screen.Browser.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Browser.route) {
            BrowserScreen(
                viewModel = viewModel,
                onNavigateToTabs = { navController.navigate(Screen.Tabs.route) },
                onNavigateToBookmarks = { navController.navigate(Screen.Bookmarks.route) },
                onNavigateToHistory = { navController.navigate(Screen.History.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToDownloads = { navController.navigate(Screen.Downloads.route) },
                onNavigateToCookies = { navController.navigate(Screen.Cookies.route) },
                onNavigateToAccounts = { navController.navigate(Screen.Accounts.route) },
                onNavigateToExtensions = { navController.navigate(Screen.Extensions.route) },
                onNavigateToReaderMode = { navController.navigate(Screen.ReaderMode.route) }
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
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Cookies.route) {
            CookiesScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Accounts.route) {
            AccountsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.SitePermissions.route) {
            SitePermissionsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Extensions.route) {
            ExtensionsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ReaderMode.route) {
            ReaderScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCookies = { navController.navigate(Screen.Cookies.route) },
                onNavigateToAccounts = { navController.navigate(Screen.Accounts.route) },
                onNavigateToSitePermissions = { navController.navigate(Screen.SitePermissions.route) },
                onNavigateToExtensions = { navController.navigate(Screen.Extensions.route) },
                onNavigateToSecurityThreats = { navController.navigate(Screen.SecurityThreats.route) },
                onNavigateToCrashInspector = {
                    val intent = Intent(context, CrashInspectorActivity::class.java)
                    context.startActivity(intent)
                }
            )
        }

        composable(Screen.SecurityThreats.route) {
            SecurityThreatScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

package com.example.ui.browser

import android.content.Context
import android.content.Intent
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.browser.engine.GeckoViewEngine
import com.example.ui.components.WebPromptDialog
import com.example.ui.components.WebSignInPromptBanner
import com.example.ui.security.ThreatBlockedScreen
import com.example.viewmodel.BrowserViewModel
import org.mozilla.geckoview.GeckoView

/**
 * Pantalla principal del navegador web.
 * 
 * Integra:
 * - [OmniboxField]: Barra de direcciones superior con indicador de cifrado y limpieza rápida.
 * - [BrowserActionMenu]: Menú contextual desplegable con todas las acciones del navegador.
 * - Contenedor nativo [GeckoView] persistente.
 * - [BrowserStartPage]: Pantalla de inicio rápida con marcadores, historial y accesos directos.
 * - [WebPromptDialog]: Diálogos nativos de JavaScript y autenticación HTTP.
 * - [WebSignInPromptBanner]: Asistencia de inicio de sesión con cuentas vinculadas.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BrowserScreen(
    viewModel: BrowserViewModel,
    onNavigateToTabs: () -> Unit,
    onNavigateToBookmarks: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToDownloads: () -> Unit,
    onNavigateToCookies: () -> Unit = {},
    onNavigateToAccounts: () -> Unit = {},
    onNavigateToExtensions: () -> Unit = {},
    onNavigateToReaderMode: () -> Unit = {}
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val isImeVisible = WindowInsets.isImeVisible

    val pageState by viewModel.pageState.collectAsState()
    val omniboxText by viewModel.omniboxText.collectAsState()
    val searchEngine by viewModel.searchEngine.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()
    val recentHistory by viewModel.recentHistory.collectAsState()
    val isIncognito by viewModel.isIncognitoMode.collectAsState()
    val isProtected by viewModel.isProtectedMode.collectAsState()
    val normalTabs by viewModel.normalTabs.collectAsState()
    val protectedTabs by viewModel.protectedTabs.collectAsState()
    val incognitoTabs by viewModel.incognitoTabs.collectAsState()
    val isBookmarked by viewModel.isCurrentPageBookmarked.collectAsState()
    val activeTabId by viewModel.activeTabId.collectAsState()
    val activeTab by viewModel.activeTab.collectAsState()
    val isDesktopDefault by viewModel.isDesktopModeDefault.collectAsState()
    val activeWebPrompt by viewModel.activeWebPrompt.collectAsState()
    val webSignInPrompt by viewModel.webSignInPrompt.collectAsState()
    val currentBlockedThreat by viewModel.currentBlockedThreat.collectAsState()

    val currentTabsCount = when {
        isProtected -> protectedTabs.size
        isIncognito -> incognitoTabs.size
        else -> normalTabs.size
    }

    val emeraldColor = Color(0xFF00897B)
    var isMenuExpanded by remember { mutableStateOf(false) }
    var isInputFocused by remember { mutableStateOf(false) }

    // Instancia persistente del contenedor GeckoView
    val geckoView = remember {
        GeckoView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    // Registrar el motor GeckoView en el ViewModel según la pestaña activa
    DisposableEffect(activeTabId, isIncognito, isProtected) {
        val tabId = activeTabId ?: 1L
        val session = viewModel.sessionManager.getOrCreateSession(
            tabId = tabId,
            isIncognito = isIncognito,
            isProtected = isProtected,
            contextId = activeTab?.contextId,
            isDesktopMode = pageState.isDesktopMode || isDesktopDefault
        )
        val engine = GeckoViewEngine(context, geckoView, viewModel, session)
        viewModel.engineController = engine
        onDispose {
            engine.release()
            viewModel.engineController = null
        }
    }

    // Manejo de botón Atrás del sistema
    BackHandler(enabled = pageState.canGoBack) {
        viewModel.engineController?.goBack()
    }

    val isHome = pageState.url == "about:home" || pageState.url.isBlank() || pageState.url == "about:blank"

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TopAppBar(
                    title = {
                        OmniboxField(
                            value = omniboxText,
                            isFocused = isInputFocused,
                            isSecure = pageState.isSecure,
                            isProtected = isProtected,
                            isHome = isHome,
                            onValueChange = { viewModel.onOmniboxTextChange(it) },
                            onFocusChange = { isInputFocused = it },
                            onSearch = { query ->
                                focusManager.clearFocus()
                                viewModel.loadInput(query)
                            },
                            onClear = { viewModel.onOmniboxTextChange("") }
                        )
                    },
                    actions = {
                        if (pageState.isLoading) {
                            IconButton(
                                onClick = { viewModel.engineController?.stopLoading() },
                                modifier = Modifier.testTag("stop_button")
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Detener carga")
                            }
                        } else if (!isHome) {
                            IconButton(
                                onClick = {
                                    viewModel.openReaderMode(pageState.url, pageState.title)
                                    onNavigateToReaderMode()
                                },
                                modifier = Modifier.testTag("action_reader_mode_button")
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = "Activar Modo Lectura",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            IconButton(
                                onClick = { viewModel.engineController?.reload() },
                                modifier = Modifier.testTag("reload_button")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Recargar")
                            }
                        }

                        // Selector de pestañas con contador
                        IconButton(
                            onClick = {
                                viewModel.captureCurrentTabThumbnail()
                                onNavigateToTabs()
                            },
                            modifier = Modifier.testTag("tabs_button")
                        ) {
                            BadgedBox(
                                badge = {
                                    Badge(
                                        containerColor = when {
                                            isProtected -> emeraldColor
                                            isIncognito -> MaterialTheme.colorScheme.tertiary
                                            else -> MaterialTheme.colorScheme.primary
                                        }
                                    ) {
                                        Text(text = "$currentTabsCount", fontSize = 10.sp)
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Tab, contentDescription = "Pestañas abiertas")
                            }
                        }

                        // Menú de opciones
                        IconButton(
                            onClick = { isMenuExpanded = true },
                            modifier = Modifier.testTag("menu_button")
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menú del navegador")
                        }

                        // Menú desplegable modular
                        BrowserActionMenu(
                            expanded = isMenuExpanded,
                            isIncognito = isIncognito,
                            isBookmarked = isBookmarked,
                            isHome = isHome,
                            isDesktopMode = pageState.isDesktopMode,
                            onDismissRequest = { isMenuExpanded = false },
                            onNewTab = { viewModel.createNewTab("about:home", isIncognito = false, isProtected = false) },
                            onNewProtectedTab = { viewModel.createProtectedTab("about:home") },
                            onToggleIncognito = { viewModel.toggleIncognitoMode(!isIncognito) },
                            onToggleBookmark = { viewModel.toggleCurrentBookmark() },
                            onNavigateToBookmarks = onNavigateToBookmarks,
                            onNavigateToHistory = onNavigateToHistory,
                            onNavigateToDownloads = onNavigateToDownloads,
                            onNavigateToExtensions = onNavigateToExtensions,
                            onNavigateToCookies = onNavigateToCookies,
                            onToggleDesktopMode = { viewModel.toggleDesktopMode() },
                            onShare = { shareUrl(context, pageState.title, pageState.url) },
                            onNavigateToAccounts = onNavigateToAccounts,
                            onNavigateToSettings = onNavigateToSettings,
                            onNavigateToReaderMode = {
                                viewModel.openReaderMode(pageState.url, pageState.title)
                                onNavigateToReaderMode()
                            }
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )

                // Barra de progreso de carga de página
                AnimatedVisibility(visible = pageState.isLoading && !isHome) {
                    val progressAnimated by animateFloatAsState(
                        targetValue = pageState.progress / 100f,
                        label = "progress"
                    )
                    LinearProgressIndicator(
                        progress = { progressAnimated },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = !isImeVisible,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it }
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .height(56.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.engineController?.goBack() },
                            enabled = pageState.canGoBack,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("bottom_back_button")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Atrás",
                                tint = if (pageState.canGoBack) MaterialTheme.colorScheme.onSurface
                                       else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        }

                        IconButton(
                            onClick = { viewModel.engineController?.goForward() },
                            enabled = pageState.canGoForward,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("bottom_forward_button")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Adelante",
                                tint = if (pageState.canGoForward) MaterialTheme.colorScheme.onSurface
                                       else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        }

                        IconButton(
                            onClick = { viewModel.navigateToHome() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("bottom_home_button")
                        ) {
                            Icon(
                                Icons.Default.Home,
                                contentDescription = "Inicio",
                                tint = if (isHome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        IconButton(
                            onClick = onNavigateToBookmarks,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("bottom_bookmarks_button")
                        ) {
                            Icon(Icons.Default.Bookmark, contentDescription = "Marcadores")
                        }

                        IconButton(
                            onClick = onNavigateToTabs,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("bottom_tabs_button")
                        ) {
                            BadgedBox(
                                badge = {
                                    Badge(
                                        containerColor = when {
                                            isProtected -> emeraldColor
                                            isIncognito -> MaterialTheme.colorScheme.tertiary
                                            else -> MaterialTheme.colorScheme.primary
                                        }
                                    ) {
                                        Text(text = "$currentTabsCount", fontSize = 10.sp)
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Tab, contentDescription = "Pestañas")
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Superficie GeckoView nativa
            AndroidView(
                factory = { geckoView },
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("browser_geckoview")
            )

            if (isHome) {
                BrowserStartPage(
                    searchEngine = searchEngine,
                    bookmarks = bookmarks,
                    recentHistory = recentHistory,
                    isIncognito = isIncognito,
                    isProtected = isProtected,
                    onNavigateToUrl = { url -> viewModel.loadInput(url) },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Banner flotante de inicio de sesión web con un toque
            WebSignInPromptBanner(
                prompt = webSignInPrompt,
                onAccept = { account -> viewModel.acceptWebSignInPrompt(account) },
                onDismiss = { viewModel.dismissWebSignInPrompt() },
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }

        // Diálogos web de prompts y autenticación
        WebPromptDialog(
            promptRequest = activeWebPrompt,
            onDismissRequest = { viewModel.dismissWebPrompt() }
        )

        // Pantalla de advertencia crítica por amenaza de phishing / malware interceptada
        currentBlockedThreat?.let { threat ->
            ThreatBlockedScreen(
                threat = threat,
                onSafeReturn = {
                    viewModel.dismissBlockedThreat()
                    viewModel.navigateToHome()
                },
                onBypassThreat = { domain ->
                    viewModel.bypassThreatAndAllow(domain)
                    viewModel.engineController?.reload()
                }
            )
        }
    }
}

/**
 * Comparte la URL activa mediante el Intent chooser de Android.
 */
private fun shareUrl(context: Context, title: String, url: String) {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, url)
        type = "text/plain"
    }
    context.startActivity(Intent.createChooser(sendIntent, "Compartir enlace"))
}

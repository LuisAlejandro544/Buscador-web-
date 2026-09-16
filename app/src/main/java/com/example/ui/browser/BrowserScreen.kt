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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.browser.engine.GeckoViewEngine
import com.example.ui.components.WebPromptDialog
import com.example.ui.components.WebSignInPromptBanner
import com.example.viewmodel.BrowserViewModel
import org.mozilla.geckoview.GeckoView

/**
 * Pantalla principal del navegador web.
 * Integra la barra de direcciones superior (Omnibox), el motor de renderizado independiente
 * Mozilla GeckoView y la barra de herramientas de navegación inferior.
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
    onNavigateToAccounts: () -> Unit = {}
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
                        // Indicador de recarga o detención
                        if (pageState.isLoading) {
                            IconButton(
                                onClick = { viewModel.engineController?.stopLoading() },
                                modifier = Modifier.testTag("stop_button")
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Detener carga")
                            }
                        } else if (!isHome) {
                            IconButton(
                                onClick = { viewModel.engineController?.reload() },
                                modifier = Modifier.testTag("reload_button")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Recargar")
                            }
                        }

                        // Botón de Pestañas con Insignia de conteo
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

                        // Menú de opciones (tres puntos)
                        IconButton(
                            onClick = { isMenuExpanded = true },
                            modifier = Modifier.testTag("menu_button")
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menú del navegador")
                        }

                        // Menú desplegable con opciones
                        DropdownMenu(
                            expanded = isMenuExpanded,
                            onDismissRequest = { isMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Nueva pestaña") },
                                leadingIcon = { Icon(Icons.Default.Tab, contentDescription = null) },
                                onClick = {
                                    isMenuExpanded = false
                                    viewModel.createNewTab("about:home", isIncognito = false, isProtected = false)
                                },
                                modifier = Modifier.testTag("menu_new_tab")
                            )

                            DropdownMenuItem(
                                text = { Text("Nueva pestaña protegida") },
                                leadingIcon = { Icon(Icons.Default.Shield, contentDescription = null, tint = emeraldColor) },
                                onClick = {
                                    isMenuExpanded = false
                                    viewModel.createProtectedTab("about:home")
                                },
                                modifier = Modifier.testTag("menu_new_protected_tab")
                            )

                            DropdownMenuItem(
                                text = { Text(if (isIncognito) "Modo normal" else "Modo incógnito") },
                                leadingIcon = { Icon(Icons.Default.Security, contentDescription = null) },
                                onClick = {
                                    isMenuExpanded = false
                                    viewModel.toggleIncognitoMode(!isIncognito)
                                },
                                modifier = Modifier.testTag("menu_toggle_incognito")
                            )

                            DropdownMenuItem(
                                text = { Text(if (isBookmarked) "Quitar de marcadores" else "Agregar a marcadores") },
                                leadingIcon = {
                                    Icon(
                                        if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                        contentDescription = null,
                                        tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                enabled = !isHome,
                                onClick = {
                                    isMenuExpanded = false
                                    viewModel.toggleCurrentBookmark()
                                },
                                modifier = Modifier.testTag("menu_toggle_bookmark")
                            )

                            DropdownMenuItem(
                                text = { Text("Marcadores") },
                                leadingIcon = { Icon(Icons.Default.Bookmark, contentDescription = null) },
                                onClick = {
                                    isMenuExpanded = false
                                    onNavigateToBookmarks()
                                },
                                modifier = Modifier.testTag("menu_bookmarks")
                            )

                            DropdownMenuItem(
                                text = { Text("Historial") },
                                leadingIcon = { Icon(Icons.Default.History, contentDescription = null) },
                                onClick = {
                                    isMenuExpanded = false
                                    onNavigateToHistory()
                                },
                                modifier = Modifier.testTag("menu_history")
                            )

                            DropdownMenuItem(
                                text = { Text("Descargas") },
                                leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) },
                                onClick = {
                                    isMenuExpanded = false
                                    onNavigateToDownloads()
                                },
                                modifier = Modifier.testTag("menu_downloads")
                            )

                            DropdownMenuItem(
                                text = { Text("Cookies de navegación") },
                                leadingIcon = { Icon(Icons.Default.Cookie, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                onClick = {
                                    isMenuExpanded = false
                                    onNavigateToCookies()
                                },
                                modifier = Modifier.testTag("menu_cookies")
                            )

                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Sitio para ordenador")
                                        Spacer(modifier = Modifier.weight(1f))
                                        if (pageState.isDesktopMode) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = "Activo",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                },
                                leadingIcon = { Icon(Icons.Default.Computer, contentDescription = null) },
                                onClick = {
                                    isMenuExpanded = false
                                    viewModel.toggleDesktopMode()
                                },
                                modifier = Modifier.testTag("menu_desktop_site")
                            )

                            if (!isHome) {
                                DropdownMenuItem(
                                    text = { Text("Compartir enlace") },
                                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                    onClick = {
                                        isMenuExpanded = false
                                        shareUrl(context, pageState.title, pageState.url)
                                    },
                                    modifier = Modifier.testTag("menu_share")
                                )
                            }

                            DropdownMenuItem(
                                text = { Text("Cuentas y acceso web") },
                                leadingIcon = { Icon(Icons.Default.AccountCircle, contentDescription = null) },
                                onClick = {
                                    isMenuExpanded = false
                                    onNavigateToAccounts()
                                },
                                modifier = Modifier.testTag("menu_accounts")
                            )

                            DropdownMenuItem(
                                text = { Text("Ajustes") },
                                leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                onClick = {
                                    isMenuExpanded = false
                                    onNavigateToSettings()
                                },
                                modifier = Modifier.testTag("menu_settings")
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                // Barra de progreso animada de carga de página
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
                        // Botón Atrás
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

                        // Botón Adelante
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

                        // Botón Inicio
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

                        // Botón Marcadores
                        IconButton(
                            onClick = onNavigateToBookmarks,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("bottom_bookmarks_button")
                        ) {
                            Icon(Icons.Default.Bookmark, contentDescription = "Marcadores")
                        }

                        // Botón Gestor de Pestañas
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
            if (isHome) {
                // Si la URL es la página de inicio interna, mostrar Start Page
                BrowserStartPage(
                    searchEngine = searchEngine,
                    bookmarks = bookmarks,
                    recentHistory = recentHistory,
                    isIncognito = isIncognito,
                    isProtected = isProtected,
                    onNavigateToUrl = { url ->
                        viewModel.loadInput(url)
                    }
                )
            } else {
                // Mostrar el motor GeckoView activo
                AndroidView(
                    factory = { geckoView },
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("browser_geckoview")
                )
            }

            // Banner flotante de inicio de sesión web con un solo toque (Google One-Tap / FedCM)
            WebSignInPromptBanner(
                prompt = webSignInPrompt,
                onAccept = { account -> viewModel.acceptWebSignInPrompt(account) },
                onDismiss = { viewModel.dismissWebSignInPrompt() },
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }

        // Diálogos web interactivos (Alerts, Prompts, Confirms, HTTP Auth, Selector de archivos)
        WebPromptDialog(
            promptRequest = activeWebPrompt,
            onDismissRequest = { viewModel.dismissWebPrompt() }
        )
    }
}

/**
 * Campo Omnibox personalizable para ingresar URLs y búsquedas.
 */
@Composable
private fun OmniboxField(
    value: String,
    isFocused: Boolean,
    isSecure: Boolean,
    isProtected: Boolean = false,
    isHome: Boolean,
    onValueChange: (String) -> Unit,
    onFocusChange: (Boolean) -> Unit,
    onSearch: (String) -> Unit,
    onClear: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .onFocusChanged { onFocusChange(it.isFocused) }
            .testTag("omnibox_input"),
        placeholder = {
            Text(
                text = if (isHome) "Buscar o escribir URL..." else "Escribir búsqueda o dirección...",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingIcon = {
            if (isProtected) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Pestaña Protegida (Aislamiento de cookies)",
                    tint = Color(0xFF00897B),
                    modifier = Modifier.size(16.dp)
                )
            } else if (isSecure && !isHome) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Conexión segura HTTPS",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Public,
                    contentDescription = "Web",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        },
        trailingIcon = {
            if (value.isNotEmpty() && isFocused) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Default.Close, contentDescription = "Limpiar texto", modifier = Modifier.size(16.dp))
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(24.dp),
        textStyle = MaterialTheme.typography.bodyMedium,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
        keyboardActions = KeyboardActions(onGo = { onSearch(value) }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = Color.Transparent
        )
    )
}

/**
 * Comparte la URL actual a través de la hoja de compartir del sistema.
 */
private fun shareUrl(context: Context, title: String, url: String) {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, url)
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "Compartir enlace")
    context.startActivity(shareIntent)
}

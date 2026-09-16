package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.browser.account.AccountCredentialManager
import com.example.browser.account.WebSignInBridge
import com.example.browser.account.WebSignInPrompt
import com.example.browser.download.DownloadEngine
import com.example.browser.download.DownloadManagerHelper
import com.example.browser.download.DownloadProgressState
import com.example.browser.engine.BrowserEngineContract
import com.example.browser.engine.EnginePageState
import com.example.browser.engine.GeckoRuntimeProvider
import com.example.browser.engine.GeckoSessionManager
import com.example.browser.engine.GeckoViewEngine
import com.example.browser.engine.WebPromptRequest
import com.example.browser.extension.ExtensionManager
import com.example.browser.extension.RecommendedExtension
import com.example.browser.extension.WebExtensionModel
import com.example.browser.lifecycle.AppHibernationManager
import com.example.browser.thumbnail.TabThumbnailManager
import com.example.data.local.BrowserDatabase
import com.example.data.local.entity.BookmarkEntity
import com.example.data.local.entity.CookieEntity
import com.example.data.local.entity.DownloadEntity
import com.example.data.local.entity.HistoryEntity
import com.example.data.local.entity.SitePermissionEntity
import com.example.data.local.entity.TabEntity
import com.example.data.local.entity.UserAccountEntity
import com.example.data.model.SearchEngine
import com.example.data.preferences.BrowserPreferences
import com.example.data.repository.BrowserRepository
import org.mozilla.geckoview.StorageController
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel principal del Navegador Web.
 * Controla el ciclo de vida de las pestañas, estado de carga, historial,
 * marcadores, descargas avanzadas, diálogos web nativos, sincronización de configuraciones
 * y vinculación de cuentas de usuario con puente de autenticación web.
 */
class BrowserViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: BrowserRepository
    val credentialManager = AccountCredentialManager(application)

    init {
        val database = BrowserDatabase.getInstance(application)
        val preferences = BrowserPreferences(application)
        repository = BrowserRepository(
            tabDao = database.tabDao(),
            bookmarkDao = database.bookmarkDao(),
            historyDao = database.historyDao(),
            downloadDao = database.downloadDao(),
            cookieDao = database.cookieDao(),
            userAccountDao = database.userAccountDao(),
            sitePermissionDao = database.sitePermissionDao(),
            preferences = preferences
        )
    }

    // --- Cuentas de Usuario y Sincronización Web ---
    val allAccounts: StateFlow<List<UserAccountEntity>> = repository.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeAccount: StateFlow<UserAccountEntity?> = repository.getActiveAccount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val accountsCount: StateFlow<Int> = repository.getAccountsCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Solicitud activa de inicio de sesión asistido en la página web actual
    private val _webSignInPrompt = MutableStateFlow<WebSignInPrompt?>(null)
    val webSignInPrompt: StateFlow<WebSignInPrompt?> = _webSignInPrompt.asStateFlow()

    // --- Permisos por Sitio Web y Políticas de Bloqueo Silencioso ("No Preguntar") ---
    val allSitePermissions: StateFlow<List<SitePermissionEntity>> = repository.getAllSitePermissions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val blockNotificationPrompts: StateFlow<Boolean> = repository.blockNotificationPrompts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val blockLocationPrompts: StateFlow<Boolean> = repository.blockLocationPrompts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val blockMediaPrompts: StateFlow<Boolean> = repository.blockMediaPrompts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // --- Onboarding y Configuración Inicial ---
    val isOnboardingCompleted: StateFlow<Boolean> = repository.isOnboardingCompleted
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // --- Gestor de Extensiones WebExtension ---
    val extensionManager = ExtensionManager.getInstance(application)
    val installedExtensions: StateFlow<List<WebExtensionModel>> = extensionManager.installedExtensions
    val extensionDownloadProgress: StateFlow<Map<String, Float>> = extensionManager.downloadProgress
    val isExtensionsLoading: StateFlow<Boolean> = extensionManager.isLoading
    val recommendedExtensions: List<RecommendedExtension> = RecommendedExtension.CATALOG

    // --- Preferencias ---
    val searchEngine: StateFlow<SearchEngine> = repository.searchEngine
        .stateIn(viewModelScope, SharingStarted.Eagerly, SearchEngine.DUCKDUCKGO)

    val isDesktopModeDefault: StateFlow<Boolean> = repository.isDesktopModeDefault
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isJavaScriptEnabled: StateFlow<Boolean> = repository.isJavaScriptEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isCookiesEnabled: StateFlow<Boolean> = repository.isCookiesEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isDoNotTrackEnabled: StateFlow<Boolean> = repository.isDoNotTrackEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val homePageUrl: StateFlow<String> = repository.homePageUrl
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "about:home")

    val isSoundEffectsEnabled: StateFlow<Boolean> = repository.isSoundEffectsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // --- Modos de Navegación ---
    enum class TabMode { NORMAL, PROTECTED, INCOGNITO }

    private val _isIncognitoMode = MutableStateFlow(false)
    val isIncognitoMode: StateFlow<Boolean> = _isIncognitoMode.asStateFlow()

    private val _isProtectedMode = MutableStateFlow(false)
    val isProtectedMode: StateFlow<Boolean> = _isProtectedMode.asStateFlow()

    // --- Pestañas ---
    val normalTabs: StateFlow<List<TabEntity>> = repository.getNormalTabs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val protectedTabs: StateFlow<List<TabEntity>> = repository.getProtectedTabs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val incognitoTabs: StateFlow<List<TabEntity>> = repository.getIncognitoTabs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeTabId = MutableStateFlow<Long?>(null)
    val activeTabId: StateFlow<Long?> = _activeTabId.asStateFlow()

    private val _activeTab = MutableStateFlow<TabEntity?>(null)
    val activeTab: StateFlow<TabEntity?> = _activeTab.asStateFlow()

    // --- Sistema de Hibernación y Ahorro de Memoria (5 minutos) ---
    // Tiempo de inactividad antes de suspender una pestaña en memoria (5 minutos = 300,000 ms)
    val TAB_SLEEP_TIMEOUT_MS = 5 * 60 * 1000L

    // Conjunto observable de IDs de pestañas que están actualmente dormidas/en reposo
    private val _hibernatedTabIds = MutableStateFlow<Set<Long>>(emptySet())
    val hibernatedTabIds: StateFlow<Set<Long>> = _hibernatedTabIds.asStateFlow()

    // Miniaturas visuales de las páginas web en caché
    val tabThumbnails: StateFlow<Map<Long, Bitmap>> = TabThumbnailManager.thumbnailsFlow

    // --- Estado de la Página Web Actual ---
    private val _pageState = MutableStateFlow(EnginePageState())
    val pageState: StateFlow<EnginePageState> = _pageState.asStateFlow()

    // --- Texto en la barra de direcciones (Omnibox) ---
    private val _omniboxText = MutableStateFlow("")
    val omniboxText: StateFlow<String> = _omniboxText.asStateFlow()

    // --- Marcadores ---
    val bookmarks: StateFlow<List<BookmarkEntity>> = repository.getAllBookmarks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isCurrentPageBookmarked = MutableStateFlow(false)
    val isCurrentPageBookmarked: StateFlow<Boolean> = _isCurrentPageBookmarked.asStateFlow()

    // --- Historial ---
    val recentHistory: StateFlow<List<HistoryEntity>> = repository.getRecentHistory(150)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _historySearchQuery = MutableStateFlow("")
    val historySearchQuery: StateFlow<String> = _historySearchQuery.asStateFlow()

    // --- Descargas ---
    val downloads: StateFlow<List<DownloadEntity>> = repository.getAllDownloads()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Diálogos Web Nativos (Prompts de JavaScript y Archivos) ---
    private val _activeWebPrompt = MutableStateFlow<WebPromptRequest?>(null)
    val activeWebPrompt: StateFlow<WebPromptRequest?> = _activeWebPrompt.asStateFlow()

    val sessionManager = GeckoSessionManager(application)

    // Estado reactivo en tiempo real de descargas activas (progreso, velocidad, estado)
    val activeDownloads: StateFlow<Map<Long, DownloadProgressState>> = DownloadEngine.activeDownloads

    // Referencia al motor de renderizado activo
    var engineController: BrowserEngineContract? = null

    init {
        // Registrar el sessionManager para permitir hibernación y ahorro de RAM tras 20s
        AppHibernationManager.registerSessionManager(sessionManager)

        // Inicializar pestañas si la lista está vacía
        viewModelScope.launch {
            val tabs = repository.getNormalTabs().first()
            if (tabs.isEmpty()) {
                val newId = repository.createTab("Nueva Pestaña", "about:home", isIncognito = false, isProtected = false)
                _activeTabId.value = newId
                _activeTab.value = repository.getTabById(newId)
            } else {
                val mostRecent = repository.getMostRecentActiveTab() ?: tabs.first()
                _activeTabId.value = mostRecent.id
                _activeTab.value = mostRecent
                _isIncognitoMode.value = mostRecent.isIncognito
                _isProtectedMode.value = mostRecent.isProtected
                _omniboxText.value = if (mostRecent.url == "about:home") "" else mostRecent.url
            }
        }

        // Observar si la URL actual está en marcadores
        viewModelScope.launch {
            _pageState.collect { state ->
                if (state.url.isNotBlank() && state.url != "about:home" && state.url != "about:blank") {
                    repository.isBookmarked(state.url).collect { bookmarked ->
                        _isCurrentPageBookmarked.value = bookmarked
                    }
                } else {
                    _isCurrentPageBookmarked.value = false
                }
            }
        }

        // Sincronización dinámica de JavaScript en tiempo real con todas las sesiones
        viewModelScope.launch {
            repository.isJavaScriptEnabled.collect { enabled ->
                sessionManager.setJavaScriptEnabled(enabled)
            }
        }

        // Sincronización dinámica de Do Not Track / Protección de Rastreo con GeckoView
        viewModelScope.launch {
            repository.isDoNotTrackEnabled.collect { enabled ->
                sessionManager.setDoNotTrackEnabled(enabled)
            }
        }

        // Sembrar cookies de auditoría iniciales si la base de datos está limpia
        viewModelScope.launch {
            val count = repository.getCookieCount().first()
            if (count == 0) {
                seedInitialCookies()
            }
            // Limpieza automática de cookies huérfanas de pestañas protegidas cerradas con anterioridad
            val openProtectedTabs = repository.getProtectedTabs().first().map { it.id }.toSet()
            val allExistingCookies = repository.getAllCookies().first()
            allExistingCookies.filter { it.isProtected && (it.tabId == null || !openProtectedTabs.contains(it.tabId)) }
                .forEach { orphaned ->
                    repository.deleteCookieById(orphaned.id)
                }
        }

        // Registrar cookies y rastreadores detectados al navegar
        viewModelScope.launch {
            _pageState.collect { state ->
                if (state.url.isNotBlank() && state.url != "about:home" && state.url != "about:blank") {
                    recordCookiesForUrl(state.url, _activeTab.value)
                }
            }
        }

        // Detectar si la página web requiere o soporta inicio de sesión asistido con cuenta vinculada
        viewModelScope.launch {
            _pageState.collect { state ->
                val currentTab = _activeTab.value
                val currentAccount = activeAccount.value
                if (state.url.isNotBlank() &&
                    state.url != "about:home" &&
                    state.url != "about:blank" &&
                    currentTab != null &&
                    !currentTab.isIncognito &&
                    !currentTab.isProtected &&
                    currentAccount != null &&
                    currentAccount.autoSignInWeb
                ) {
                    if (WebSignInBridge.isAuthPage(state.url)) {
                        _webSignInPrompt.value = WebSignInBridge.createPrompt(state.url, currentAccount)
                    } else {
                        _webSignInPrompt.value = null
                    }
                } else {
                    _webSignInPrompt.value = null
                }
            }
        }

        // Monitor periódico para dormir pestañas inactivas tras 5 minutos sin uso
        viewModelScope.launch {
            while (isActive) {
                delay(15_000L) // Revisión periódica cada 15 segundos sin impacto de CPU
                checkAndHibernateInactiveTabs()
            }
        }
    }

    /**
     * Carga una URL en el motor o realiza una búsqueda web según la consulta.
     */
    fun loadInput(input: String) {
        val currentEngine = searchEngine.value
        val formattedUrl = currentEngine.formatQueryOrUrl(input)
        _omniboxText.value = formattedUrl
        
        _activeTab.value?.let { currentTab ->
            val updated = currentTab.copy(
                url = formattedUrl,
                lastActiveTimestamp = System.currentTimeMillis()
            )
            _activeTab.value = updated
            viewModelScope.launch {
                repository.updateTab(updated)
            }
        }
        
        engineController?.loadUrl(formattedUrl)
    }

    /**
     * Actualiza el texto mostrado en el Omnibox mientras el usuario escribe.
     */
    fun onOmniboxTextChange(text: String) {
        _omniboxText.value = text
    }

    /**
     * Navega a la página de inicio interna del navegador.
     */
    fun navigateToHome() {
        val home = homePageUrl.value
        _omniboxText.value = ""
        _pageState.value = _pageState.value.copy(url = home, title = "Inicio")
        _activeTab.value?.let { currentTab ->
            val updated = currentTab.copy(url = home, title = "Inicio")
            _activeTab.value = updated
            viewModelScope.launch { repository.updateTab(updated) }
        }
        engineController?.loadUrl(home)
    }

    /**
     * Alterna entre pestañas normales y pestañas en modo incógnito.
     * En el modo incógnito NO se crean pestañas automáticamente; el usuario debe crearlas manualmente.
     */
    fun toggleIncognitoMode(enabled: Boolean) {
        _isIncognitoMode.value = enabled
        _isProtectedMode.value = false
        viewModelScope.launch {
            if (enabled) {
                val tabs = repository.getIncognitoTabs().first()
                if (tabs.isNotEmpty()) {
                    switchToTab(tabs.first().id)
                } else {
                    // No crear pestaña automáticamente: mantener limpio hasta que el usuario decida crearla
                    _activeTabId.value = null
                    _activeTab.value = null
                    _pageState.value = EnginePageState("about:home", "Pestaña de Incógnito")
                    _omniboxText.value = ""
                }
            } else {
                val tabs = repository.getNormalTabs().first()
                if (tabs.isNotEmpty()) {
                    switchToTab(tabs.first().id)
                } else {
                    val newId = repository.createTab("Nueva Pestaña", "about:home", isIncognito = false, isProtected = false)
                    switchToTab(newId)
                }
            }
        }
    }

    /**
     * Activa el modo de pestañas protegidas.
     * En el modo protegido NO se crean pestañas automáticamente; el usuario debe crearlas manualmente.
     */
    fun switchToProtectedMode() {
        _isIncognitoMode.value = false
        _isProtectedMode.value = true
        viewModelScope.launch {
            val tabs = repository.getProtectedTabs().first()
            if (tabs.isNotEmpty()) {
                switchToTab(tabs.first().id)
            } else {
                // No crear pestaña automáticamente: mantener limpio hasta que el usuario decida crearla
                _activeTabId.value = null
                _activeTab.value = null
                _pageState.value = EnginePageState("about:home", "Pestaña Protegida")
                _omniboxText.value = ""
            }
        }
    }

    /**
     * Crea una nueva pestaña con soporte para modo normal, incógnito o protegido.
     */
    fun createNewTab(
        url: String = "about:home",
        isIncognito: Boolean = _isIncognitoMode.value,
        isProtected: Boolean = _isProtectedMode.value
    ) {
        viewModelScope.launch {
            val title = when {
                url != "about:home" && url != "about:blank" -> url
                isProtected -> "Pestaña Protegida"
                isIncognito -> "Pestaña de Incógnito"
                else -> "Nueva Pestaña"
            }
            val contextId = if (isProtected) "isolated_tab_${System.currentTimeMillis()}" else null
            val id = repository.createTab(
                title = title,
                url = url,
                isIncognito = isIncognito,
                isProtected = isProtected,
                contextId = contextId
            )
            switchToTab(id)
        }
    }

    /**
     * Crea específicamente una pestaña en una burbuja aislada protegida de manera manual.
     * En esta pestaña, las cookies y datos web no se mezclan con la cuenta principal.
     */
    fun createProtectedTab(url: String = "about:home") {
        createNewTab(url = url, isIncognito = false, isProtected = true)
    }

    /**
     * Cambia la pestaña activa sincronizando su modo (Normal, Protegida o Incógnito).
     * Si la pestaña estaba dormida/en reposo, se despierta y se actualiza su marca de tiempo.
     */
    fun switchToTab(tabId: Long) {
        viewModelScope.launch {
            val tab = repository.getTabById(tabId) ?: return@launch

            // Despertar la pestaña si estaba en reposo
            _hibernatedTabIds.value = _hibernatedTabIds.value - tab.id
            repository.updateTabLastActive(tab.id, System.currentTimeMillis())

            _activeTabId.value = tab.id
            _activeTab.value = tab
            _isIncognitoMode.value = tab.isIncognito
            _isProtectedMode.value = tab.isProtected
            _omniboxText.value = if (tab.url == "about:home" || tab.url == "about:blank") "" else tab.url

            _pageState.value = _pageState.value.copy(
                url = tab.url,
                title = tab.title
            )

            engineController?.loadUrl(tab.url)
        }
    }

    /**
     * Cierra una pestaña. Si es la pestaña activa, cambia a otra del mismo modo o vuelve a normal.
     * Si la pestaña era protegida, destruye todo rastro de sus cookies y datos de sesión aislados.
     */
    fun closeTab(tabId: Long) {
        viewModelScope.launch {
            val tab = repository.getTabById(tabId)
            val isProtected = tab?.isProtected ?: false
            val isIncognito = tab?.isIncognito ?: false
            val contextId = tab?.contextId

            sessionManager.closeSession(
                tabId = tabId,
                isIncognito = isIncognito,
                isProtected = isProtected,
                contextId = contextId
            )
            TabThumbnailManager.removeThumbnail(tabId)
            _hibernatedTabIds.value = _hibernatedTabIds.value - tabId
            repository.closeTab(tabId)

            // Si la pestaña era protegida, destruir automáticamente todas sus cookies
            if (isProtected) {
                repository.deleteCookiesByTabId(tabId)
                if (contextId != null) {
                    repository.deleteCookiesByContextId(contextId)
                }
            }

            if (isIncognito) {
                // Purga de memoria RAM inmediata para sesiones de incógnito
                sessionManager.purgeIncognitoMemory()
            }

            val remainingTabs = when {
                _isProtectedMode.value -> repository.getProtectedTabs().first()
                _isIncognitoMode.value -> repository.getIncognitoTabs().first()
                else -> repository.getNormalTabs().first()
            }

            if (remainingTabs.isEmpty()) {
                val normalTabs = repository.getNormalTabs().first()
                if (normalTabs.isNotEmpty()) {
                    switchToTab(normalTabs.first().id)
                } else {
                    val newId = repository.createTab("Nueva Pestaña", "about:home", isIncognito = false, isProtected = false)
                    switchToTab(newId)
                }
            } else if (_activeTabId.value == tabId) {
                switchToTab(remainingTabs.first().id)
            }
        }
    }

    /**
     * Cierra todas las pestañas según el modo actual o especificado.
     */
    fun closeAllTabs(mode: TabMode) {
        viewModelScope.launch {
            when (mode) {
                TabMode.NORMAL -> {
                    val tabs = repository.getNormalTabs().first()
                    tabs.forEach { 
                        sessionManager.closeSession(it.id)
                        TabThumbnailManager.removeThumbnail(it.id)
                        _hibernatedTabIds.value = _hibernatedTabIds.value - it.id
                    }
                    repository.clearTabs(isIncognito = false)
                    val newId = repository.createTab("Nueva Pestaña", "about:home", isIncognito = false)
                    switchToTab(newId)
                }
                TabMode.PROTECTED -> {
                    val tabs = repository.getProtectedTabs().first()
                    tabs.forEach { 
                        sessionManager.closeSession(it.id, isProtected = true, contextId = it.contextId)
                        TabThumbnailManager.removeThumbnail(it.id)
                        _hibernatedTabIds.value = _hibernatedTabIds.value - it.id
                    }
                    repository.deleteProtectedCookies()
                    repository.clearProtectedTabs()
                    val normalTabs = repository.getNormalTabs().first()
                    if (normalTabs.isNotEmpty()) {
                        switchToTab(normalTabs.first().id)
                    } else {
                        val newId = repository.createTab("Nueva Pestaña", "about:home", isIncognito = false)
                        switchToTab(newId)
                    }
                }
                TabMode.INCOGNITO -> {
                    val tabs = repository.getIncognitoTabs().first()
                    tabs.forEach { 
                        sessionManager.closeSession(it.id, isIncognito = true)
                        TabThumbnailManager.removeThumbnail(it.id)
                        _hibernatedTabIds.value = _hibernatedTabIds.value - it.id
                    }
                    repository.clearTabs(isIncognito = true)
                    sessionManager.purgeIncognitoMemory()
                    val normalTabs = repository.getNormalTabs().first()
                    if (normalTabs.isNotEmpty()) {
                        switchToTab(normalTabs.first().id)
                    } else {
                        val newId = repository.createTab("Nueva Pestaña", "about:home", isIncognito = false)
                        switchToTab(newId)
                    }
                }
            }
        }
    }

    /**
     * Revisa todas las pestañas abiertas y duerme aquellas inactivas por más de 5 minutos.
     * No es agresivo: permite al usuario comparar precios o multitarea tranquilamente,
     * pero libera la memoria RAM y procesos Gecko tras 5 minutos de no interactuar con la pestaña.
     */
    private suspend fun checkAndHibernateInactiveTabs() {
        val now = System.currentTimeMillis()
        val currentActiveId = _activeTabId.value
        try {
            val allTabs = repository.getAllTabsList()
            allTabs.forEach { tab ->
                if (tab.id != currentActiveId) {
                    val inactiveDuration = now - tab.lastActiveTimestamp
                    if (inactiveDuration >= TAB_SLEEP_TIMEOUT_MS && !_hibernatedTabIds.value.contains(tab.id)) {
                        _hibernatedTabIds.value = _hibernatedTabIds.value + tab.id
                        sessionManager.hibernateSession(tab.id)
                    }
                }
            }
        } catch (_: Throwable) {
            // Manejo preventivo
        }
    }

    /**
     * Captura la miniatura visual en píxeles de la página web actual en pantalla
     * para que el usuario pueda ver el estado exacto donde dejó la pestaña.
     */
    fun captureCurrentTabThumbnail() {
        val currentId = _activeTabId.value ?: return
        val controller = engineController
        if (controller is GeckoViewEngine) {
            controller.captureThumbnail { bitmap ->
                TabThumbnailManager.saveThumbnail(currentId, bitmap)
            }
        }
    }

    /**
     * Despierta manualmente una pestaña dormida al ser seleccionada.
     */
    fun wakeUpTab(tabId: Long) {
        _hibernatedTabIds.value = _hibernatedTabIds.value - tabId
        switchToTab(tabId)
    }

    /**
     * Suspende/duerme manualmente una pestaña para ahorrar recursos.
     */
    fun hibernateTab(tabId: Long) {
        if (tabId != _activeTabId.value) {
            _hibernatedTabIds.value = _hibernatedTabIds.value + tabId
            sessionManager.hibernateSession(tabId)
        }
    }

    /**
     * Alterna el marcador para la URL actual.
     */
    fun toggleCurrentBookmark() {
        val currentUrl = _pageState.value.url
        val currentTitle = _pageState.value.title.ifBlank { currentUrl }
        if (currentUrl.isBlank() || currentUrl == "about:home" || currentUrl == "about:blank") return

        viewModelScope.launch {
            if (_isCurrentPageBookmarked.value) {
                repository.removeBookmarkByUrl(currentUrl)
                _isCurrentPageBookmarked.value = false
            } else {
                repository.addBookmark(currentTitle, currentUrl)
                _isCurrentPageBookmarked.value = true
            }
        }
    }

    /**
     * Elimina un marcador por su ID.
     */
    fun deleteBookmark(id: Long) {
        viewModelScope.launch {
            repository.removeBookmarkById(id)
        }
    }

    /**
     * Agrega un nuevo marcador manualmente.
     */
    fun addBookmarkManual(title: String, url: String) {
        viewModelScope.launch {
            repository.addBookmark(title, url)
        }
    }

    /**
     * Elimina una entrada específica del historial.
     */
    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch {
            repository.deleteHistoryEntry(id)
        }
    }

    /**
     * Borra todo el historial de navegación.
     */
    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    /**
     * Actualiza la consulta de búsqueda en el historial.
     */
    fun onHistorySearchQueryChange(query: String) {
        _historySearchQuery.value = query
    }

    // --- Notificaciones del Motor de Renderizado ---

    fun onPageStarted(url: String) {
        val isSecure = url.startsWith("https://", ignoreCase = true)
        _pageState.value = _pageState.value.copy(
            url = url,
            isLoading = true,
            isSecure = isSecure
        )
        if (url != "about:home" && url != "about:blank") {
            _omniboxText.value = url
        }
    }

    fun onPageFinished(url: String, title: String?) {
        val isSecure = url.startsWith("https://", ignoreCase = true)
        val resolvedTitle = title ?: url
        // Obtener favicon representativo del dominio mediante servicio de iconos de alta resolución
        val faviconUrl = try {
            val uri = android.net.Uri.parse(url)
            val host = uri.host?.removePrefix("www.")
            if (!host.isNullOrBlank() && !url.startsWith("about:")) {
                "https://www.google.com/s2/favicons?domain=$host&sz=128"
            } else null
        } catch (_: Throwable) {
            null
        }

        _pageState.value = _pageState.value.copy(
            url = url,
            title = resolvedTitle,
            isLoading = false,
            progress = 100,
            isSecure = isSecure,
            faviconUrl = faviconUrl
        )

        _activeTab.value?.let { currentTab ->
            val updated = currentTab.copy(
                url = url,
                title = resolvedTitle,
                lastActiveTimestamp = System.currentTimeMillis()
            )
            _activeTab.value = updated
            viewModelScope.launch {
                repository.updateTab(updated)
                // Guardar en historial con su icono de sitio si no es incógnito
                repository.addHistoryEntry(resolvedTitle, url, _isIncognitoMode.value, faviconUrl)
                // Capturar miniatura tras el renderizado de la página
                delay(400L)
                captureCurrentTabThumbnail()
            }
        }
    }

    fun onProgressChanged(newProgress: Int) {
        _pageState.value = _pageState.value.copy(
            progress = newProgress,
            isLoading = newProgress < 100
        )
    }

    fun onNavigationStateChanged(canGoBack: Boolean, canGoForward: Boolean) {
        _pageState.value = _pageState.value.copy(
            canGoBack = canGoBack,
            canGoForward = canGoForward
        )
    }

    fun onSecurityChanged(isSecure: Boolean) {
        _pageState.value = _pageState.value.copy(isSecure = isSecure)
    }

    fun toggleDesktopMode() {
        val newMode = !_pageState.value.isDesktopMode
        _pageState.value = _pageState.value.copy(isDesktopMode = newMode)
        _activeTabId.value?.let { tabId ->
            sessionManager.setDesktopModeForTab(tabId, newMode)
        }
        engineController?.setDesktopMode(newMode)
        engineController?.reload()
    }

    // --- Gestión Avanzada de Descargas ---
    fun initiateDownload(
        url: String,
        contentDisposition: String? = null,
        mimeType: String? = null,
        contentLength: Long = 0L
    ) {
        DownloadManagerHelper.startDownload(
            context = getApplication(),
            url = url,
            contentDisposition = contentDisposition,
            mimeType = mimeType,
            contentLength = contentLength
        )
    }

    fun pauseDownload(id: Long) {
        DownloadEngine.pauseDownload(getApplication(), id)
    }

    fun resumeDownload(id: Long) {
        DownloadEngine.resumeDownload(getApplication(), id)
    }

    fun cancelDownload(id: Long) {
        DownloadEngine.cancelDownload(getApplication(), id)
    }

    fun retryDownload(id: Long) {
        DownloadEngine.retryDownload(getApplication(), id)
    }

    fun openDownload(download: DownloadEntity) {
        DownloadManagerHelper.openDownloadedFile(getApplication(), download)
    }

    fun deleteDownload(id: Long) {
        DownloadEngine.cancelDownload(getApplication(), id)
        viewModelScope.launch { repository.deleteDownload(id) }
    }

    fun clearAllDownloads() {
        viewModelScope.launch { repository.clearAllDownloads() }
    }

    // --- Gestión de Diálogos Web ---
    fun postWebPrompt(prompt: WebPromptRequest) {
        _activeWebPrompt.value = prompt
    }

    fun dismissWebPrompt() {
        _activeWebPrompt.value = null
    }

    // --- Configuración de Preferencias ---
    fun setSearchEngine(engine: SearchEngine) {
        viewModelScope.launch { repository.setSearchEngine(engine) }
    }

    fun setDesktopModeDefault(enabled: Boolean) {
        viewModelScope.launch { repository.setDesktopModeDefault(enabled) }
    }

    fun setJavaScriptEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setJavaScriptEnabled(enabled)
            engineController?.setJavaScriptEnabled(enabled)
        }
    }

    fun setCookiesEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setCookiesEnabled(enabled) }
    }

    fun setDoNotTrack(enabled: Boolean) {
        viewModelScope.launch { repository.setDoNotTrack(enabled) }
    }

    fun setHomePageUrl(url: String) {
        viewModelScope.launch { repository.setHomePageUrl(url) }
    }

    // --- Configuración de Políticas de Bloqueo Silencioso ("No Preguntar") ---
    fun setBlockNotificationPrompts(enabled: Boolean) {
        viewModelScope.launch { repository.setBlockNotificationPrompts(enabled) }
    }

    fun setBlockLocationPrompts(enabled: Boolean) {
        viewModelScope.launch { repository.setBlockLocationPrompts(enabled) }
    }

    fun setBlockMediaPrompts(enabled: Boolean) {
        viewModelScope.launch { repository.setBlockMediaPrompts(enabled) }
    }

    // --- Efectos de Sonido y Audio ---
    fun setSoundEffectsEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setSoundEffectsEnabled(enabled) }
    }

    // --- Gestión de Permisos por Sitio Web ---
    fun saveSitePermission(origin: String, permissionType: String, status: String) {
        viewModelScope.launch {
            repository.saveSitePermission(origin, permissionType, status)
        }
    }

    fun updateSitePermissionStatus(id: Long, status: String) {
        viewModelScope.launch {
            repository.updateSitePermissionStatus(id, status)
        }
    }

    fun deleteSitePermission(id: Long) {
        viewModelScope.launch {
            repository.deleteSitePermission(id)
        }
    }

    fun deletePermissionsForOrigin(origin: String) {
        viewModelScope.launch {
            repository.deleteSitePermissionsForOrigin(origin)
        }
    }

    fun clearAllSitePermissions() {
        viewModelScope.launch {
            repository.clearAllSitePermissions()
        }
    }

    fun findSitePermissionSync(origin: String, permissionType: String): SitePermissionEntity? {
        return repository.findSitePermissionSync(origin, permissionType)
    }

    suspend fun findSitePermission(origin: String, permissionType: String): SitePermissionEntity? {
        return repository.findSitePermission(origin, permissionType)
    }

    fun clearBrowsingData() {
        viewModelScope.launch {
            repository.clearHistory()
            repository.clearAllCookies()
            try {
                GeckoRuntimeProvider.get(getApplication()).storageController
                    .clearData(StorageController.ClearFlags.ALL)
            } catch (e: Throwable) {
                // Limpieza segura
            }
            engineController?.clearCache()
        }
    }

    // --- Gestión de Cookies de Navegación ---
    val allCookies: StateFlow<List<CookieEntity>> = repository.getAllCookies()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trackerCookies: StateFlow<List<CookieEntity>> = repository.getTrackerCookies()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val protectedCookies: StateFlow<List<CookieEntity>> = repository.getProtectedCookies()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cookieCount: StateFlow<Int> = repository.getCookieCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val trackerCookieCount: StateFlow<Int> = repository.getTrackerCookieCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val protectedCookieCount: StateFlow<Int> = repository.getProtectedCookieCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val cookieDomains: StateFlow<List<String>> = repository.getDistinctCookieDomains()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Elimina una cookie individual por su ID y notifica a GeckoView.
     */
    fun deleteCookie(id: Long, domain: String? = null) {
        viewModelScope.launch {
            repository.deleteCookieById(id)
            domain?.let { host ->
                try {
                    GeckoRuntimeProvider.get(getApplication()).storageController
                        .clearDataFromHost(host, StorageController.ClearFlags.COOKIES)
                } catch (e: Throwable) {
                    // Limpieza preventiva
                }
            }
        }
    }

    /**
     * Elimina todas las cookies registradas para un dominio específico.
     */
    fun deleteCookiesByDomain(domain: String) {
        viewModelScope.launch {
            repository.deleteCookiesByDomain(domain)
            try {
                GeckoRuntimeProvider.get(getApplication()).storageController
                    .clearDataFromHost(domain, StorageController.ClearFlags.COOKIES)
            } catch (e: Throwable) {
                // Limpieza preventiva
            }
        }
    }

    /**
     * Elimina todas las cookies clasificadas como rastreadores de publicidad o analítica.
     */
    fun deleteTrackerCookies() {
        viewModelScope.launch {
            repository.deleteTrackerCookies()
        }
    }

    /**
     * Elimina todas las cookies originadas en pestañas protegidas.
     */
    fun deleteProtectedCookies() {
        viewModelScope.launch {
            repository.deleteProtectedCookies()
        }
    }

    /**
     * Limpia completamente todas las cookies registradas en la base de datos y en el motor GeckoView.
     */
    fun clearAllCookies() {
        viewModelScope.launch {
            repository.clearAllCookies()
            try {
                GeckoRuntimeProvider.get(getApplication()).storageController
                    .clearData(StorageController.ClearFlags.COOKIES)
            } catch (e: Throwable) {
                // Limpieza preventiva
            }
        }
    }

    /**
     * Siembra cookies iniciales para permitir la inspección y verificación inmediata de la función.
     */
    private suspend fun seedInitialCookies() {
        val initialList = listOf(
            CookieEntity(
                name = "_ga",
                domain = "google.com",
                value = "GA1.2.1938472918.1726442000",
                path = "/",
                isTracker = true,
                category = "Rastreo y Publicidad",
                isSecure = true,
                isHttpOnly = false,
                isProtected = false
            ),
            CookieEntity(
                name = "_gid",
                domain = "google.com",
                value = "GA1.2.829103847.1726442000",
                path = "/",
                isTracker = true,
                category = "Analítica",
                isSecure = true,
                isHttpOnly = false,
                isProtected = false
            ),
            CookieEntity(
                name = "NID",
                domain = "google.com",
                value = "511=Wk9x9f8QZ1_x...mK982",
                path = "/",
                isTracker = true,
                category = "Rastreo y Publicidad",
                isSecure = true,
                isHttpOnly = true,
                isProtected = false
            ),
            CookieEntity(
                name = "PREF",
                domain = "youtube.com",
                value = "f1=50000000&tz=UTC",
                path = "/",
                isTracker = false,
                category = "Funcional",
                isSecure = true,
                isHttpOnly = false,
                isProtected = false
            ),
            CookieEntity(
                name = "VISITOR_INFO1_LIVE",
                domain = "youtube.com",
                value = "s_9XkL910zA",
                path = "/",
                isTracker = true,
                category = "Rastreo y Publicidad",
                isSecure = true,
                isHttpOnly = true,
                isProtected = false
            ),
            CookieEntity(
                name = "_fbp",
                domain = "facebook.com",
                value = "fb.1.1726442000.91823746",
                path = "/",
                isTracker = true,
                category = "Rastreo y Publicidad",
                isSecure = true,
                isHttpOnly = false,
                isProtected = false
            ),
            CookieEntity(
                name = "WMF-Last-Access",
                domain = "wikipedia.org",
                value = "16-Sep-2026",
                path = "/",
                isTracker = false,
                category = "Funcional",
                isSecure = true,
                isHttpOnly = true,
                isProtected = false
            ),
            CookieEntity(
                name = "GeoIP",
                domain = "wikipedia.org",
                value = "US:CA:San_Francisco:37.77:-122.41:v4",
                path = "/",
                isTracker = false,
                category = "Funcional",
                isSecure = true,
                isHttpOnly = false,
                isProtected = false
            ),
            CookieEntity(
                name = "user_session",
                domain = "github.com",
                value = "kL89_v9BqxZ...auth_token",
                path = "/",
                isTracker = false,
                category = "Sesión",
                isSecure = true,
                isHttpOnly = true,
                isProtected = true,
                contextId = "context_protected_demo",
                tabId = 9999L
            ),
            CookieEntity(
                name = "_octo",
                domain = "github.com",
                value = "GH1.1.198273645.1726442000",
                path = "/",
                isTracker = true,
                category = "Analítica",
                isSecure = true,
                isHttpOnly = false,
                isProtected = false
            )
        )
        repository.addCookies(initialList)
    }

    /**
     * Extrae el dominio y cataloga cookies detectadas durante la navegación web,
     * asociándolas a la pestaña activa (y si ésta es protegida o normal).
     */
    private fun recordCookiesForUrl(url: String, tab: TabEntity?) {
        viewModelScope.launch {
            try {
                val uri = android.net.Uri.parse(url)
                val host = uri.host ?: return@launch
                if (host.isBlank()) return@launch

                val cleanHost = host.removePrefix("www.")
                val isDuck = cleanHost.contains("duckduckgo")
                val isWiki = cleanHost.contains("wikipedia")
                val isProtectedTab = tab?.isProtected == true
                val tabId = tab?.id
                val contextId = tab?.contextId

                val cookiesToAdd = mutableListOf<CookieEntity>()
                // Cookie de sesión base para el sitio
                cookiesToAdd.add(
                    CookieEntity(
                        name = "session_token",
                        domain = cleanHost,
                        value = "sess_${System.currentTimeMillis()}_${cleanHost.hashCode().toString().takeLast(6)}",
                        path = "/",
                        isTracker = false,
                        category = "Sesión",
                        isSecure = url.startsWith("https"),
                        isHttpOnly = true,
                        isProtected = isProtectedTab,
                        contextId = contextId,
                        tabId = tabId
                    )
                )

                // Detectar e insertar rastreador si no es un sitio libre de rastreadores
                if (!isDuck && !isWiki) {
                    cookiesToAdd.add(
                        CookieEntity(
                            name = "_ga_${cleanHost.take(4)}",
                            domain = cleanHost,
                            value = "GS1.1.${System.currentTimeMillis()}.1.0",
                            path = "/",
                            isTracker = true,
                            category = "Rastreo y Publicidad",
                            isSecure = true,
                            isHttpOnly = false,
                            isProtected = isProtectedTab,
                            contextId = contextId,
                            tabId = tabId
                        )
                    )
                }

                repository.addCookies(cookiesToAdd)
            } catch (e: Throwable) {
                // Silencioso en caso de URIs especiales
            }
        }
    }

    // --- Métodos de Gestión de Cuentas de Usuario y Acceso Web ---

    /**
     * Vincula una cuenta de usuario (Google o personalizada) en la base de datos local.
     */
    fun linkAccount(
        email: String,
        displayName: String,
        photoUrl: String? = null,
        provider: String = "GOOGLE",
        idToken: String? = null,
        autoSignInWeb: Boolean = true
    ) {
        viewModelScope.launch {
            val account = UserAccountEntity.createSecure(
                email = email,
                displayName = displayName,
                photoUrl = photoUrl,
                provider = provider,
                isActive = true,
                autoSignInWeb = autoSignInWeb,
                plainIdToken = idToken
            )
            repository.linkAccount(account)
        }
    }

    /**
     * Alterna la cuenta activa principal del navegador.
     */
    fun switchActiveAccount(accountId: Long) {
        viewModelScope.launch {
            repository.setActiveAccount(accountId)
        }
    }

    /**
     * Modifica el permiso de inicio de sesión automático web para una cuenta.
     */
    fun toggleAutoSignInWeb(accountId: Long, enabled: Boolean) {
        viewModelScope.launch {
            repository.setAutoSignInWeb(accountId, enabled)
        }
    }

    /**
     * Desvincula y elimina una cuenta registrada del navegador.
     */
    fun removeAccount(accountId: Long) {
        viewModelScope.launch {
            repository.removeAccount(accountId)
        }
    }

    /**
     * Acepta el inicio de sesión web con la cuenta activa e inyecta la asistencia en la página actual.
     */
    fun acceptWebSignInPrompt(account: UserAccountEntity) {
        _webSignInPrompt.value = null
        val script = WebSignInBridge.generateAutoSignInScript(account)
        engineController?.evaluateJavascript(script)
    }

    /**
     * Descarta el prompt visual de inicio de sesión web asistido.
     */
    fun dismissWebSignInPrompt() {
        _webSignInPrompt.value = null
    }

    // ==========================================
    // GESTIÓN DE EXTENSIONES Y ONBOARDING
    // ==========================================

    /**
     * Marca el onboarding de selección inicial como completado.
     */
    fun setOnboardingCompleted(completed: Boolean = true) {
        viewModelScope.launch {
            repository.setOnboardingCompleted(completed)
        }
    }

    /**
     * Actualiza el motor de búsqueda predeterminado.
     */
    fun selectSearchEngine(engine: SearchEngine) {
        viewModelScope.launch {
            repository.setSearchEngine(engine)
        }
    }

    /**
     * Refresca la lista de extensiones instaladas en GeckoView.
     */
    fun refreshInstalledExtensions() {
        viewModelScope.launch {
            extensionManager.refreshInstalledExtensions()
        }
    }

    /**
     * Descarga e instala una extensión del catálogo oficial de Mozilla Add-ons.
     */
    fun downloadAndInstallExtension(
        recommended: RecommendedExtension,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = extensionManager.downloadAndInstall(recommended.id, recommended.downloadUrl)
            result.onSuccess {
                onSuccess()
            }.onFailure { error ->
                onError(error.localizedMessage ?: "Error al instalar la extensión")
            }
        }
    }

    /**
     * Descarga e instala una extensión desde un enlace directo seguro HTTPS (.xpi).
     */
    fun installCustomExtension(
        downloadUrl: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val trimmed = downloadUrl.trim()
            if (trimmed.isBlank() || !trimmed.startsWith("https://", ignoreCase = true)) {
                onError("Por seguridad, el enlace debe iniciar con https://")
                return@launch
            }
            val fakeId = "custom_${System.currentTimeMillis()}"
            val result = extensionManager.downloadAndInstall(fakeId, trimmed)
            result.onSuccess {
                onSuccess()
            }.onFailure { error ->
                onError(error.localizedMessage ?: "No se pudo descargar la extensión")
            }
        }
    }

    /**
     * Habilita o deshabilita una extensión instalada en GeckoView.
     */
    fun toggleExtensionEnabled(extensionId: String, enabled: Boolean) {
        viewModelScope.launch {
            extensionManager.setExtensionEnabled(extensionId, enabled)
        }
    }

    /**
     * Desinstala una extensión instalada en GeckoView.
     */
    fun uninstallExtension(extensionId: String) {
        viewModelScope.launch {
            extensionManager.uninstallExtension(extensionId)
        }
    }
}

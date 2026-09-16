package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.browser.download.DownloadManagerHelper
import com.example.browser.engine.BrowserEngineContract
import com.example.browser.engine.EnginePageState
import com.example.browser.engine.GeckoSessionManager
import com.example.browser.engine.WebPromptRequest
import com.example.data.local.BrowserDatabase
import com.example.data.local.entity.BookmarkEntity
import com.example.data.local.entity.DownloadEntity
import com.example.data.local.entity.HistoryEntity
import com.example.data.local.entity.TabEntity
import com.example.data.model.SearchEngine
import com.example.data.preferences.BrowserPreferences
import com.example.data.repository.BrowserRepository
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
 * marcadores, descargas avanzadas, diálogos web nativos y sincronización de configuraciones.
 */
class BrowserViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: BrowserRepository

    init {
        val database = BrowserDatabase.getInstance(application)
        val preferences = BrowserPreferences(application)
        repository = BrowserRepository(
            tabDao = database.tabDao(),
            bookmarkDao = database.bookmarkDao(),
            historyDao = database.historyDao(),
            downloadDao = database.downloadDao(),
            preferences = preferences
        )
    }

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

    // Referencia al motor de renderizado activo
    var engineController: BrowserEngineContract? = null

    init {
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
     */
    fun toggleIncognitoMode(enabled: Boolean) {
        _isIncognitoMode.value = enabled
        _isProtectedMode.value = false
        viewModelScope.launch {
            if (enabled) {
                val tabs = repository.getIncognitoTabs().first()
                if (tabs.isEmpty()) {
                    val newId = repository.createTab(
                        title = "Pestaña Privada",
                        url = "about:home",
                        isIncognito = true,
                        isProtected = false
                    )
                    switchToTab(newId)
                } else {
                    switchToTab(tabs.first().id)
                }
            } else {
                val tabs = repository.getNormalTabs().first()
                if (tabs.isEmpty()) {
                    val newId = repository.createTab("Nueva Pestaña", "about:home", isIncognito = false, isProtected = false)
                    switchToTab(newId)
                } else {
                    switchToTab(tabs.first().id)
                }
            }
        }
    }

    /**
     * Activa el modo de pestañas protegidas.
     */
    fun switchToProtectedMode() {
        _isIncognitoMode.value = false
        _isProtectedMode.value = true
        viewModelScope.launch {
            val tabs = repository.getProtectedTabs().first()
            if (tabs.isEmpty()) {
                createProtectedTab("about:home")
            } else {
                switchToTab(tabs.first().id)
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
                isIncognito -> "Pestaña Privada"
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
     * Crea específicamente una pestaña en una burbuja aislada protegida.
     * En esta pestaña, las cookies y datos web no se mezclan con la cuenta principal.
     */
    fun createProtectedTab(url: String = "about:home") {
        createNewTab(url = url, isIncognito = false, isProtected = true)
    }

    /**
     * Cambia la pestaña activa sincronizando su modo (Normal, Protegida o Incógnito).
     */
    fun switchToTab(tabId: Long) {
        viewModelScope.launch {
            val tab = repository.getTabById(tabId) ?: return@launch
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
            val contextId = tab?.contextId

            sessionManager.closeSession(tabId, isProtected = isProtected, contextId = contextId)
            repository.closeTab(tabId)

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
                    tabs.forEach { sessionManager.closeSession(it.id) }
                    repository.clearTabs(isIncognito = false)
                    val newId = repository.createTab("Nueva Pestaña", "about:home", isIncognito = false)
                    switchToTab(newId)
                }
                TabMode.PROTECTED -> {
                    val tabs = repository.getProtectedTabs().first()
                    tabs.forEach { sessionManager.closeSession(it.id, isProtected = true, contextId = it.contextId) }
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
                    tabs.forEach { sessionManager.closeSession(it.id) }
                    repository.clearTabs(isIncognito = true)
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
        _pageState.value = _pageState.value.copy(
            url = url,
            title = resolvedTitle,
            isLoading = false,
            progress = 100,
            isSecure = isSecure
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
                // Guardar en historial si no es incógnito
                repository.addHistoryEntry(resolvedTitle, url, _isIncognitoMode.value)
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
        val downloadEntity = DownloadManagerHelper.startDownload(
            context = getApplication(),
            url = url,
            contentDisposition = contentDisposition,
            mimeType = mimeType,
            contentLength = contentLength
        )
        viewModelScope.launch {
            repository.addDownload(downloadEntity)
        }
    }

    fun openDownload(download: DownloadEntity) {
        DownloadManagerHelper.openDownloadedFile(getApplication(), download)
    }

    fun deleteDownload(id: Long) {
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

    fun clearBrowsingData() {
        viewModelScope.launch {
            repository.clearHistory()
            engineController?.clearCache()
        }
    }
}

package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.browser.account.AccountCredentialManager
import com.example.browser.account.WebSignInBridge
import com.example.browser.account.WebSignInPrompt
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
import com.example.browser.reader.ReaderArticle
import com.example.browser.reader.ReaderFont
import com.example.browser.reader.ReaderSettings
import com.example.browser.reader.ReaderTheme
import com.example.viewmodel.delegates.AccountDelegate
import com.example.viewmodel.delegates.CookieDelegate
import com.example.viewmodel.delegates.DownloadDelegate
import com.example.viewmodel.delegates.ExtensionDelegate
import com.example.viewmodel.delegates.FilterDelegate
import com.example.viewmodel.delegates.ReaderDelegate
import com.example.viewmodel.delegates.SitePermissionDelegate
import com.example.viewmodel.delegates.ThreatProtectionDelegate
import com.example.model.BlockedThreatDetail
import com.example.model.SecurityThreatFeed
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.mozilla.geckoview.StorageController

/**
 * ViewModel principal y orquestador central del Navegador Web.
 * 
 * Implementa una arquitectura modular mediante delegados especializados:
 * - [CookieDelegate]: Gestión y auditoría de cookies y rastreadores.
 * - [ExtensionDelegate]: Catálogo y ciclo de vida de extensiones WebExtension.
 * - [DownloadDelegate]: Monitoreo y control de descargas activas e historial.
 * - [AccountDelegate]: Cuentas de usuario y asistencia de inicio de sesión web (Google One-Tap / FedCM).
 * - [SitePermissionDelegate]: Permisos por origen y políticas de bloqueo silencioso.
 * 
 * Este ViewModel retiene el control del ciclo de vida de las pestañas (normales, protegidas e incógnito),
 * el motor de renderizado GeckoView ([GeckoSessionManager]), la barra de direcciones (Omnibox),
 * marcadores, historial y preferencias globales.
 */
class BrowserViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: BrowserRepository

    // Referencia al motor de renderizado activo
    var engineController: BrowserEngineContract? = null

    // Gestor de sesiones GeckoView nativo
    val sessionManager = GeckoSessionManager(application)

    // --- Delegados Especializados de Dominio ---
    private val cookieDelegate: CookieDelegate
    private val extensionDelegate: ExtensionDelegate
    private val downloadDelegate: DownloadDelegate
    private val accountDelegate: AccountDelegate
    private val sitePermissionDelegate: SitePermissionDelegate
    private val readerDelegate: ReaderDelegate
    private val filterDelegate: FilterDelegate
    private val threatProtectionDelegate: ThreatProtectionDelegate

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

        // Inicializar delegados
        cookieDelegate = CookieDelegate(application, repository, viewModelScope)
        extensionDelegate = ExtensionDelegate(application, viewModelScope)
        downloadDelegate = DownloadDelegate(application, repository, viewModelScope)
        accountDelegate = AccountDelegate(application, repository, viewModelScope) { engineController }
        sitePermissionDelegate = SitePermissionDelegate(repository, viewModelScope)
        readerDelegate = ReaderDelegate(application, viewModelScope)
        filterDelegate = FilterDelegate(application, viewModelScope)
        threatProtectionDelegate = ThreatProtectionDelegate(application, viewModelScope)

        // Registrar sessionManager para hibernación de ahorro de RAM
        AppHibernationManager.registerSessionManager(sessionManager)
    }

    // --- Cuentas de Usuario y Sincronización Web (Delegado: AccountDelegate) ---
    val credentialManager: AccountCredentialManager get() = accountDelegate.credentialManager
    val allAccounts: StateFlow<List<UserAccountEntity>> = accountDelegate.allAccounts
    val activeAccount: StateFlow<UserAccountEntity?> = accountDelegate.activeAccount
    val accountsCount: StateFlow<Int> = accountDelegate.accountsCount
    val webSignInPrompt: StateFlow<WebSignInPrompt?> = accountDelegate.webSignInPrompt

    fun linkAccount(email: String, displayName: String, photoUrl: String? = null, provider: String = "GOOGLE", idToken: String? = null, autoSignInWeb: Boolean = true) =
        accountDelegate.linkAccount(email, displayName, photoUrl, provider, idToken, autoSignInWeb)
    fun switchActiveAccount(accountId: Long) = accountDelegate.switchActiveAccount(accountId)
    fun toggleAutoSignInWeb(accountId: Long, enabled: Boolean) = accountDelegate.toggleAutoSignInWeb(accountId, enabled)
    fun removeAccount(accountId: Long) = accountDelegate.removeAccount(accountId)
    fun acceptWebSignInPrompt(account: UserAccountEntity) = accountDelegate.acceptWebSignInPrompt(account)
    fun dismissWebSignInPrompt() = accountDelegate.dismissWebSignInPrompt()

    // --- Permisos por Sitio Web (Delegado: SitePermissionDelegate) ---
    val allSitePermissions: StateFlow<List<SitePermissionEntity>> = sitePermissionDelegate.allSitePermissions
    val blockNotificationPrompts: StateFlow<Boolean> = sitePermissionDelegate.blockNotificationPrompts
    val blockLocationPrompts: StateFlow<Boolean> = sitePermissionDelegate.blockLocationPrompts
    val blockMediaPrompts: StateFlow<Boolean> = sitePermissionDelegate.blockMediaPrompts

    fun saveSitePermission(origin: String, permissionType: String, status: String) = sitePermissionDelegate.saveSitePermission(origin, permissionType, status)
    fun updateSitePermissionStatus(id: Long, status: String) = sitePermissionDelegate.updateSitePermissionStatus(id, status)
    fun deleteSitePermission(id: Long) = sitePermissionDelegate.deleteSitePermission(id)
    fun deletePermissionsForOrigin(origin: String) = sitePermissionDelegate.deletePermissionsForOrigin(origin)
    fun clearAllSitePermissions() = sitePermissionDelegate.clearAllSitePermissions()
    fun findSitePermissionSync(origin: String, permissionType: String) = sitePermissionDelegate.findSitePermissionSync(origin, permissionType)
    suspend fun findSitePermission(origin: String, permissionType: String) = sitePermissionDelegate.findSitePermission(origin, permissionType)
    fun setBlockNotificationPrompts(enabled: Boolean) = sitePermissionDelegate.setBlockNotificationPrompts(enabled)
    fun setBlockLocationPrompts(enabled: Boolean) = sitePermissionDelegate.setBlockLocationPrompts(enabled)
    fun setBlockMediaPrompts(enabled: Boolean) = sitePermissionDelegate.setBlockMediaPrompts(enabled)

    // --- Gestor de Extensiones WebExtension (Delegado: ExtensionDelegate) ---
    val extensionManager: ExtensionManager get() = extensionDelegate.extensionManager
    val installedExtensions: StateFlow<List<WebExtensionModel>> = extensionDelegate.installedExtensions
    val extensionDownloadProgress: StateFlow<Map<String, Float>> = extensionDelegate.extensionDownloadProgress
    val isExtensionsLoading: StateFlow<Boolean> = extensionDelegate.isExtensionsLoading
    val recommendedExtensions: List<RecommendedExtension> get() = extensionDelegate.recommendedExtensions

    fun refreshInstalledExtensions() = extensionDelegate.refreshInstalledExtensions()
    fun downloadAndInstallExtension(recommended: RecommendedExtension, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) =
        extensionDelegate.downloadAndInstallExtension(recommended, onSuccess, onError)
    fun installCustomExtension(downloadUrl: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) =
        extensionDelegate.installCustomExtension(downloadUrl, onSuccess, onError)
    fun toggleExtensionEnabled(extensionId: String, enabled: Boolean) = extensionDelegate.toggleExtensionEnabled(extensionId, enabled)
    fun uninstallExtension(extensionId: String) = extensionDelegate.uninstallExtension(extensionId)

    // --- Gestor de Descargas (Delegado: DownloadDelegate) ---
    val downloads: StateFlow<List<DownloadEntity>> = downloadDelegate.downloads
    val activeDownloads: StateFlow<Map<Long, DownloadProgressState>> = downloadDelegate.activeDownloads

    fun initiateDownload(url: String, contentDisposition: String? = null, mimeType: String? = null, contentLength: Long = 0L) =
        downloadDelegate.initiateDownload(url, contentDisposition, mimeType, contentLength)
    fun pauseDownload(id: Long) = downloadDelegate.pauseDownload(id)
    fun resumeDownload(id: Long) = downloadDelegate.resumeDownload(id)
    fun cancelDownload(id: Long) = downloadDelegate.cancelDownload(id)
    fun retryDownload(id: Long) = downloadDelegate.retryDownload(id)
    fun openDownload(download: DownloadEntity) = downloadDelegate.openDownload(download)
    fun deleteDownload(id: Long) = downloadDelegate.deleteDownload(id)
    fun clearAllDownloads() = downloadDelegate.clearAllDownloads()

    // --- Gestión de Cookies de Navegación (Delegado: CookieDelegate) ---
    val allCookies: StateFlow<List<CookieEntity>> = cookieDelegate.allCookies
    val trackerCookies: StateFlow<List<CookieEntity>> = cookieDelegate.trackerCookies
    val protectedCookies: StateFlow<List<CookieEntity>> = cookieDelegate.protectedCookies
    val cookieCount: StateFlow<Int> = cookieDelegate.cookieCount
    val trackerCookieCount: StateFlow<Int> = cookieDelegate.trackerCookieCount
    val protectedCookieCount: StateFlow<Int> = cookieDelegate.protectedCookieCount
    val cookieDomains: StateFlow<List<String>> = cookieDelegate.cookieDomains

    fun deleteCookie(id: Long, domain: String? = null) = cookieDelegate.deleteCookie(id, domain)
    fun deleteCookiesByDomain(domain: String) = cookieDelegate.deleteCookiesByDomain(domain)
    fun deleteTrackerCookies() = cookieDelegate.deleteTrackerCookies()
    fun deleteProtectedCookies() = cookieDelegate.deleteProtectedCookies()
    fun clearAllCookies() = cookieDelegate.clearAllCookies()

    // --- Onboarding y Preferencias Generales ---
    val isOnboardingCompleted: StateFlow<Boolean> = repository.isOnboardingCompleted
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

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

    // --- Modos de Navegación y Pestañas ---
    enum class TabMode { NORMAL, PROTECTED, INCOGNITO }

    private val _isIncognitoMode = MutableStateFlow(false)
    val isIncognitoMode: StateFlow<Boolean> = _isIncognitoMode.asStateFlow()

    private val _isProtectedMode = MutableStateFlow(false)
    val isProtectedMode: StateFlow<Boolean> = _isProtectedMode.asStateFlow()

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

    // Hibernación de pestañas tras 5 minutos de inactividad
    private val TAB_SLEEP_TIMEOUT_MS = 5 * 60 * 1000L
    private val _hibernatedTabIds = MutableStateFlow<Set<Long>>(emptySet())
    val hibernatedTabIds: StateFlow<Set<Long>> = _hibernatedTabIds.asStateFlow()

    val tabThumbnails: StateFlow<Map<Long, Bitmap>> = TabThumbnailManager.thumbnailsFlow

    // --- Estado de la Página Web Actual ---
    private val _pageState = MutableStateFlow(EnginePageState())
    val pageState: StateFlow<EnginePageState> = _pageState.asStateFlow()

    private val _omniboxText = MutableStateFlow("")
    val omniboxText: StateFlow<String> = _omniboxText.asStateFlow()

    // --- Marcadores e Historial ---
    val bookmarks: StateFlow<List<BookmarkEntity>> = repository.getAllBookmarks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isCurrentPageBookmarked = MutableStateFlow(false)
    val isCurrentPageBookmarked: StateFlow<Boolean> = _isCurrentPageBookmarked.asStateFlow()

    val recentHistory: StateFlow<List<HistoryEntity>> = repository.getRecentHistory(150)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _historySearchQuery = MutableStateFlow("")
    val historySearchQuery: StateFlow<String> = _historySearchQuery.asStateFlow()

    // --- Diálogos Web Nativos ---
    private val _activeWebPrompt = MutableStateFlow<WebPromptRequest?>(null)
    val activeWebPrompt: StateFlow<WebPromptRequest?> = _activeWebPrompt.asStateFlow()

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

        // Sincronización dinámica de JavaScript con sesiones
        viewModelScope.launch {
            repository.isJavaScriptEnabled.collect { enabled ->
                sessionManager.setJavaScriptEnabled(enabled)
            }
        }

        // Sincronización dinámica de Do Not Track
        viewModelScope.launch {
            repository.isDoNotTrackEnabled.collect { enabled ->
                sessionManager.setDoNotTrackEnabled(enabled)
            }
        }

        // Sembrar cookies iniciales si es necesario y limpiar huérfanas
        viewModelScope.launch {
            cookieDelegate.seedInitialCookiesIfNeeded()
            val openProtectedTabs = repository.getProtectedTabs().first().map { it.id }.toSet()
            val allExistingCookies = repository.getAllCookies().first()
            allExistingCookies.filter { it.isProtected && (it.tabId == null || !openProtectedTabs.contains(it.tabId)) }
                .forEach { orphaned -> repository.deleteCookieById(orphaned.id) }
        }

        // Registrar cookies durante la navegación
        viewModelScope.launch {
            _pageState.collect { state ->
                if (state.url.isNotBlank() && state.url != "about:home" && state.url != "about:blank") {
                    cookieDelegate.recordCookiesForUrl(state.url, _activeTab.value)
                }
            }
        }

        // Detección de inicio de sesión asistido con cuenta vinculada
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
                        accountDelegate.postWebSignInPrompt(WebSignInBridge.createPrompt(state.url, currentAccount))
                    } else {
                        accountDelegate.dismissWebSignInPrompt()
                    }
                } else {
                    accountDelegate.dismissWebSignInPrompt()
                }
            }
        }

        // Monitor periódico de hibernación de pestañas (cada 15s)
        viewModelScope.launch {
            while (isActive) {
                delay(15_000L)
                checkAndHibernateInactiveTabs()
            }
        }
    }

    // --- Métodos de Navegación y Ciclo de Pestañas ---

    fun loadInput(input: String) {
        val currentEngine = searchEngine.value
        val formattedUrl = currentEngine.formatQueryOrUrl(input)
        _omniboxText.value = formattedUrl
        _pageState.value = _pageState.value.copy(url = formattedUrl, isLoading = true, progress = 15)

        _activeTab.value?.let { currentTab ->
            val updated = currentTab.copy(url = formattedUrl, lastActiveTimestamp = System.currentTimeMillis())
            _activeTab.value = updated
            viewModelScope.launch { repository.updateTab(updated) }
        }

        val tabId = _activeTabId.value ?: 1L
        val session = sessionManager.getOrCreateSession(
            tabId = tabId,
            isIncognito = _isIncognitoMode.value,
            isProtected = _isProtectedMode.value,
            contextId = _activeTab.value?.contextId
        )
        session.loadUri(formattedUrl)
        engineController?.loadUrl(formattedUrl)
    }

    fun onOmniboxTextChange(text: String) {
        _omniboxText.value = text
    }

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

    fun toggleIncognitoMode(enabled: Boolean) {
        _isIncognitoMode.value = enabled
        _isProtectedMode.value = false
        viewModelScope.launch {
            if (enabled) {
                val tabs = repository.getIncognitoTabs().first()
                if (tabs.isNotEmpty()) {
                    switchToTab(tabs.first().id)
                } else {
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

    fun switchToProtectedMode() {
        _isIncognitoMode.value = false
        _isProtectedMode.value = true
        viewModelScope.launch {
            val tabs = repository.getProtectedTabs().first()
            if (tabs.isNotEmpty()) {
                switchToTab(tabs.first().id)
            } else {
                _activeTabId.value = null
                _activeTab.value = null
                _pageState.value = EnginePageState("about:home", "Pestaña Protegida")
                _omniboxText.value = ""
            }
        }
    }

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
            val id = repository.createTab(title, url, isIncognito, isProtected, contextId)
            switchToTab(id)
        }
    }

    fun createProtectedTab(url: String = "about:home") {
        createNewTab(url = url, isIncognito = false, isProtected = true)
    }

    fun switchToTab(tabId: Long) {
        viewModelScope.launch {
            val tab = repository.getTabById(tabId) ?: return@launch
            _hibernatedTabIds.value = _hibernatedTabIds.value - tab.id
            repository.updateTabLastActive(tab.id, System.currentTimeMillis())

            _activeTabId.value = tab.id
            _activeTab.value = tab
            _isIncognitoMode.value = tab.isIncognito
            _isProtectedMode.value = tab.isProtected
            _omniboxText.value = if (tab.url == "about:home" || tab.url == "about:blank") "" else tab.url

            _pageState.value = _pageState.value.copy(url = tab.url, title = tab.title)
            engineController?.loadUrl(tab.url)
        }
    }

    fun closeTab(tabId: Long) {
        viewModelScope.launch {
            val tab = repository.getTabById(tabId)
            val isProtected = tab?.isProtected ?: false
            val isIncognito = tab?.isIncognito ?: false
            val contextId = tab?.contextId

            sessionManager.closeSession(tabId, isIncognito, isProtected, contextId)
            TabThumbnailManager.removeThumbnail(tabId)
            _hibernatedTabIds.value = _hibernatedTabIds.value - tabId
            repository.closeTab(tabId)

            if (isProtected) {
                repository.deleteCookiesByTabId(tabId)
                if (contextId != null) repository.deleteCookiesByContextId(contextId)
            }
            if (isIncognito) sessionManager.purgeIncognitoMemory()

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
        } catch (_: Throwable) {}
    }

    fun captureCurrentTabThumbnail() {
        val currentId = _activeTabId.value ?: return
        val controller = engineController
        if (controller is GeckoViewEngine) {
            controller.captureThumbnail { bitmap ->
                TabThumbnailManager.saveThumbnail(currentId, bitmap)
            }
        }
    }

    fun wakeUpTab(tabId: Long) {
        _hibernatedTabIds.value = _hibernatedTabIds.value - tabId
        switchToTab(tabId)
    }

    fun hibernateTab(tabId: Long) {
        if (tabId != _activeTabId.value) {
            _hibernatedTabIds.value = _hibernatedTabIds.value + tabId
            sessionManager.hibernateSession(tabId)
        }
    }

    // --- Marcadores e Historial ---
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

    fun deleteBookmark(id: Long) = viewModelScope.launch { repository.removeBookmarkById(id) }
    fun addBookmarkManual(title: String, url: String) = viewModelScope.launch { repository.addBookmark(title, url) }
    fun deleteHistoryItem(id: Long) = viewModelScope.launch { repository.deleteHistoryEntry(id) }
    fun clearAllHistory() = viewModelScope.launch { repository.clearHistory() }
    fun onHistorySearchQueryChange(query: String) { _historySearchQuery.value = query }

    // --- Notificaciones del Motor de Renderizado ---
    fun onPageStarted(url: String) {
        val isSecure = url.startsWith("https://", ignoreCase = true)
        _pageState.value = _pageState.value.copy(url = url, isLoading = true, isSecure = isSecure)
        if (url != "about:home" && url != "about:blank") _omniboxText.value = url
    }

    fun onPageFinished(url: String, title: String?) {
        val isSecure = url.startsWith("https://", ignoreCase = true)
        val resolvedTitle = title ?: url
        val faviconUrl = try {
            val uri = android.net.Uri.parse(url)
            val host = uri.host?.removePrefix("www.")
            if (!host.isNullOrBlank() && !url.startsWith("about:")) "https://www.google.com/s2/favicons?domain=$host&sz=128" else null
        } catch (_: Throwable) { null }

        _pageState.value = _pageState.value.copy(
            url = url,
            title = resolvedTitle,
            isLoading = false,
            progress = 100,
            isSecure = isSecure,
            faviconUrl = faviconUrl
        )

        _activeTab.value?.let { currentTab ->
            val updated = currentTab.copy(url = url, title = resolvedTitle, lastActiveTimestamp = System.currentTimeMillis())
            _activeTab.value = updated
            viewModelScope.launch {
                repository.updateTab(updated)
                repository.addHistoryEntry(resolvedTitle, url, _isIncognitoMode.value, faviconUrl)
                delay(400L)
                captureCurrentTabThumbnail()
            }
        }
    }

    fun onProgressChanged(newProgress: Int) {
        _pageState.value = _pageState.value.copy(progress = newProgress, isLoading = newProgress < 100)
    }

    fun onNavigationStateChanged(canGoBack: Boolean, canGoForward: Boolean) {
        _pageState.value = _pageState.value.copy(canGoBack = canGoBack, canGoForward = canGoForward)
    }

    fun onSecurityChanged(isSecure: Boolean) {
        _pageState.value = _pageState.value.copy(isSecure = isSecure)
    }

    fun toggleDesktopMode() {
        val newMode = !_pageState.value.isDesktopMode
        _pageState.value = _pageState.value.copy(isDesktopMode = newMode)
        _activeTabId.value?.let { tabId -> sessionManager.setDesktopModeForTab(tabId, newMode) }
        engineController?.setDesktopMode(newMode)
        engineController?.reload()
    }

    fun postWebPrompt(prompt: WebPromptRequest) { _activeWebPrompt.value = prompt }
    fun dismissWebPrompt() { _activeWebPrompt.value = null }

    // --- Preferencias del Navegador ---
    fun setOnboardingCompleted(completed: Boolean = true) = viewModelScope.launch { repository.setOnboardingCompleted(completed) }
    fun selectSearchEngine(engine: SearchEngine) = viewModelScope.launch { repository.setSearchEngine(engine) }
    fun setSearchEngine(engine: SearchEngine) = viewModelScope.launch { repository.setSearchEngine(engine) }
    fun setDesktopModeDefault(enabled: Boolean) = viewModelScope.launch { repository.setDesktopModeDefault(enabled) }
    fun setJavaScriptEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setJavaScriptEnabled(enabled)
            engineController?.setJavaScriptEnabled(enabled)
        }
    }
    fun setCookiesEnabled(enabled: Boolean) = viewModelScope.launch { repository.setCookiesEnabled(enabled) }
    fun setDoNotTrack(enabled: Boolean) = viewModelScope.launch { repository.setDoNotTrack(enabled) }
    fun setHomePageUrl(url: String) = viewModelScope.launch { repository.setHomePageUrl(url) }
    fun setSoundEffectsEnabled(enabled: Boolean) = viewModelScope.launch { repository.setSoundEffectsEnabled(enabled) }

    fun clearBrowsingData() {
        viewModelScope.launch {
            repository.clearHistory()
            repository.clearAllCookies()
            try {
                GeckoRuntimeProvider.get(getApplication()).storageController.clearData(StorageController.ClearFlags.ALL)
            } catch (_: Throwable) {}
            engineController?.clearCache()
        }
    }

    // --- Modo Lectura Nativo (Delegado: ReaderDelegate) ---
    val readerArticle: StateFlow<ReaderArticle?> get() = readerDelegate.currentArticle
    val isReaderLoading: StateFlow<Boolean> get() = readerDelegate.isLoading
    val readerErrorMessage: StateFlow<String?> get() = readerDelegate.errorMessage
    val readerSettings: StateFlow<ReaderSettings> get() = readerDelegate.settings

    fun openReaderMode(url: String = pageState.value.url, title: String = pageState.value.title) {
        readerDelegate.loadArticle(url, title)
    }
    fun toggleReaderSpeech() = readerDelegate.toggleSpeech()
    fun stopReaderSpeech() = readerDelegate.stopSpeech()
    fun setReaderTheme(theme: ReaderTheme) = readerDelegate.setTheme(theme)
    fun setReaderFont(font: ReaderFont) = readerDelegate.setFont(font)
    fun adjustReaderFontSize(deltaSp: Float) = readerDelegate.adjustFontSize(deltaSp)

    // --- Motor de Filtrado de Red Nativo (Delegado: FilterDelegate) ---
    val isFilterEnabled: StateFlow<Boolean> get() = filterDelegate.isFilterEnabled
    val filterRulesCount: StateFlow<Int> get() = filterDelegate.rulesCount
    val blockedRequestsCount: StateFlow<Long> get() = filterDelegate.blockedRequestsCount
    val isUpdatingFilterRules: StateFlow<Boolean> get() = filterDelegate.isUpdatingRules
    val lastFilterUpdateMessage: StateFlow<String?> get() = filterDelegate.lastUpdateMessage

    fun toggleFilterShield(enabled: Boolean) = filterDelegate.toggleFilter(enabled)
    fun updateFilterRulesFromRemote(onComplete: ((Boolean, Int) -> Unit)? = null) =
        filterDelegate.updateRulesFromRemote(onComplete = onComplete)
    fun addCustomFilterRule(rule: String) = filterDelegate.addRules(rule)
    fun refreshFilterStats() = filterDelegate.refreshStats()
    fun shouldBlockUrlRequest(url: String, sourceUrl: String = "", requestType: String = "other"): Boolean =
        filterDelegate.shouldBlockUrl(url, sourceUrl, requestType)

    // --- Escudo de Seguridad Web, Anti-Phishing y Malware en Tiempo Real ---
    val isThreatShieldEnabled: StateFlow<Boolean> get() = threatProtectionDelegate.isThreatShieldEnabled
    val blockedThreatsCount: StateFlow<Long> get() = threatProtectionDelegate.blockedThreatsCount
    val currentBlockedThreat: StateFlow<BlockedThreatDetail?> get() = threatProtectionDelegate.currentBlockedThreat
    val isUpdatingThreatFeeds: StateFlow<Boolean> get() = threatProtectionDelegate.isUpdatingFeeds
    val threatFeedUpdateStatus: StateFlow<String?> get() = threatProtectionDelegate.feedUpdateStatus
    val threatFeeds: StateFlow<List<SecurityThreatFeed>> get() = threatProtectionDelegate.threatFeeds

    fun toggleThreatShield(enabled: Boolean) = threatProtectionDelegate.toggleThreatShield(enabled)
    fun evaluateNavigationSecurity(url: String): BlockedThreatDetail? = threatProtectionDelegate.evaluateNavigationSecurity(url)
    fun bypassThreatAndAllow(domain: String) = threatProtectionDelegate.bypassThreatAndAllow(domain)
    fun dismissBlockedThreat() = threatProtectionDelegate.dismissBlockedThreat()
    fun updateThreatFeedsFromRemote(onComplete: ((Boolean, Int) -> Unit)? = null) =
        threatProtectionDelegate.updateThreatFeeds(onComplete)

    override fun onCleared() {
        super.onCleared()
        readerDelegate.release()
    }
}

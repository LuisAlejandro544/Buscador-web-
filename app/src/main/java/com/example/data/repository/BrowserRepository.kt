package com.example.data.repository

import androidx.annotation.WorkerThread
import com.example.data.local.dao.BookmarkDao
import com.example.data.local.dao.CookieDao
import com.example.data.local.dao.DownloadDao
import com.example.data.local.dao.HistoryDao
import com.example.data.local.dao.SitePermissionDao
import com.example.data.local.dao.TabDao
import com.example.data.local.dao.UserAccountDao
import com.example.data.local.entity.BookmarkEntity
import com.example.data.local.entity.CookieEntity
import com.example.data.local.entity.DownloadEntity
import com.example.data.local.entity.HistoryEntity
import com.example.data.local.entity.SitePermissionEntity
import com.example.data.local.entity.TabEntity
import com.example.data.local.entity.UserAccountEntity
import com.example.data.model.SearchEngine
import com.example.data.preferences.BrowserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Repositorio central de datos para el navegador web.
 * Abstrae las fuentes de datos (Room y DataStore) proporcionando una interfaz unificada
 * y garantizando la ejecución de operaciones fuera del hilo de interfaz (UI Thread).
 */
class BrowserRepository(
    private val tabDao: TabDao,
    private val bookmarkDao: BookmarkDao,
    private val historyDao: HistoryDao,
    private val downloadDao: DownloadDao,
    private val cookieDao: CookieDao,
    private val userAccountDao: UserAccountDao,
    private val sitePermissionDao: SitePermissionDao,
    private val preferences: BrowserPreferences
) {
    // --- Pestañas ---
    fun getNormalTabs(): Flow<List<TabEntity>> = tabDao.getNormalTabs()

    fun getProtectedTabs(): Flow<List<TabEntity>> = tabDao.getProtectedTabs()

    fun getIncognitoTabs(): Flow<List<TabEntity>> = tabDao.getIncognitoTabs()

    fun getTabs(isIncognito: Boolean): Flow<List<TabEntity>> = tabDao.getTabs(isIncognito)
    
    fun getTabCount(isIncognito: Boolean): Flow<Int> = tabDao.getTabCount(isIncognito)

    fun getProtectedTabCount(): Flow<Int> = tabDao.getProtectedTabCount()

    suspend fun getTabById(id: Long): TabEntity? = tabDao.getTabById(id)

    suspend fun getMostRecentActiveTab(): TabEntity? = tabDao.getMostRecentActiveTab()

    suspend fun getAllTabsList(): List<TabEntity> = tabDao.getAllTabsList()

    suspend fun updateTabLastActive(id: Long, timestamp: Long) = tabDao.updateTabLastActive(id, timestamp)

    suspend fun createTab(
        title: String,
        url: String,
        isIncognito: Boolean,
        isProtected: Boolean = false,
        contextId: String? = null
    ): Long {
        val tab = TabEntity(
            title = title,
            url = url,
            isIncognito = isIncognito,
            isProtected = isProtected,
            contextId = contextId,
            lastActiveTimestamp = System.currentTimeMillis()
        )
        return tabDao.insertTab(tab)
    }

    suspend fun updateTab(tab: TabEntity) = tabDao.updateTab(tab)

    suspend fun closeTab(id: Long) = tabDao.deleteTabById(id)

    suspend fun clearTabs(isIncognito: Boolean) = tabDao.clearTabs(isIncognito)

    suspend fun clearProtectedTabs() = tabDao.clearProtectedTabs()

    // --- Marcadores / Favoritos ---
    fun getAllBookmarks(): Flow<List<BookmarkEntity>> = bookmarkDao.getAllBookmarks()

    fun searchBookmarks(query: String): Flow<List<BookmarkEntity>> = bookmarkDao.searchBookmarks(query)

    fun isBookmarked(url: String): Flow<Boolean> = bookmarkDao.isBookmarked(url)

    suspend fun addBookmark(title: String, url: String, faviconUrl: String? = null): Long {
        val bookmark = BookmarkEntity(
            title = title.ifBlank { url },
            url = url,
            faviconUrl = faviconUrl
        )
        return bookmarkDao.insertBookmark(bookmark)
    }

    suspend fun removeBookmarkByUrl(url: String) = bookmarkDao.deleteBookmarkByUrl(url)

    suspend fun removeBookmarkById(id: Long) = bookmarkDao.deleteBookmarkById(id)

    // --- Historial ---
    fun getRecentHistory(limit: Int = 100): Flow<List<HistoryEntity>> = historyDao.getRecentHistory(limit)

    fun searchHistory(query: String): Flow<List<HistoryEntity>> = historyDao.searchHistory(query)

    suspend fun addHistoryEntry(title: String, url: String, isIncognito: Boolean, faviconUrl: String? = null) {
        // En modo incógnito NO se guarda historial
        if (isIncognito) return
        if (url == "about:blank" || url == "about:home") return

        val entry = HistoryEntity(
            title = title.ifBlank { url },
            url = url,
            faviconUrl = faviconUrl,
            visitedAt = System.currentTimeMillis()
        )
        historyDao.insertHistory(entry)
    }

    suspend fun deleteHistoryEntry(id: Long) = historyDao.deleteHistoryById(id)

    suspend fun clearHistory() = historyDao.clearAllHistory()

    // --- Descargas ---
    fun getAllDownloads(): Flow<List<DownloadEntity>> = downloadDao.getAllDownloads()

    suspend fun getDownloadById(id: Long): DownloadEntity? = downloadDao.getDownloadById(id)

    suspend fun addDownload(download: DownloadEntity): Long = downloadDao.insertDownload(download)

    suspend fun updateDownload(download: DownloadEntity) = downloadDao.updateDownload(download)

    suspend fun deleteDownload(id: Long) = downloadDao.deleteDownloadById(id)

    suspend fun clearAllDownloads() = downloadDao.clearAllDownloads()

    // --- Cookies de Navegación ---
    fun getAllCookies(): Flow<List<CookieEntity>> = cookieDao.getAllCookies()

    fun searchCookies(query: String): Flow<List<CookieEntity>> = cookieDao.searchCookies(query)

    fun getTrackerCookies(): Flow<List<CookieEntity>> = cookieDao.getTrackerCookies()

    fun getProtectedCookies(): Flow<List<CookieEntity>> = cookieDao.getProtectedCookies()

    fun getNormalCookies(): Flow<List<CookieEntity>> = cookieDao.getNormalCookies()

    fun getDistinctCookieDomains(): Flow<List<String>> = cookieDao.getDistinctDomains()

    fun getCookiesByDomain(domain: String): Flow<List<CookieEntity>> = cookieDao.getCookiesByDomain(domain)

    fun getCookieCount(): Flow<Int> = cookieDao.getCookieCount()

    fun getTrackerCookieCount(): Flow<Int> = cookieDao.getTrackerCookieCount()

    fun getProtectedCookieCount(): Flow<Int> = cookieDao.getProtectedCookieCount()

    suspend fun addCookie(cookie: CookieEntity): Long = cookieDao.insertCookie(cookie)

    suspend fun addCookies(cookies: List<CookieEntity>) = cookieDao.insertCookies(cookies)

    suspend fun deleteCookieById(id: Long) = cookieDao.deleteCookieById(id)

    suspend fun deleteCookiesByTabId(tabId: Long) = cookieDao.deleteCookiesByTabId(tabId)

    suspend fun deleteCookiesByContextId(contextId: String) = cookieDao.deleteCookiesByContextId(contextId)

    suspend fun deleteProtectedCookies() = cookieDao.deleteProtectedCookies()

    suspend fun deleteCookiesByDomain(domain: String) = cookieDao.deleteCookiesByDomain(domain)

    suspend fun deleteTrackerCookies() = cookieDao.deleteTrackerCookies()

    suspend fun clearAllCookies() = cookieDao.clearAllCookies()

    // --- Cuentas de Usuario y Sincronización Web ---
    fun getAllAccounts(): Flow<List<UserAccountEntity>> = userAccountDao.getAllAccounts()

    fun getActiveAccount(): Flow<UserAccountEntity?> = userAccountDao.getActiveAccount()

    suspend fun getActiveAccountDirect(): UserAccountEntity? = userAccountDao.getActiveAccountDirect()

    fun getAccountsCount(): Flow<Int> = userAccountDao.getAccountsCount()

    suspend fun linkAccount(account: UserAccountEntity): Long {
        // Si la nueva cuenta se marca como activa, desactivamos las demás primero
        if (account.isActive) {
            userAccountDao.deactivateAllAccounts()
        }
        return userAccountDao.insertAccount(account)
    }

    suspend fun setActiveAccount(id: Long) = userAccountDao.switchActiveAccount(id)

    suspend fun setAutoSignInWeb(id: Long, enabled: Boolean) = userAccountDao.updateAutoSignIn(id, enabled)

    suspend fun removeAccount(id: Long) {
        userAccountDao.deleteAccountById(id)
        // Si eliminamos la activa y quedan cuentas, activamos la más reciente
        val remaining = userAccountDao.getActiveAccountDirect()
        if (remaining == null) {
            // No hay activa, pero si hay alguna cuenta, reactivar la primera
            // userAccountDao.getAllAccounts() mantendrá la consistencia
        }
    }

    suspend fun clearAllAccounts() = userAccountDao.clearAllAccounts()

    // --- Permisos por Sitio Web ---
    fun getAllSitePermissions(): Flow<List<SitePermissionEntity>> = sitePermissionDao.getAllPermissions()

    fun getSitePermissionsForOrigin(origin: String): Flow<List<SitePermissionEntity>> = sitePermissionDao.getPermissionsForOrigin(origin)

    suspend fun findSitePermission(origin: String, type: String): SitePermissionEntity? = withContext(Dispatchers.IO) {
        sitePermissionDao.findPermission(origin, type)
    }

    /**
     * Consulta síncrona exclusiva para hilos de fondo. No llamar desde el hilo principal de la UI.
     */
    @WorkerThread
    fun findSitePermissionSync(origin: String, type: String): SitePermissionEntity? = sitePermissionDao.findPermissionSync(origin, type)

    suspend fun saveSitePermission(origin: String, permissionType: String, status: String): Long = withContext(Dispatchers.IO) {
        sitePermissionDao.insertOrUpdate(
            SitePermissionEntity(
                origin = origin,
                permissionType = permissionType,
                status = status,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun updateSitePermissionStatus(id: Long, status: String) = withContext(Dispatchers.IO) {
        sitePermissionDao.updateStatus(id, status, System.currentTimeMillis())
    }

    suspend fun deleteSitePermission(id: Long) = withContext(Dispatchers.IO) {
        sitePermissionDao.deleteById(id)
    }

    suspend fun deleteSitePermissionsForOrigin(origin: String) = withContext(Dispatchers.IO) {
        sitePermissionDao.deleteByOrigin(origin)
    }

    suspend fun clearAllSitePermissions() = withContext(Dispatchers.IO) {
        sitePermissionDao.clearAll()
    }

    // --- Preferencias y Ajustes ---
    val searchEngine: Flow<SearchEngine> = preferences.searchEngine
    val isDesktopModeDefault: Flow<Boolean> = preferences.isDesktopModeDefault
    val isJavaScriptEnabled: Flow<Boolean> = preferences.isJavaScriptEnabled
    val isCookiesEnabled: Flow<Boolean> = preferences.isCookiesEnabled
    val isDoNotTrackEnabled: Flow<Boolean> = preferences.isDoNotTrackEnabled
    val homePageUrl: Flow<String> = preferences.homePageUrl

    // Políticas de Bloqueo Silencioso ("No Preguntar")
    val blockNotificationPrompts: Flow<Boolean> = preferences.blockNotificationPrompts
    val blockLocationPrompts: Flow<Boolean> = preferences.blockLocationPrompts
    val blockMediaPrompts: Flow<Boolean> = preferences.blockMediaPrompts
    val isSoundEffectsEnabled: Flow<Boolean> = preferences.isSoundEffectsEnabled
    val isOnboardingCompleted: Flow<Boolean> = preferences.isOnboardingCompleted

    fun isOnboardingCompletedSync(): Boolean = preferences.isOnboardingCompletedSync()
    fun getSearchEngineSync(): SearchEngine = preferences.getSearchEngineSync()

    suspend fun setSearchEngine(engine: SearchEngine) = preferences.setSearchEngine(engine)
    suspend fun setDesktopModeDefault(enabled: Boolean) = preferences.setDesktopModeDefault(enabled)
    suspend fun setJavaScriptEnabled(enabled: Boolean) = preferences.setJavaScriptEnabled(enabled)
    suspend fun setCookiesEnabled(enabled: Boolean) = preferences.setCookiesEnabled(enabled)
    suspend fun setDoNotTrack(enabled: Boolean) = preferences.setDoNotTrack(enabled)
    suspend fun setHomePageUrl(url: String) = preferences.setHomePageUrl(url)

    suspend fun setBlockNotificationPrompts(enabled: Boolean) = preferences.setBlockNotificationPrompts(enabled)
    suspend fun setBlockLocationPrompts(enabled: Boolean) = preferences.setBlockLocationPrompts(enabled)
    suspend fun setBlockMediaPrompts(enabled: Boolean) = preferences.setBlockMediaPrompts(enabled)
    suspend fun setSoundEffectsEnabled(enabled: Boolean) = preferences.setSoundEffectsEnabled(enabled)
    suspend fun setOnboardingCompleted(completed: Boolean) = preferences.setOnboardingCompleted(completed)

    // --- Actualizaciones en Segundo Plano del Escudo de Seguridad ---
    val isAutoUpdateThreatsEnabled: Flow<Boolean> = preferences.isAutoUpdateThreatsEnabled
    val isThreatsUpdateOnlyWifi: Flow<Boolean> = preferences.isThreatsUpdateOnlyWifi
    val lastThreatUpdateTimestamp: Flow<Long> = preferences.lastThreatUpdateTimestamp
    val lastThreatUpdateRulesCount: Flow<Int> = preferences.lastThreatUpdateRulesCount

    suspend fun setAutoUpdateThreatsEnabled(enabled: Boolean) = preferences.setAutoUpdateThreatsEnabled(enabled)
    suspend fun setThreatsUpdateOnlyWifi(onlyWifi: Boolean) = preferences.setThreatsUpdateOnlyWifi(onlyWifi)
    suspend fun recordThreatUpdateResult(rulesCount: Int, timestamp: Long = System.currentTimeMillis()) =
        preferences.recordThreatUpdateResult(rulesCount, timestamp)
}

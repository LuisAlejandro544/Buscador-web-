package com.example.data.repository

import com.example.data.local.dao.BookmarkDao
import com.example.data.local.dao.DownloadDao
import com.example.data.local.dao.HistoryDao
import com.example.data.local.dao.TabDao
import com.example.data.local.entity.BookmarkEntity
import com.example.data.local.entity.DownloadEntity
import com.example.data.local.entity.HistoryEntity
import com.example.data.local.entity.TabEntity
import com.example.data.model.SearchEngine
import com.example.data.preferences.BrowserPreferences
import kotlinx.coroutines.flow.Flow

/**
 * Repositorio central de datos para el navegador web.
 * Abstrae las fuentes de datos (Room y DataStore) proporcionando una interfaz unificada
 * para el ViewModel y los controladores del navegador.
 */
class BrowserRepository(
    private val tabDao: TabDao,
    private val bookmarkDao: BookmarkDao,
    private val historyDao: HistoryDao,
    private val downloadDao: DownloadDao,
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

    suspend fun addHistoryEntry(title: String, url: String, isIncognito: Boolean) {
        // En modo incógnito NO se guarda historial
        if (isIncognito) return
        if (url == "about:blank" || url == "about:home") return

        val entry = HistoryEntity(
            title = title.ifBlank { url },
            url = url,
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

    // --- Preferencias y Ajustes ---
    val searchEngine: Flow<SearchEngine> = preferences.searchEngine
    val isDesktopModeDefault: Flow<Boolean> = preferences.isDesktopModeDefault
    val isJavaScriptEnabled: Flow<Boolean> = preferences.isJavaScriptEnabled
    val isCookiesEnabled: Flow<Boolean> = preferences.isCookiesEnabled
    val isDoNotTrackEnabled: Flow<Boolean> = preferences.isDoNotTrackEnabled
    val homePageUrl: Flow<String> = preferences.homePageUrl

    suspend fun setSearchEngine(engine: SearchEngine) = preferences.setSearchEngine(engine)
    suspend fun setDesktopModeDefault(enabled: Boolean) = preferences.setDesktopModeDefault(enabled)
    suspend fun setJavaScriptEnabled(enabled: Boolean) = preferences.setJavaScriptEnabled(enabled)
    suspend fun setCookiesEnabled(enabled: Boolean) = preferences.setCookiesEnabled(enabled)
    suspend fun setDoNotTrack(enabled: Boolean) = preferences.setDoNotTrack(enabled)
    suspend fun setHomePageUrl(url: String) = preferences.setHomePageUrl(url)
}

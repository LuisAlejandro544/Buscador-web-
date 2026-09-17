package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.SearchEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "browser_settings")

/**
 * Gestor de preferencias de usuario del navegador usando Jetpack DataStore con respaldo sincrónico
 * en SharedPreferences para evitar pantallas de bienvenida intermitentes o parpadeos de configuración.
 */
class BrowserPreferences(private val context: Context) {

    private val sharedPrefs = context.getSharedPreferences("browser_settings_sync", Context.MODE_PRIVATE)

    companion object {
        val KEY_SEARCH_ENGINE = stringPreferencesKey("search_engine")
        val KEY_DESKTOP_MODE_DEFAULT = booleanPreferencesKey("desktop_mode_default")
        val KEY_JAVASCRIPT_ENABLED = booleanPreferencesKey("javascript_enabled")
        val KEY_COOKIES_ENABLED = booleanPreferencesKey("cookies_enabled")
        val KEY_DO_NOT_TRACK = booleanPreferencesKey("do_not_track")
        val KEY_HOME_PAGE_URL = stringPreferencesKey("home_page_url")
        val KEY_BLOCK_THIRD_PARTY_COOKIES = booleanPreferencesKey("block_third_party_cookies")
        val KEY_BLOCK_NOTIFICATION_PROMPTS = booleanPreferencesKey("block_notification_prompts")
        val KEY_BLOCK_LOCATION_PROMPTS = booleanPreferencesKey("block_location_prompts")
        val KEY_BLOCK_MEDIA_PROMPTS = booleanPreferencesKey("block_media_prompts")
        val KEY_SOUND_EFFECTS_ENABLED = booleanPreferencesKey("sound_effects_enabled")
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")

        private const val SP_KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val SP_KEY_SEARCH_ENGINE = "search_engine"
    }

    /**
     * Consulta inmediata y sincrónica para evitar cualquier redirección errónea en el arranque.
     */
    fun isOnboardingCompletedSync(): Boolean {
        return sharedPrefs.getBoolean(SP_KEY_ONBOARDING_COMPLETED, false)
    }

    fun getSearchEngineSync(): SearchEngine {
        val name = sharedPrefs.getString(SP_KEY_SEARCH_ENGINE, SearchEngine.DUCKDUCKGO.name)
        return try {
            SearchEngine.valueOf(name ?: SearchEngine.DUCKDUCKGO.name)
        } catch (_: Exception) {
            SearchEngine.DUCKDUCKGO
        }
    }

    val isOnboardingCompleted: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[KEY_ONBOARDING_COMPLETED] ?: isOnboardingCompletedSync()
        }

    val searchEngine: Flow<SearchEngine> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val engineName = preferences[KEY_SEARCH_ENGINE] ?: sharedPrefs.getString(SP_KEY_SEARCH_ENGINE, SearchEngine.DUCKDUCKGO.name)
            try {
                SearchEngine.valueOf(engineName ?: SearchEngine.DUCKDUCKGO.name)
            } catch (_: Exception) {
                SearchEngine.DUCKDUCKGO
            }
        }

    val isDesktopModeDefault: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_DESKTOP_MODE_DEFAULT] ?: false
    }

    val isJavaScriptEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_JAVASCRIPT_ENABLED] ?: true
    }

    val isCookiesEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_COOKIES_ENABLED] ?: true
    }

    val isDoNotTrackEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_DO_NOT_TRACK] ?: true
    }

    val homePageUrl: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_HOME_PAGE_URL] ?: "about:home"
    }

    // Bloqueo Silencioso de Solicitudes de Permisos ("No Preguntar")
    val blockNotificationPrompts: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_BLOCK_NOTIFICATION_PROMPTS] ?: false
    }

    val blockLocationPrompts: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_BLOCK_LOCATION_PROMPTS] ?: false
    }

    val blockMediaPrompts: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_BLOCK_MEDIA_PROMPTS] ?: false
    }

    val isSoundEffectsEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_SOUND_EFFECTS_ENABLED] ?: true
    }

    suspend fun setSearchEngine(engine: SearchEngine) {
        sharedPrefs.edit().putString(SP_KEY_SEARCH_ENGINE, engine.name).apply()
        context.dataStore.edit { preferences ->
            preferences[KEY_SEARCH_ENGINE] = engine.name
        }
    }

    suspend fun setDesktopModeDefault(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_DESKTOP_MODE_DEFAULT] = enabled
        }
    }

    suspend fun setJavaScriptEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_JAVASCRIPT_ENABLED] = enabled
        }
    }

    suspend fun setCookiesEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_COOKIES_ENABLED] = enabled
        }
    }

    suspend fun setDoNotTrack(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_DO_NOT_TRACK] = enabled
        }
    }

    suspend fun setHomePageUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_HOME_PAGE_URL] = url
        }
    }

    suspend fun setBlockNotificationPrompts(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_BLOCK_NOTIFICATION_PROMPTS] = enabled
        }
    }

    suspend fun setBlockLocationPrompts(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_BLOCK_LOCATION_PROMPTS] = enabled
        }
    }

    suspend fun setBlockMediaPrompts(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_BLOCK_MEDIA_PROMPTS] = enabled
        }
    }

    suspend fun setSoundEffectsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SOUND_EFFECTS_ENABLED] = enabled
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        sharedPrefs.edit().putBoolean(SP_KEY_ONBOARDING_COMPLETED, completed).apply()
        context.dataStore.edit { preferences ->
            preferences[KEY_ONBOARDING_COMPLETED] = completed
        }
    }
}

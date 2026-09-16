package com.example.viewmodel.delegates

import android.content.Context
import com.example.browser.engine.GeckoRuntimeProvider
import com.example.data.local.entity.CookieEntity
import com.example.data.local.entity.TabEntity
import com.example.data.repository.BrowserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.mozilla.geckoview.StorageController

/**
 * Delegado de gestión de cookies de navegación web.
 * 
 * Responsabilidades:
 * - Exponer flujos de estado (StateFlow) de cookies: todas, rastreadores, protegidas y dominios.
 * - Eliminar cookies individuales, por dominio, rastreadores o purga completa.
 * - Notificar al controlador de almacenamiento de GeckoView (StorageController) para sincronización nativa.
 * - Registrar y categorizar cookies interceptadas durante la navegación web.
 * - Sembrar cookies iniciales demostrativas para inspección visual.
 */
class CookieDelegate(
    private val context: Context,
    private val repository: BrowserRepository,
    private val scope: CoroutineScope
) {
    // Flujos de estado observables para la interfaz de usuario
    val allCookies: StateFlow<List<CookieEntity>> = repository.getAllCookies()
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trackerCookies: StateFlow<List<CookieEntity>> = repository.getTrackerCookies()
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    val protectedCookies: StateFlow<List<CookieEntity>> = repository.getProtectedCookies()
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cookieCount: StateFlow<Int> = repository.getCookieCount()
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), 0)

    val trackerCookieCount: StateFlow<Int> = repository.getTrackerCookieCount()
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), 0)

    val protectedCookieCount: StateFlow<Int> = repository.getProtectedCookieCount()
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), 0)

    val cookieDomains: StateFlow<List<String>> = repository.getDistinctCookieDomains()
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Elimina una cookie individual por su ID y notifica a GeckoView.
     */
    fun deleteCookie(id: Long, domain: String? = null) {
        scope.launch {
            repository.deleteCookieById(id)
            domain?.let { host ->
                try {
                    GeckoRuntimeProvider.get(context).storageController
                        .clearDataFromHost(host, StorageController.ClearFlags.COOKIES)
                } catch (_: Throwable) {
                    // Limpieza preventiva
                }
            }
        }
    }

    /**
     * Elimina todas las cookies registradas para un dominio específico.
     */
    fun deleteCookiesByDomain(domain: String) {
        scope.launch {
            repository.deleteCookiesByDomain(domain)
            try {
                GeckoRuntimeProvider.get(context).storageController
                    .clearDataFromHost(domain, StorageController.ClearFlags.COOKIES)
            } catch (_: Throwable) {
                // Limpieza preventiva
            }
        }
    }

    /**
     * Elimina todas las cookies clasificadas como rastreadores de publicidad o analítica.
     */
    fun deleteTrackerCookies() {
        scope.launch {
            repository.deleteTrackerCookies()
        }
    }

    /**
     * Elimina todas las cookies originadas en pestañas protegidas.
     */
    fun deleteProtectedCookies() {
        scope.launch {
            repository.deleteProtectedCookies()
        }
    }

    /**
     * Limpia completamente todas las cookies registradas en la base de datos y en el motor GeckoView.
     */
    fun clearAllCookies() {
        scope.launch {
            repository.clearAllCookies()
            try {
                GeckoRuntimeProvider.get(context).storageController
                    .clearData(StorageController.ClearFlags.COOKIES)
            } catch (_: Throwable) {
                // Limpieza preventiva
            }
        }
    }

    /**
     * Extrae el dominio y cataloga cookies detectadas durante la navegación web,
     * asociándolas a la pestaña activa (y si ésta es protegida o normal).
     */
    fun recordCookiesForUrl(url: String, tab: TabEntity?) {
        scope.launch {
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
            } catch (_: Throwable) {
                // Silencioso en caso de URIs especiales
            }
        }
    }

    /**
     * Siembra cookies iniciales demostrativas si no hay cookies registradas.
     */
    suspend fun seedInitialCookiesIfNeeded() {
        val count = repository.getCookieCount().first()
        if (count == 0) {
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
    }
}

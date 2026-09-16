package com.example.browser.engine

import android.content.Context
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import java.util.concurrent.ConcurrentHashMap

/**
 * Gestor de sesiones GeckoSession para la arquitectura multipestaña.
 * 
 * Mantiene un mapa en memoria de pestañas y sus sesiones correspondientes de GeckoView.
 * Esto permite cambiar entre pestañas de forma instantánea sin recargar ni perder el estado
 * de la página, aislando además las pestañas privadas de las normales.
 */
class GeckoSessionManager(private val context: Context) {

    private val runtime: GeckoRuntime get() = GeckoRuntimeProvider.get(context)
    private val sessions = ConcurrentHashMap<Long, GeckoSession>()

    // Estados dinámicos de configuración aplicados globalmente a las sesiones
    var isJavaScriptEnabled: Boolean = true
        private set
    var isDoNotTrackEnabled: Boolean = true
        private set

    /**
     * Sincroniza dinámicamente el estado de ejecución de JavaScript en todas las sesiones activas.
     */
    fun setJavaScriptEnabled(enabled: Boolean) {
        isJavaScriptEnabled = enabled
        sessions.values.forEach { session ->
            session.settings.allowJavascript = enabled
        }
    }

    /**
     * Sincroniza dinámicamente la protección contra rastreo (Do Not Track) en todas las sesiones activas.
     */
    fun setDoNotTrackEnabled(enabled: Boolean) {
        isDoNotTrackEnabled = enabled
        sessions.values.forEach { session ->
            session.settings.useTrackingProtection = enabled
        }
    }

    /**
     * Actualiza el modo de visualización de escritorio para una pestaña específica.
     */
    fun setDesktopModeForTab(tabId: Long, enabled: Boolean) {
        sessions[tabId]?.let { session ->
            session.settings.userAgentMode = if (enabled) {
                GeckoSessionSettings.USER_AGENT_MODE_DESKTOP
            } else {
                GeckoSessionSettings.USER_AGENT_MODE_MOBILE
            }
            session.settings.viewportMode = if (enabled) {
                GeckoSessionSettings.VIEWPORT_MODE_DESKTOP
            } else {
                GeckoSessionSettings.VIEWPORT_MODE_MOBILE
            }
        }
    }

    /**
     * Obtiene una sesión existente o crea y abre una nueva asociada al ID de la pestaña.
     * 
     * @param tabId Identificador único de la pestaña en base de datos Room.
     * @param isIncognito Indica si la pestaña debe ejecutarse en modo incógnito/privado.
     * @param isDesktopMode Indica si debe iniciar con User-Agent de escritorio.
     * @return Instancia activa de [GeckoSession].
     */
    fun getOrCreateSession(
        tabId: Long,
        isIncognito: Boolean,
        isProtected: Boolean = false,
        contextId: String? = null,
        isDesktopMode: Boolean = false
    ): GeckoSession {
        return sessions.getOrPut(tabId) {
            val builder = GeckoSessionSettings.Builder()
                .usePrivateMode(isIncognito)
                .userAgentMode(
                    if (isDesktopMode) GeckoSessionSettings.USER_AGENT_MODE_DESKTOP
                    else GeckoSessionSettings.USER_AGENT_MODE_MOBILE
                )
                .viewportMode(
                    if (isDesktopMode) GeckoSessionSettings.VIEWPORT_MODE_DESKTOP
                    else GeckoSessionSettings.VIEWPORT_MODE_MOBILE
                )
                .useTrackingProtection(isDoNotTrackEnabled)
                .allowJavascript(isJavaScriptEnabled)

            // Si es una pestaña protegida, asignamos un contexto aislado de identidad
            if (isProtected) {
                val effectiveContextId = contextId ?: "isolated_tab_$tabId"
                builder.contextId(effectiveContextId)
            }

            val session = GeckoSession(builder.build())
            session.open(runtime)
            session
        }
    }

    /**
     * Cierra y libera los recursos de una sesión específica cuando se cierra su pestaña.
     * 
     * @param tabId Identificador de la pestaña a cerrar.
     */
    fun closeSession(tabId: Long, isProtected: Boolean = false, contextId: String? = null) {
        sessions.remove(tabId)?.let { session ->
            if (session.isOpen) {
                session.close()
            }
        }
        if (isProtected) {
            val ctx = contextId ?: "isolated_tab_$tabId"
            try {
                runtime.storageController.clearDataForSessionContext(ctx)
            } catch (e: Throwable) {
                // Manejo preventivo
            }
        }
    }

    /**
     * Cierra todas las sesiones abiertas al salir o limpiar el navegador.
     */
    fun closeAllSessions() {
        sessions.values.forEach { session ->
            if (session.isOpen) {
                session.close()
            }
        }
        sessions.clear()
    }
}

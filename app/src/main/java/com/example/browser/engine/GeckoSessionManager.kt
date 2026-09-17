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
        val existing = sessions[tabId]
        if (existing != null && existing.isOpen) {
            return existing
        }
        // Si existía pero la sesión nativa fue cerrada o invalidada, purgar la referencia
        if (existing != null) {
            sessions.remove(tabId)
        }

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
            .useTrackingProtection(isDoNotTrackEnabled || isIncognito)
            .allowJavascript(isJavaScriptEnabled)

        // Dejar que GeckoView envíe su User-Agent nativo sincronizado con el motor
        // para evitar banderas de discrepancia en sistemas de protección contra bots

        // Si es una pestaña protegida, asignamos un contexto aislado de identidad
        if (isProtected) {
            val effectiveContextId = contextId ?: "isolated_tab_$tabId"
            builder.contextId(effectiveContextId)
        }

        val session = GeckoSession(builder.build())
        session.open(runtime)
        sessions[tabId] = session
        return session
    }

    /**
     * Cierra y libera los recursos de una sesión específica cuando se cierra su pestaña.
     * Si es incógnito, purga de inmediato la memoria RAM y cachés volátiles.
     * 
     * @param tabId Identificador de la pestaña a cerrar.
     */
    fun closeSession(
        tabId: Long,
        isIncognito: Boolean = false,
        isProtected: Boolean = false,
        contextId: String? = null
    ) {
        sessions.remove(tabId)?.let { session ->
            if (session.isOpen) {
                try {
                    session.close()
                } catch (_: Throwable) {}
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
        if (isIncognito) {
            // Purga de RAM y cachés al cerrar pestaña de incógnito
            purgeIncognitoMemory()
        }
    }

    /**
     * Purga agresiva de memoria RAM al destruir pestañas o salir de incógnito.
     */
    fun purgeIncognitoMemory() {
        GeckoRuntimeProvider.purgeIncognitoMemory(context)
    }

    /**
     * Duerme o hiberna una sesión de pestaña tras un periodo de inactividad (5 minutos).
     * Cierra la sesión en memoria para liberar recursos del teléfono, manteniendo intactos
     * los datos de almacenamiento, cookies y estado persistido para cuando vuelva a abrirse.
     */
    fun hibernateSession(tabId: Long) {
        sessions.remove(tabId)?.let { session ->
            if (session.isOpen) {
                try {
                    session.close()
                } catch (_: Throwable) {}
            }
        }
    }

    /**
     * Indica si una pestaña tiene actualmente una sesión viva y abierta en la memoria de GeckoView.
     */
    fun isSessionLoaded(tabId: Long): Boolean = sessions[tabId]?.isOpen == true

    /**
     * Hiberna todas las pestañas secundarias en segundo plano, protegiendo la sesión de la pestaña activa en pantalla.
     */
    fun hibernateInactiveSessions(activeTabId: Long?) {
        sessions.keys.forEach { tabId ->
            if (tabId != activeTabId) {
                hibernateSession(tabId)
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

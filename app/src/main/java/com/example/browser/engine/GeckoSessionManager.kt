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
        isDesktopMode: Boolean = false
    ): GeckoSession {
        return sessions.getOrPut(tabId) {
            val settings = GeckoSessionSettings.Builder()
                .usePrivateMode(isIncognito)
                .userAgentMode(
                    if (isDesktopMode) GeckoSessionSettings.USER_AGENT_MODE_DESKTOP
                    else GeckoSessionSettings.USER_AGENT_MODE_MOBILE
                )
                .viewportMode(
                    if (isDesktopMode) GeckoSessionSettings.VIEWPORT_MODE_DESKTOP
                    else GeckoSessionSettings.VIEWPORT_MODE_MOBILE
                )
                .useTrackingProtection(true)
                .build()

            val session = GeckoSession(settings)
            session.open(runtime)
            session
        }
    }

    /**
     * Cierra y libera los recursos de una sesión específica cuando se cierra su pestaña.
     * 
     * @param tabId Identificador de la pestaña a cerrar.
     */
    fun closeSession(tabId: Long) {
        sessions.remove(tabId)?.let { session ->
            if (session.isOpen) {
                session.close()
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

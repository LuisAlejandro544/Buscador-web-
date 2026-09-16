package com.example.browser.lifecycle

import android.content.Context
import android.util.Log
import com.example.browser.download.DownloadEngine
import com.example.browser.engine.GeckoRuntimeProvider
import com.example.browser.engine.GeckoSessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Gestor inteligente del ciclo de vida, ahorro de memoria RAM e hibernación del Navegador.
 *
 * Implementa las reglas de optimización de recursos:
 * 1. Al salir el usuario de la aplicación (segundo plano), inicia una cuenta regresiva
 *    secreta de 20 segundos.
 * 2. Si el usuario vuelve a entrar antes de cumplirse los 20 segundos, se cancela la cuenta
 *    y todo continúa instantáneamente sin interrupción.
 * 3. Si pasan los 20 segundos sin retorno del usuario: la aplicación entra en modo
 *    hibernación profunda: se liberan las sesiones pesadas de GeckoView y cachés de renderizado,
 *    dejando únicamente los receptores mínimos para notificaciones web y servicios esenciales.
 * 4. Si hay descargas activas en curso: la aplicación mantiene activa únicamente la memoria
 *    estrictamente requerida para el streaming de descarga. Una vez completadas todas las descargas,
 *    se activa automáticamente la cuenta regresiva de 20 segundos si el usuario sigue fuera de la app.
 */
object AppHibernationManager {

    private const val TAG = "AppHibernationManager"
    private const val HIBERNATION_DELAY_MS = 20_000L // 20 segundos de cuenta regresiva

    private val scope = CoroutineScope(Dispatchers.Default)
    private var countdownJob: Job? = null

    // Estado observable de si la app se encuentra en hibernación profunda
    private val _isHibernated = MutableStateFlow(false)
    val isHibernated: StateFlow<Boolean> = _isHibernated.asStateFlow()

    // Control de si la interfaz de usuario está en primer plano
    @Volatile
    private var isAppInForeground = false

    // Referencia al SessionManager para liberar sesiones en hibernación
    private var sessionManager: GeckoSessionManager? = null

    /**
     * Inicializa o registra el gestor de sesiones para poder liberar memoria en hibernación.
     */
    fun registerSessionManager(manager: GeckoSessionManager) {
        sessionManager = manager
    }

    /**
     * Invocado cuando la actividad principal pasa a primer plano (onStart / onResume).
     * Cancela cualquier cuenta regresiva activa y despierta el motor si estaba en hibernación.
     */
    fun onAppForegrounded(context: Context) {
        isAppInForeground = true
        cancelCountdown()

        if (_isHibernated.value) {
            Log.d(TAG, "Restaurando aplicación desde hibernación profunda (usuario regresó).")
            _isHibernated.value = false
        }
    }

    /**
     * Invocado cuando la actividad principal pasa a segundo plano (onStop).
     */
    fun onAppBackgrounded(context: Context) {
        isAppInForeground = false

        val hasActiveDownloads = DownloadEngine.hasActiveDownloads()
        if (hasActiveDownloads) {
            Log.d(TAG, "App en segundo plano pero con descargas activas. Optimizando RAM de Gecko y esperando finalización.")
            // Reducir la memoria consumida por pestañas web para dar prioridad total al proceso de descarga
            trimGeckoMemory(context)
        } else {
            startHibernationCountdown(context)
        }
    }

    /**
     * Notificación emitida por el motor de descargas cuando una descarga finaliza (éxito, error o cancelación).
     */
    fun onDownloadFinished(context: Context) {
        if (!isAppInForeground && !DownloadEngine.hasActiveDownloads()) {
            Log.d(TAG, "Todas las descargas han concluido y el usuario está fuera de la app. Iniciando cuenta regresiva de 20 segundos.")
            startHibernationCountdown(context)
        }
    }

    /**
     * Inicia la cuenta regresiva secreta de 20 segundos para entrar en hibernación.
     */
    @Synchronized
    private fun startHibernationCountdown(context: Context) {
        countdownJob?.cancel()
        countdownJob = scope.launch {
            Log.d(TAG, "Iniciando cuenta regresiva secreta de 20 segundos para hibernación...")
            delay(HIBERNATION_DELAY_MS)

            // Si tras 20 segundos el usuario aún no regresa y no hay descargas activas
            if (!isAppInForeground && !DownloadEngine.hasActiveDownloads()) {
                enterDeepHibernation(context)
            }
        }
    }

    /**
     * Cancela la cuenta regresiva de hibernación.
     */
    @Synchronized
    private fun cancelCountdown() {
        countdownJob?.cancel()
        countdownJob = null
    }

    /**
     * Aplica la hibernación profunda: libera recursos y minimiza el uso de RAM al extremo.
     */
    private fun enterDeepHibernation(context: Context) {
        Log.d(TAG, "Ejecutando hibernación profunda: durmiendo sesiones web y liberando memoria RAM.")
        _isHibernated.value = true

        try {
            // Dormir sesiones pesadas de GeckoView abiertas en memoria
            sessionManager?.closeAllSessions()

            // Purgar cachés temporales y solicitar recolección de basura
            GeckoRuntimeProvider.purgeIncognitoMemory(context)

            System.gc()
            Runtime.getRuntime().gc()
        } catch (e: Exception) {
            Log.e(TAG, "Error durante hibernación profunda", e)
        }
    }

    /**
     * Purga memoria no esencial de Gecko mientras se mantiene una descarga en curso.
     */
    private fun trimGeckoMemory(context: Context) {
        try {
            GeckoRuntimeProvider.purgeIncognitoMemory(context)
        } catch (_: Exception) {}
    }
}

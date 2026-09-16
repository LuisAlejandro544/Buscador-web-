package com.example.browser.engine

import android.content.Context
import org.mozilla.geckoview.ContentBlocking
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.StorageController

/**
 * Proveedor singleton del entorno de ejecución GeckoRuntime de Mozilla.
 * 
 * Centraliza la inicialización de GeckoView en un único proceso para optimizar
 * el consumo de memoria RAM y garantizar un arranque rápido en dispositivos móviles.
 * 
 * Configura por defecto:
 * - Protección contra rastreo mejorada (ETP Estricto).
 * - Bloqueo de rastreadores conocidos y cookies de terceros invasivas.
 * - Soporte para páginas de configuración interna (about:config).
 * - Control centralizado de almacenamiento, caché y cookies.
 */
object GeckoRuntimeProvider {

    @Volatile
    private var instance: GeckoRuntime? = null

    /**
     * Obtiene o inicializa la instancia compartida de GeckoRuntime.
     * 
     * @param context Contexto de la aplicación Android.
     * @return Instancia única configurada de GeckoRuntime.
     */
    fun get(context: Context): GeckoRuntime {
        return instance ?: synchronized(this) {
            instance ?: createRuntime(context.applicationContext).also {
                instance = it
            }
        }
    }

    /**
     * Construye la configuración de alto rendimiento y privacidad para GeckoView.
     */
    private fun createRuntime(appContext: Context): GeckoRuntime {
        val contentBlockingSettings = ContentBlocking.Settings.Builder()
            .antiTracking(ContentBlocking.AntiTracking.STRICT)
            .enhancedTrackingProtectionLevel(ContentBlocking.EtpLevel.STRICT)
            .cookieBehavior(ContentBlocking.CookieBehavior.ACCEPT_FIRST_PARTY)
            .build()

        val runtimeSettings = GeckoRuntimeSettings.Builder()
            .contentBlocking(contentBlockingSettings)
            .aboutConfigEnabled(true)
            .build()

        return GeckoRuntime.create(appContext, runtimeSettings)
    }

    /**
     * Limpia la memoria caché y datos de navegación almacenados por GeckoView.
     * 
     * @param context Contexto para acceder al runtime.
     * @param onComplete Callback opcional al finalizar la limpieza.
     */
    fun clearAllData(context: Context, onComplete: (() -> Unit)? = null) {
        val runtime = get(context)
        runtime.storageController.clearData(StorageController.ClearFlags.ALL)
        onComplete?.invoke()
    }
}

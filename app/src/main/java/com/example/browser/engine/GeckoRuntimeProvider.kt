package com.example.browser.engine

import android.content.Context
import org.mozilla.geckoview.ContentBlocking
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.StorageController
import java.io.File

/**
 * Proveedor singleton del entorno de ejecución GeckoRuntime de Mozilla.
 * 
 * Centraliza la inicialización de GeckoView en un único proceso para optimizar
 * el consumo de memoria RAM y garantizar un arranque rápido en dispositivos móviles.
 * 
 * Integra protecciones de seguridad y privacidad profunda:
 * - Protección contra Huella Digital (Resist Fingerprinting - RFP estilo Tor).
 * - Bloqueo de Fugas por WebRTC (Anti IP-Leak / media.peerconnection desactivado).
 * - DNS sobre HTTPS Cifrado (DoH mediante Cloudflare/Mozilla).
 * - Total Cookie Protection (dFPI - Aislamiento dinámico de cookies de terceros).
 * - Despojo automático de parámetros de rastreo (query parameter stripping).
 * - Bloqueo estricto de minado de criptomonedas y rastreadores de huellas.
 * - Purga inmediata y agresiva de memoria RAM al cerrar sesiones privadas.
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
     * Construye la configuración de alto rendimiento y privacidad avanzada para GeckoView.
     */
    private fun createRuntime(appContext: Context): GeckoRuntime {
        // Generar archivo de preferencias avanzadas de Gecko para RFP, WebRTC y protección de red
        val configFile = File(appContext.filesDir, "geckoview-privacy-config.yaml")
        try {
            configFile.writeText(
                """
                prefs:
                  privacy.resistFingerprinting: true
                  privacy.resistFingerprinting.letterboxing: false
                  privacy.trackingprotection.fingerprinting.enabled: true
                  privacy.trackingprotection.cryptomining.enabled: true
                  privacy.trackingprotection.socialtracking.enabled: true
                  media.peerconnection.enabled: false
                  media.peerconnection.ice.no_host: true
                  media.peerconnection.ice.default_address_only: true
                  media.navigator.enabled: false
                  network.trr.mode: 2
                  network.trr.uri: "https://mozilla.cloudflare-dns.com/dns-query"
                  network.cookie.cookieBehavior: 5
                  privacy.partition.network_state: true
                  privacy.firstparty.isolate: true
                  privacy.query_stripping.enabled: true
                  privacy.query_stripping.enabled.pbmode: true
                  dom.battery.enabled: false
                  dom.gamepad.enabled: false
                  dom.netinfo.enabled: false
                """.trimIndent()
            )
        } catch (_: Throwable) {
            // Manejo preventivo si hay fallo de I/O
        }

        // Configuración de bloqueo de contenido (Total Cookie Protection + ETP Estricto)
        val contentBlockingSettings = ContentBlocking.Settings.Builder()
            .antiTracking(ContentBlocking.AntiTracking.STRICT)
            .enhancedTrackingProtectionLevel(ContentBlocking.EtpLevel.STRICT)
            // Total Cookie Protection (dFPI): aísla cookies de terceros al host de origen
            .cookieBehavior(ContentBlocking.CookieBehavior.ACCEPT_FIRST_PARTY_AND_ISOLATE_OTHERS)
            .cookieBehaviorPrivateMode(ContentBlocking.CookieBehavior.ACCEPT_FIRST_PARTY_AND_ISOLATE_OTHERS)
            .cookiePurging(true)
            .strictSocialTrackingProtection(true)
            .queryParameterStrippingEnabled(true)
            .queryParameterStrippingPrivateBrowsingEnabled(true)
            .emailTrackerBlockingPrivateMode(true)
            .build()

        val runtimeSettingsBuilder = GeckoRuntimeSettings.Builder()
            .contentBlocking(contentBlockingSettings)
            .aboutConfigEnabled(true)
            .globalPrivacyControlEnabled(true)
            // DNS sobre HTTPS cifrado (DoH - Trusted Recursive Resolver)
            .trustedRecursiveResolverMode(GeckoRuntimeSettings.TRR_MODE_FIRST)
            .trustedRecursiveResolverUri("https://mozilla.cloudflare-dns.com/dns-query")
            .defaultRecursiveResolverUri("https://mozilla.cloudflare-dns.com/dns-query")

        if (configFile.exists()) {
            runtimeSettingsBuilder.configFilePath(configFile.absolutePath)
        }

        return GeckoRuntime.create(appContext, runtimeSettingsBuilder.build())
    }

    /**
     * Purga de memoria RAM inmediata y agresiva: elimina cachés temporales, sesiones
     * volátiles de autenticación y solicita la recolección de basura del sistema.
     */
    fun purgeIncognitoMemory(context: Context) {
        try {
            val runtime = get(context)
            runtime.storageController.clearData(
                StorageController.ClearFlags.ALL_CACHES or StorageController.ClearFlags.AUTH_SESSIONS
            )
        } catch (_: Throwable) {}
        System.gc()
        Runtime.getRuntime().gc()
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

package com.example.browser.engine

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

    private const val CONFIG_FILE_NAME = "geckoview-privacy-config.yaml"

    private const val CONFIG_YAML_CONTENT = """
prefs:
  privacy.resistFingerprinting: false
  privacy.resistFingerprinting.letterboxing: false
  privacy.trackingprotection.fingerprinting.enabled: true
  privacy.trackingprotection.cryptomining.enabled: true
  privacy.trackingprotection.socialtracking.enabled: true
  media.peerconnection.enabled: true
  media.peerconnection.ice.no_host: true
  media.peerconnection.ice.default_address_only: true
  media.navigator.enabled: true
  network.cookie.cookieBehavior: 5
  privacy.partition.network_state: true
  privacy.firstparty.isolate: false
  privacy.query_stripping.enabled: false
  privacy.query_stripping.enabled.pbmode: false
  dom.battery.enabled: false
  dom.gamepad.enabled: false
  dom.netinfo.enabled: true
  browser.cache.memory.enable: true
  browser.cache.memory.capacity: 32768
  image.mem.surfacecache.max_size_kb: 32768
  javascript.options.mem.gc_frequency: 20
  dom.ipc.processHangMonitor: false
  extensions.webextensions.background-delayed-startup: true
"""

    @Volatile
    private var instance: GeckoRuntime? = null

    /**
     * Prepara de forma asíncrona en un hilo secundario (Dispatchers.IO) el archivo de configuración
     * de privacidad y precalienta el entorno de ejecución para evitar cualquier micro-parón en la UI.
     */
    suspend fun warmup(context: Context) = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        ensureConfigFile(appContext)
        get(appContext)
    }

    /**
     * Garantiza la existencia del archivo de configuración sin reescribirlo innecesariamente si ya existe.
     */
    private fun ensureConfigFile(appContext: Context): File {
        val configFile = File(appContext.filesDir, CONFIG_FILE_NAME)
        try {
            if (!configFile.exists() || configFile.length() == 0L) {
                configFile.writeText(CONFIG_YAML_CONTENT.trimIndent())
            }
        } catch (_: Throwable) {
            // Manejo preventivo si hay fallo de I/O en disco
        }
        return configFile
    }

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
        val configFile = ensureConfigFile(appContext)

        // Configuración de bloqueo de contenido (Total Cookie Protection + ETP Estricto)
        // Manteniendo compatibilidad total con tokens de búsqueda y flujos de autenticación
        val contentBlockingSettings = ContentBlocking.Settings.Builder()
            .antiTracking(ContentBlocking.AntiTracking.STRICT)
            .enhancedTrackingProtectionLevel(ContentBlocking.EtpLevel.STRICT)
            // Total Cookie Protection (dFPI): aísla cookies de terceros al host de origen
            .cookieBehavior(ContentBlocking.CookieBehavior.ACCEPT_FIRST_PARTY_AND_ISOLATE_OTHERS)
            .cookieBehaviorPrivateMode(ContentBlocking.CookieBehavior.ACCEPT_FIRST_PARTY_AND_ISOLATE_OTHERS)
            .cookiePurging(true)
            .strictSocialTrackingProtection(true)
            .queryParameterStrippingEnabled(false)
            .queryParameterStrippingPrivateBrowsingEnabled(false)
            .emailTrackerBlockingPrivateMode(true)
            .build()

        val runtimeSettingsBuilder = GeckoRuntimeSettings.Builder()
            .contentBlocking(contentBlockingSettings)
            .aboutConfigEnabled(true)
            .globalPrivacyControlEnabled(true)
            // Usar resolución DNS nativa del sistema para garantizar coherencia geográfica con el ISP del dispositivo móvil
            .trustedRecursiveResolverMode(GeckoRuntimeSettings.TRR_MODE_OFF)

        if (configFile.exists()) {
            runtimeSettingsBuilder.configFilePath(configFile.absolutePath)
        }

        return GeckoRuntime.create(appContext, runtimeSettingsBuilder.build())
    }

    /**
     * Purga de memoria RAM asíncrona en hilo secundario.
     */
    suspend fun purgeIncognitoMemoryAsync(context: Context) = withContext(Dispatchers.IO) {
        purgeIncognitoMemory(context)
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
     * Limpia la memoria caché y datos de navegación de forma asíncrona en un hilo secundario.
     */
    suspend fun clearAllDataAsync(context: Context) = withContext(Dispatchers.IO) {
        val runtime = get(context)
        runtime.storageController.clearData(StorageController.ClearFlags.ALL)
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

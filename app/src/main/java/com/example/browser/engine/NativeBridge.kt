package com.example.browser.engine

import android.util.Log

/**
 * Puente JNI seguro entre Kotlin y la capa nativa (C++26 / CMake / Rust).
 * 
 * Permite acceder a funciones de bajo nivel compiladas con el NDK
 * para arquitecturas arm64-v8a, armeabi-v7a, x86 y x86_64.
 *
 * Incluye fallback seguro para entornos de prueba donde las librerías .so
 * no están vinculadas dinámicamente.
 */
object NativeBridge {
    private const val TAG = "NativeBridge"

    private var isLoaded = false

    init {
        try {
            System.loadLibrary("browser_native")
            isLoaded = true
            runCatching { Log.i(TAG, "Librería nativa libbrowser_native.so cargada correctamente.") }
        } catch (e: UnsatisfiedLinkError) {
            runCatching { Log.w(TAG, "No se pudo cargar libbrowser_native.so (normal en tests de JVM): ${e.message}") }
            isLoaded = false
        } catch (e: Throwable) {
            runCatching { Log.e(TAG, "Error inesperado cargando subsistema nativo: ${e.message}") }
            isLoaded = false
        }
    }

    /**
     * Indica si la librería nativa está cargada y lista para llamadas JNI.
     */
    fun isAvailable(): Boolean = isLoaded

    /**
     * Retorna la versión del subsistema nativo C++26 / Rust.
     */
    fun getVersion(): String {
        return if (isLoaded) {
            try {
                getNativeVersion()
            } catch (e: Throwable) {
                "Native Error: ${e.message}"
            }
        } else {
            "Native Library Not Loaded (JVM/Mock Mode)"
        }
    }

    /**
     * Calcula un hash de 64 bits para una URL dada.
     * Si la librería nativa no está disponible, utiliza una implementación en Kotlin como respaldo.
     */
    fun hashUrl(url: String): Long {
        return if (isLoaded) {
            try {
                hashUrlNative(url)
            } catch (e: Throwable) {
                fallbackHash(url)
            }
        } else {
            fallbackHash(url)
        }
    }

    /**
     * Algoritmo FNV-1a en Kotlin para fallback seguro en entornos sin runtime JNI.
     */
    private fun fallbackHash(input: String): Long {
        var hash = 0xcbf29ce484222325UL.toLong()
        for (char in input) {
            hash = hash xor (char.code.toLong() and 0xffL)
            hash *= 0x100000001b3L
        }
        return hash
    }

    // Declaraciones de métodos nativos en C++ (app/src/main/cpp/native-bridge.cpp)
    private external fun getNativeVersion(): String
    private external fun hashUrlNative(url: String): Long
    private external fun isNativeReady(): Boolean
    private external fun isFilterEngineReady(): Boolean
    private external fun addFilterRulesNative(rules: String): Int
    private external fun shouldBlockUrlNative(url: String, sourceUrl: String?, requestType: String?): Boolean
    private external fun getFilterStatsNative(): String

    /**
     * Consulta si el motor de filtrado nativo (Rust adblock) está disponible y listo.
     */
    fun isFilterReady(): Boolean {
        return if (isLoaded) {
            try {
                isFilterEngineReady()
            } catch (_: Throwable) {
                FallbackFilterEngine.isReady
            }
        } else {
            FallbackFilterEngine.isReady
        }
    }

    /**
     * Determina si una petición a una URL específica debe ser bloqueada
     * por coincidencia con listas de filtros (publicidad, rastreadores, telemetría).
     */
    fun shouldBlockUrl(url: String, sourceUrl: String = "", requestType: String = "other"): Boolean {
        if (url.isBlank()) return false
        return if (isLoaded) {
            try {
                shouldBlockUrlNative(url, sourceUrl, requestType)
            } catch (_: Throwable) {
                FallbackFilterEngine.shouldBlock(url)
            }
        } else {
            FallbackFilterEngine.shouldBlock(url)
        }
    }

    /**
     * Incorpora nuevas reglas de filtrado en formato estándar (EasyList / ABP / Hosts)
     * al motor nativo o al motor de respaldo.
     */
    fun addFilterRules(rules: String): Int {
        if (rules.isBlank()) return 0
        val count = if (isLoaded) {
            try {
                addFilterRulesNative(rules)
            } catch (_: Throwable) {
                FallbackFilterEngine.addRules(rules)
            }
        } else {
            FallbackFilterEngine.addRules(rules)
        }
        FallbackFilterEngine.addRules(rules)
        return count
    }

    /**
     * Obtiene el resumen de métricas del motor de filtrado.
     */
    fun getFilterStats(): String {
        return if (isLoaded) {
            try {
                getFilterStatsNative()
            } catch (_: Throwable) {
                FallbackFilterEngine.getStatsJson()
            }
        } else {
            FallbackFilterEngine.getStatsJson()
        }
    }

    /**
     * Motor de filtrado de respaldo en Kotlin para entornos donde la biblioteca nativa
     * .so no esté cargada o en pruebas unitarias JVM / Robolectric.
     */
    object FallbackFilterEngine {
        var isReady = true
        private val blockedDomains = mutableSetOf(
            "doubleclick.net",
            "google-analytics.com",
            "googlesyndication.com",
            "googleadservices.com",
            "adservice.google.com",
            "facebook.com/tr",
            "outbrain.com",
            "taboola.com",
            "adnxs.com",
            "rubiconproject.com",
            "criteo.com",
            "scorecardresearch.com",
            "quantserve.com",
            "hotjar.com",
            "chartbeat.com"
        )
        private var blockedCount = 0L
        private var allowedCount = 0L

        fun shouldBlock(url: String): Boolean {
            val lower = url.lowercase()
            for (domain in blockedDomains) {
                if (lower.contains(domain)) {
                    blockedCount++
                    return true
                }
            }
            allowedCount++
            return false
        }

        fun addRules(rules: String): Int {
            var count = 0
            rules.lineSequence().forEach { rawLine ->
                val line = rawLine.trim()
                if (line.isNotEmpty() && !line.startsWith("!") && !line.startsWith("#")) {
                    val clean = line.removePrefix("||").removeSuffix("^").trim()
                    if (clean.isNotEmpty()) {
                        blockedDomains.add(clean.lowercase())
                        count++
                    }
                }
            }
            return blockedDomains.size
        }

        fun getStatsJson(): String {
            return """{"rules_count":${blockedDomains.size},"blocked_count":$blockedCount,"allowed_count":$allowedCount,"is_ready":true,"version":"Fallback-Filter-v1.0"}"""
        }
    }
}

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
            Log.i(TAG, "Librería nativa libbrowser_native.so cargada correctamente.")
        } catch (e: UnsatisfiedLinkError) {
            Log.w(TAG, "No se pudo cargar libbrowser_native.so (normal en tests de JVM): ${e.message}")
            isLoaded = false
        } catch (e: Throwable) {
            Log.e(TAG, "Error inesperado cargando subsistema nativo: ${e.message}")
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
}

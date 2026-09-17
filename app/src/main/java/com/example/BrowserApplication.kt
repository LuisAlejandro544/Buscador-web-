package com.example

import android.app.Application
import android.content.Intent
import android.util.Log
import com.example.browser.debug.CrashLogManager
import com.example.ui.debug.CrashInspectorActivity
import com.github.anrwatchdog.ANRWatchDog
import kotlin.system.exitProcess

/**
 * Clase principal de aplicación del Navegador Web.
 *
 * Configura los interceptores globales de diagnósticos y estabilidad:
 * 1. **Crash Inspector Propio:** `UncaughtExceptionHandler` para capturar excepciones fatales de Kotlin/Java,
 *    persistir el informe con memoria y stacktrace, y abrir de inmediato la pantalla de depuración.
 * 2. **ANR-WatchDog:** Monitoreo en segundo plano que detecta congelamientos del hilo principal (> 5 segundos)
 *    y los archiva en el historial de incidentes.
 * 3. **LeakCanary:** Vigilancia continua de fugas de memoria en Activities, Fragments y sesiones web.
 */
class BrowserApplication : Application() {

    companion object {
        private const val TAG = "BrowserApplication"
        private const val ANR_TIMEOUT_MS = 5000
    }

    override fun onCreate() {
        super.onCreate()
        setupCrashHandler()
        setupAnrWatchdog()
    }

    /**
     * Configura el interceptor global de excepciones no controladas.
     * En lugar de cerrarse de forma silenciosa, guarda el reporte y abre la actividad de diagnóstico.
     */
    private fun setupCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "Excepción no controlada en hilo '${thread.name}': ${throwable.message}", throwable)

            try {
                // Registrar el incidente de forma persistente en almacenamiento local
                CrashLogManager.recordException(this, throwable)

                // Abrir la pantalla de Crash Inspector en una nueva tarea limpia
                val intent = Intent(this, CrashInspectorActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    putExtra(CrashInspectorActivity.EXTRA_SHOW_LATEST, true)
                }
                startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error durante la gestión del cierre inesperado: ${e.message}", e)
                defaultHandler?.uncaughtException(thread, throwable)
            }

            // Terminar el proceso que colapsó para evitar estados corruptos
            android.os.Process.killProcess(android.os.Process.myPid())
            exitProcess(10)
        }
    }

    /**
     * Inicia ANR-WatchDog con un intervalo de 5 segundos para detectar bloqueos de la UI.
     */
    private fun setupAnrWatchdog() {
        try {
            ANRWatchDog(ANR_TIMEOUT_MS)
                .setANRListener { error ->
                    Log.w(TAG, "ANR Detectado por ANR-WatchDog: ${error.message}", error)
                    CrashLogManager.recordAnr(this, error)
                }
                .start()
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo iniciar ANR-WatchDog: ${e.message}", e)
        }
    }
}

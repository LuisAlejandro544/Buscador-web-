package com.example.browser.debug

import android.app.ActivityManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Gestor centralizado de diagnósticos y registro persistente de fallos en el dispositivo móvil.
 *
 * Captura excepciones no controladas de Kotlin/Java, alertas de congelamiento (ANR de ANR-WatchDog)
 * y caídas o terminaciones por falta de memoria (OOM) en los procesos hijos de GeckoView.
 *
 * Almacena los informes en un archivo JSON interno seguro en almacenamiento privado (`filesDir/crash_reports.json`)
 * manteniendo un historial de los últimos 50 incidentes listos para copiar al portapapeles.
 */
object CrashLogManager {

    private const val TAG = "CrashLogManager"
    private const val CRASH_FILE_NAME = "crash_reports.json"
    private const val MAX_REPORTS = 50

    @Volatile
    private var currentNavigatingUrl: String? = null

    /**
     * Actualiza la URL actualmente activa en el navegador para enriquecer los informes de fallo.
     */
    fun updateCurrentUrl(url: String?) {
        currentNavigatingUrl = url
    }

    /**
     * Registra una excepción imprevista de Kotlin o Java capturada por el UncaughtExceptionHandler.
     */
    fun recordException(context: Context, throwable: Throwable): CrashReport {
        val (availMb, totalMb, isLow) = getMemoryInfo(context)
        val stackTrace = getStackTraceString(throwable)
        val title = throwable.javaClass.simpleName.ifBlank { "Exception" }
        val message = throwable.message ?: "Sin mensaje explicativo"

        val report = CrashReport(
            type = CrashType.UNCAUGHT_EXCEPTION,
            title = title,
            message = message,
            stackTrace = stackTrace,
            deviceModel = getDeviceModel(),
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            availableRamMb = availMb,
            totalRamMb = totalMb,
            isLowMemory = isLow,
            urlAtCrash = currentNavigatingUrl
        )

        saveReport(context, report)
        return report
    }

    /**
     * Registra una caída del proceso de renderizado web de GeckoView o una matanza por OOM.
     */
    fun recordGeckoCrash(
        context: Context,
        url: String?,
        isOom: Boolean,
        consecutiveCount: Int
    ): CrashReport {
        val (availMb, totalMb, isLow) = getMemoryInfo(context)
        val type = if (isOom) CrashType.GECKO_OOM else CrashType.GECKO_TAB_CRASH

        val title = if (isOom) {
            "Terminación por Presión de RAM (GeckoView OOM Kill)"
        } else {
            "Caída del Proceso Hijo de Pestaña GeckoView"
        }

        val message = if (isOom) {
            "El sistema operativo Android terminó el proceso de renderizado de GeckoView debido a falta crítica de memoria RAM. Reintentos consecutivos: $consecutiveCount."
        } else {
            "El motor de renderizado GeckoView se cerró de forma inesperada mientras procesaba la página web. Reintentos consecutivos: $consecutiveCount."
        }

        val stackTrace = buildString {
            appendLine("Evento registrado por GeckoSession.ContentDelegate.${if (isOom) "onKill" else "onCrash"}")
            appendLine("Intentos consecutivos de recuperación: $consecutiveCount")
            appendLine("URL objetivo: ${url ?: "(Desconocida o vacía)"}")
            appendLine("Estado de memoria física del sistema: $availMb MB libres de $totalMb MB.")
            if (isLow) {
                appendLine("¡ADVERTENCIA! El sistema operativo había activado la bandera de memoria baja (lowMemory = true).")
            }
        }

        val report = CrashReport(
            type = type,
            title = title,
            message = message,
            stackTrace = stackTrace,
            deviceModel = getDeviceModel(),
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            availableRamMb = availMb,
            totalRamMb = totalMb,
            isLowMemory = isLow,
            urlAtCrash = url ?: currentNavigatingUrl
        )

        saveReport(context, report)
        return report
    }

    /**
     * Registra un bloqueo de la interfaz (ANR) detectado por ANR-WatchDog.
     */
    fun recordAnr(context: Context, error: Throwable): CrashReport {
        val (availMb, totalMb, isLow) = getMemoryInfo(context)
        val stackTrace = getStackTraceString(error)

        val report = CrashReport(
            type = CrashType.ANR_FREEZE,
            title = "Bloqueo de Interfaz de Usuario (ANR)",
            message = "El hilo principal de la aplicación no respondió durante más de 5 segundos.",
            stackTrace = stackTrace,
            deviceModel = getDeviceModel(),
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            availableRamMb = availMb,
            totalRamMb = totalMb,
            isLowMemory = isLow,
            urlAtCrash = currentNavigatingUrl
        )

        saveReport(context, report)
        return report
    }

    /**
     * Guarda el informe en el archivo persistente JSON local.
     */
    @Synchronized
    private fun saveReport(context: Context, report: CrashReport) {
        try {
            val file = File(context.filesDir, CRASH_FILE_NAME)
            val jsonArray = if (file.exists()) {
                val content = file.readText()
                if (content.isNotBlank()) JSONArray(content) else JSONArray()
            } else {
                JSONArray()
            }

            val obj = JSONObject().apply {
                put("id", report.id)
                put("timestamp", report.timestamp)
                put("type", report.type.name)
                put("title", report.title)
                put("message", report.message)
                put("stackTrace", report.stackTrace)
                put("deviceModel", report.deviceModel)
                put("androidVersion", report.androidVersion)
                put("availableRamMb", report.availableRamMb)
                put("totalRamMb", report.totalRamMb)
                put("isLowMemory", report.isLowMemory)
                put("urlAtCrash", report.urlAtCrash ?: "")
            }

            // Insertar al inicio para orden cronológico descendente
            val newArray = JSONArray()
            newArray.put(obj)
            for (i in 0 until jsonArray.length()) {
                if (newArray.length() >= MAX_REPORTS) break
                newArray.put(jsonArray.getJSONObject(i))
            }

            file.writeText(newArray.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Error persistiendo informe de fallo: ${e.message}", e)
        }
    }

    /**
     * Recupera todos los informes registrados desde el almacenamiento local.
     */
    @Synchronized
    fun getAllReports(context: Context): List<CrashReport> {
        val list = mutableListOf<CrashReport>()
        try {
            val file = File(context.filesDir, CRASH_FILE_NAME)
            if (!file.exists()) return emptyList()

            val content = file.readText()
            if (content.isBlank()) return emptyList()

            val jsonArray = JSONArray(content)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val typeName = obj.optString("type", CrashType.UNCAUGHT_EXCEPTION.name)
                val type = try {
                    CrashType.valueOf(typeName)
                } catch (_: Exception) {
                    CrashType.UNCAUGHT_EXCEPTION
                }

                val report = CrashReport(
                    id = obj.optString("id"),
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                    type = type,
                    title = obj.optString("title", "Error"),
                    message = obj.optString("message", ""),
                    stackTrace = obj.optString("stackTrace", ""),
                    deviceModel = obj.optString("deviceModel", "Desconocido"),
                    androidVersion = obj.optString("androidVersion", ""),
                    availableRamMb = obj.optLong("availableRamMb", 0L),
                    totalRamMb = obj.optLong("totalRamMb", 0L),
                    isLowMemory = obj.optBoolean("isLowMemory", false),
                    urlAtCrash = obj.optString("urlAtCrash").ifBlank { null }
                )
                list.add(report)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error leyendo informes de fallo: ${e.message}", e)
        }
        return list
    }

    /**
     * Elimina todos los informes registrados del historial.
     */
    @Synchronized
    fun clearReports(context: Context): Boolean {
        return try {
            val file = File(context.filesDir, CRASH_FILE_NAME)
            if (file.exists()) file.delete() else true
        } catch (e: Exception) {
            Log.e(TAG, "Error limpiando informes: ${e.message}", e)
            false
        }
    }

    /**
     * Copia un informe específico al portapapeles del sistema operativo Android.
     */
    fun copyToClipboard(context: Context, report: CrashReport): Boolean {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            if (clipboard != null) {
                val clip = ClipData.newPlainText("Crash Report", report.toClipboardText())
                clipboard.setPrimaryClip(clip)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error copiando al portapapeles: ${e.message}", e)
            false
        }
    }

    /**
     * Consulta el estado de memoria RAM física del teléfono mediante ActivityManager.
     */
    private fun getMemoryInfo(context: Context): Triple<Long, Long, Boolean> {
        return try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            am?.getMemoryInfo(memInfo)
            val availMb = memInfo.availMem / (1024 * 1024)
            val totalMb = memInfo.totalMem / (1024 * 1024)
            Triple(availMb, totalMb, memInfo.lowMemory)
        } catch (_: Exception) {
            Triple(0L, 0L, false)
        }
    }

    /**
     * Obtiene el nombre comercial y fabricante del teléfono móvil.
     */
    private fun getDeviceModel(): String {
        val manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
        val model = Build.MODEL
        return if (model.startsWith(manufacturer, ignoreCase = true)) {
            model
        } else {
            "$manufacturer $model"
        }
    }

    /**
     * Convierte una excepción y sus causas anidadas a una cadena legible de texto.
     */
    private fun getStackTraceString(throwable: Throwable): String {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        throwable.printStackTrace(pw)
        return sw.toString()
    }
}

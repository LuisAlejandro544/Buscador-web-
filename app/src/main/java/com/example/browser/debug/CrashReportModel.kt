package com.example.browser.debug

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Categorías de fallos y eventos anómalos detectados por el sistema de diagnóstico.
 */
enum class CrashType(val displayName: String) {
    /** Excepción no controlada en código Kotlin / Java del proceso principal */
    UNCAUGHT_EXCEPTION("Excepción Inesperada (Crash)"),

    /** Caída del proceso de contenido o renderizado de GeckoView */
    GECKO_TAB_CRASH("Caída del Motor GeckoView"),

    /** Matanza del proceso hijo por falta de memoria RAM del sistema (Out Of Memory) */
    GECKO_OOM("Cierre por Falta de RAM (OOM)"),

    /** Congelamiento del hilo principal por más de 5 segundos detectado por ANR-WatchDog */
    ANR_FREEZE("Bloqueo de Interfaz (ANR)")
}

/**
 * Modelo de datos inmutable que almacena la información forense de un error o cierre inesperado.
 *
 * Incluye diagnóstico de memoria física, modelo de hardware, versión del sistema operativo,
 * traza de la pila (Stack Trace) y la URL que se estaba navegando en el momento del incidente.
 */
data class CrashReport(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val type: CrashType,
    val title: String,
    val message: String,
    val stackTrace: String,
    val deviceModel: String,
    val androidVersion: String,
    val availableRamMb: Long,
    val totalRamMb: Long,
    val isLowMemory: Boolean,
    val urlAtCrash: String? = null
) {
    /**
     * Retorna la marca de tiempo formateada para lectura humana en pantallas móviles.
     */
    fun formattedDate(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    /**
     * Genera un informe técnico completo y estructurado en texto plano, ideal para copiar
     * directamente al portapapeles y compartir con desarrolladores.
     */
    fun toClipboardText(): String {
        return buildString {
            appendLine("════════════════════════════════════════════")
            appendLine("📋 INFORME FORENSE DE FALLO - BROWSER DEBUG")
            appendLine("════════════════════════════════════════════")
            appendLine("• Tipo de Incidente: ${type.displayName}")
            appendLine("• Fecha y Hora: ${formattedDate()}")
            appendLine("• Dispositivo: $deviceModel")
            appendLine("• Versión Android: $androidVersion")
            appendLine("• Memoria RAM Libre: $availableRamMb MB de $totalRamMb MB")
            appendLine("• Estado de Memoria Crítica (Low RAM): ${if (isLowMemory) "SÍ (Alerta OOM)" else "NO"}")
            if (!urlAtCrash.isNullOrBlank()) {
                appendLine("• URL Activa en el Momento: $urlAtCrash")
            }
            appendLine("• Resumen del Error: $title")
            appendLine("• Detalle: $message")
            appendLine("────────────────────────────────────────────")
            appendLine("📜 TRAZA DE PILA COMPLETA (STACK TRACE):")
            appendLine("────────────────────────────────────────────")
            appendLine(stackTrace.ifBlank { "(Sin traza disponible)" })
            appendLine("════════════════════════════════════════════")
        }
    }
}

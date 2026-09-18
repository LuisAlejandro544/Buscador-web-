package com.example.browser.debug

/**
 * Categoría de la auditoría forense de privacidad ejecutada.
 */
enum class AuditCategory(val title: String) {
    DATABASE("Base de Datos (Room Leak Check)"),
    MEMORY_LIFECYCLE("Memoria y Ciclo de Vida GeckoView"),
    NETWORK_DOH("Fugas de Red y DNS (DoH/WebRTC)"),
    FINGERPRINT("Huella Digital (RFP Discrepancy)"),
    ISP_LEAK("Fugas hacia Proveedor de Internet (ISP)")
}

/**
 * Estado del resultado de una prueba de auditoría forense.
 */
enum class AuditVerdict {
    PASSED,    // Hermético / 100% Protegido
    WARNING,   // Advertencia menor o discrepancia de configuración
    FAILED,    // Fuga detectada o violación de aislamiento
    RUNNING    // Prueba en ejecución
}

/**
 * Modelo inmutable con los hallazgos forenses de una verificación individual de privacidad.
 */
data class AuditCheckResult(
    val id: String,
    val category: AuditCategory,
    val testName: String,
    val description: String,
    val verdict: AuditVerdict,
    val summary: String,
    val rawDetails: String,
    val timestamp: Long = System.currentTimeMillis()
)

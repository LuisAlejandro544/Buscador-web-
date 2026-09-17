package com.example.model

/**
 * Representa una fuente remota o motor de inteligencia sobre amenazas de seguridad web.
 *
 * Incluye fuentes comunitarias y de ciberseguridad especializadas en:
 * - Phishing y estafas bancarias (PhishTank, OpenPhish, HaGeZi)
 * - Distribución de malware y troyanos (URLhaus de Abuse.ch)
 * - Dominios maliciosos generales y botnets (StevenBlack Hosts)
 */
data class SecurityThreatFeed(
    val id: String,
    val name: String,
    val provider: String,
    val description: String,
    val category: ThreatCategory,
    val feedUrl: String,
    val isEnabledByDefault: Boolean = true
)

/**
 * Clasificación de la amenaza detectada.
 */
enum class ThreatCategory(val displayName: String, val iconDescription: String) {
    PHISHING(
        displayName = "Suplantación de Identidad (Phishing)",
        iconDescription = "Robo de credenciales bancarias o contraseñas"
    ),
    MALWARE(
        displayName = "Distribución de Malware",
        iconDescription = "Virus, troyanos y descargas maliciosas"
    ),
    FRAUD(
        displayName = "Estafa Financiera y Fraude Web",
        iconDescription = "Páginas fraudulentas de comercio o inversión"
    ),
    TRACKER(
        displayName = "Rastreo Extremo y Telemetría",
        iconDescription = "Espionaje de hábitos y huella digital"
    )
}

/**
 * Detalle de una amenaza bloqueada para desplegar en la pantalla de advertencia.
 */
data class BlockedThreatDetail(
    val targetUrl: String,
    val domain: String,
    val category: ThreatCategory,
    val threatSource: String,
    val timestamp: Long = System.currentTimeMillis()
)

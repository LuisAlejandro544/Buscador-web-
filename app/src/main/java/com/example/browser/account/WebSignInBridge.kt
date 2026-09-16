package com.example.browser.account

import com.example.data.local.entity.UserAccountEntity
import org.json.JSONObject
import java.net.URI

/**
 * Solicitud de inicio de sesión web asistido emitida para una página web abierta.
 * 
 * @property domain Dominio del sitio web que solicita o soporta autenticación.
 * @property url URL completa de la página actual.
 * @property account Cuenta de usuario sugerida para iniciar sesión.
 * @property title Título visible en el indicador o banner de acceso rápido.
 */
data class WebSignInPrompt(
    val domain: String,
    val url: String,
    val account: UserAccountEntity,
    val title: String = "Acceder con tu cuenta vinculada"
)

/**
 * Puente de enlace e integración entre las cuentas del navegador y los sitios web.
 * 
 * Detecta solicitudes de login federado (como Google One-Tap o páginas de autenticación),
 * prepara la sugerencia nativa para el usuario y genera scripts seguros para completar
 * el inicio de sesión sin necesidad de ingresar contraseñas manualmente.
 */
object WebSignInBridge {

    /**
     * Proveedores de identidad verificados y rutas de autenticación reconocidas.
     */
    private val trustedAuthHosts = listOf(
        "accounts.google.com",
        "login.microsoftonline.com",
        "appleid.apple.com",
        "github.com"
    )

    private val authPathSegments = listOf(
        "/login",
        "/signin",
        "/sign-in",
        "/oauth",
        "/auth",
        "/session",
        "/iniciar-sesion"
    )

    /**
     * Evalúa si una URL corresponde legítimamente a una página de acceso o autenticación
     * de un proveedor de identidad de confianza comprobada.
     * Exige estrictamente HTTPS y host verificado para prevenir robo de identidad en sitios maliciosos o phishing.
     */
    fun isAuthPage(url: String): Boolean {
        if (url.isBlank() || !url.startsWith("https://", ignoreCase = true)) return false

        return try {
            val uri = URI(url)
            val host = uri.host?.lowercase() ?: return false
            val path = uri.path?.lowercase() ?: ""

            // Coincidencia estricta con proveedores de identidad reconocidos para prevenir ataques de phishing
            val isTrustedHost = trustedAuthHosts.any { host == it || host.endsWith(".$it") }
            if (!isTrustedHost) {
                // Denegado: nunca sugerir credenciales en dominios no verificados ni sitios de terceros
                return false
            }

            // En hosts confiables, verificar si la ruta corresponde a login o si es el portal de acceso
            path.isBlank() || path == "/" || authPathSegments.any { path.contains(it) }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Extrae el dominio legible de una URL web.
     */
    fun extractDomain(url: String): String {
        return try {
            val uri = URI(url)
            uri.host ?: url
        } catch (e: Exception) {
            url
        }
    }

    /**
     * Crea un objeto [WebSignInPrompt] para ser renderizado en la interfaz del navegador.
     */
    fun createPrompt(url: String, account: UserAccountEntity): WebSignInPrompt {
        val domain = extractDomain(url)
        return WebSignInPrompt(
            domain = domain,
            url = url,
            account = account,
            title = "¿Completar acceso en $domain como ${account.displayName}?"
        )
    }

    /**
     * Genera un script JavaScript sanitizado mediante JSONObject.quote() para evitar
     * inyecciones de código (XSS) y proteger los datos del usuario en formularios web.
     * Incluye una verificación estricta de origen (HTTPS y host de confianza) dentro del script
     * para mitigar inyecciones maliciosas si la página sufre redirecciones imprevistas.
     */
    fun generateAutoSignInScript(account: UserAccountEntity): String {
        // Sanitización rigurosa de cadenas mediante JSON estándar
        val quotedEmail = JSONObject.quote(account.email)
        val trustedHostsJson = JSONObject.quote(trustedAuthHosts.joinToString(","))

        return """
            (function() {
                try {
                    // Validar protocolo HTTPS y verificar que el host pertenezca a la lista segura
                    if (window.location.protocol !== 'https:') {
                        console.warn("Auto-signin denegado: solo permitido en conexiones seguras HTTPS.");
                        return;
                    }
                    const host = window.location.hostname.toLowerCase();
                    const trusted = $trustedHostsJson.split(',');
                    const isTrusted = trusted.some(t => host === t || host.endsWith('.' + t));
                    if (!isTrusted) {
                        console.warn("Auto-signin bloqueado por seguridad: host no verificado como proveedor legítimo.");
                        return;
                    }

                    // Buscar campos de usuario/email comunes en formularios web
                    const emailSelectors = [
                        'input[type="email"]',
                        'input[name="email"]',
                        'input[name="username"]',
                        'input[name="identifier"]',
                        '#identifierId',
                        'input[autocomplete="username"]',
                        'input[autocomplete="email"]'
                    ];
                    for (const selector of emailSelectors) {
                        const input = document.querySelector(selector);
                        if (input && !input.value) {
                            input.value = $quotedEmail;
                            input.dispatchEvent(new Event('input', { bubbles: true }));
                            input.dispatchEvent(new Event('change', { bubbles: true }));
                            break;
                        }
                    }
                } catch(e) {
                    console.warn("Auto-signin bridge script error:", e);
                }
            })();
        """.trimIndent()
    }
}

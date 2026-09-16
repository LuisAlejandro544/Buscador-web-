package com.example.browser.account

import com.example.data.local.entity.UserAccountEntity
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
 * prepara la sugerencia nativa para el usuario y genera scripts para completar
 * el inicio de sesión sin necesidad de ingresar contraseñas manualmente.
 */
object WebSignInBridge {

    /**
     * Lista de palabras clave o dominios reconocidos que típicamente contienen
     * botones de "Iniciar sesión con Google" o flujos de inicio de sesión web.
     */
    private val authPatterns = listOf(
        "accounts.google.com",
        "login",
        "signin",
        "sign-in",
        "auth",
        "oauth",
        "session",
        "entrar",
        "iniciar-sesion"
    )

    /**
     * Evalúa si una URL corresponde a una página de acceso o flujo de autenticación.
     */
    fun isAuthPage(url: String): Boolean {
        if (url.isBlank() || url.startsWith("about:")) return false
        val lower = url.lowercase()
        return authPatterns.any { pattern -> lower.contains(pattern) }
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
            title = "Iniciar sesión en $domain como ${account.displayName}"
        )
    }

    /**
     * Genera un script JavaScript ligero y no intrusivo que asiste al inicio de sesión
     * buscando campos estándar de email o activando botones de Google One-Tap si están presentes.
     */
    fun generateAutoSignInScript(account: UserAccountEntity): String {
        val safeEmail = account.email.replace("'", "\\'")
        return """
            (function() {
                try {
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
                            input.value = '$safeEmail';
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

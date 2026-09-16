package com.example.browser.engine

import android.net.Uri

/**
 * Modelos de datos para gestionar diálogos interactivos de la web (JavaScript Prompts, Autenticación y Selectores de archivos).
 * Permiten abstraer las peticiones del motor GeckoView y mostrarlas mediante componentes nativos Material 3 en Compose.
 */
sealed class WebPromptRequest {

    /**
     * Diálogo de alerta JavaScript (`alert("mensaje")`).
     */
    data class Alert(
        val title: String,
        val message: String,
        val onConfirm: () -> Unit
    ) : WebPromptRequest()

    /**
     * Diálogo de confirmación JavaScript (`confirm("¿Estás seguro?")`).
     */
    data class Confirm(
        val title: String,
        val message: String,
        val onConfirm: () -> Unit,
        val onDismiss: () -> Unit
    ) : WebPromptRequest()

    /**
     * Diálogo de solicitud de texto JavaScript (`prompt("Introduce tu nombre", "predeterminado")`).
     */
    data class Prompt(
        val title: String,
        val message: String,
        val defaultValue: String,
        val onConfirm: (String) -> Unit,
        val onDismiss: () -> Unit
    ) : WebPromptRequest()

    /**
     * Diálogo de selección de archivos para elementos HTML `<input type="file">`.
     */
    data class FileChooser(
        val title: String,
        val isMultiple: Boolean,
        val mimeTypes: List<String>,
        val onFilesSelected: (List<Uri>) -> Unit,
        val onDismiss: () -> Unit
    ) : WebPromptRequest()

    /**
     * Diálogo de autenticación HTTP básica (usuario y contraseña).
     */
    data class HttpAuth(
        val host: String,
        val realm: String?,
        val onConfirm: (username: String, password: String) -> Unit,
        val onDismiss: () -> Unit
    ) : WebPromptRequest()
}

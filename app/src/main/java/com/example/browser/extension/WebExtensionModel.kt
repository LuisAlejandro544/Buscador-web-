package com.example.browser.extension

/**
 * Modelo de datos representativo de una extensión WebExtension para la interfaz de usuario.
 * 
 * Modela el estado funcional de una extensión instalada en GeckoView:
 * @property id Identificador único de la extensión (ej: uBlock0@raymondhill.net).
 * @property name Nombre legible para el usuario.
 * @property description Descripción de las capacidades y propósito de la extensión.
 * @property version Versión instalada de la extensión.
 * @property isEnabled Indica si la extensión está activa en las sesiones web.
 * @property isBuiltIn Indica si fue provista por el sistema o descargada por el usuario.
 * @property optionsPageUrl URL interna de la página de configuración u opciones de la extensión.
 * @property iconUrl URL o esquema del icono descriptivo de la extensión.
 * @property isAllowedInPrivateBrowsing Si la extensión tiene permiso para ejecutarse en modo incógnito.
 */
data class WebExtensionModel(
    val id: String,
    val name: String,
    val description: String = "",
    val version: String = "1.0",
    val isEnabled: Boolean = true,
    val isBuiltIn: Boolean = false,
    val optionsPageUrl: String? = null,
    val iconUrl: String? = null,
    val isAllowedInPrivateBrowsing: Boolean = true
)

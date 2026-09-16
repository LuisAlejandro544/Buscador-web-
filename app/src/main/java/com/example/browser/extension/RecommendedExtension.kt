package com.example.browser.extension

/**
 * Representa una extensión oficial recomendada para ser descargada por el usuario desde Mozilla Add-ons (AMO).
 * 
 * Principio de arquitectura y licenciamiento:
 * Ningún archivo binario o paquete .xpi se almacena localmente en el repositorio ni en los assets de la app.
 * La aplicación se conecta de forma segura a los repositorios oficiales de extensiones de Mozilla (AMO)
 * únicamente cuando el usuario solicita explícitamente su instalación.
 * 
 * @property id Identificador canónico en AMO.
 * @property name Nombre popular de la extensión.
 * @property category Categoría funcional (Privacidad, Apariencia, Herramientas).
 * @property description Resumen de beneficios para el usuario móvil.
 * @property downloadUrl Enlace HTTPS directo y oficial para descargar la última versión .xpi desde Mozilla.
 * @property iconType Clave visual para la selección de icono en Jetpack Compose.
 */
data class RecommendedExtension(
    val id: String,
    val name: String,
    val category: String,
    val description: String,
    val downloadUrl: String,
    val iconType: String,
    val iconUrl: String
) {
    companion object {
        /**
         * Catálogo curado de extensiones oficiales de Mozilla optimizadas para navegación móvil:
         * 1. uBlock Origin: El bloqueador de publicidad y rastreadores más eficiente del ecosistema.
         * 2. Dark Reader: Modo oscuro universal para forzar fondos oscuros en páginas web con alto contraste.
         * 3. TWP (Traducir Páginas Web): Traducción en vivo de sitios web completos.
         * 4. ClearURLs: Limpieza de parámetros de telemetría y espionaje en enlaces.
         */
        val CATALOG: List<RecommendedExtension> = listOf(
            RecommendedExtension(
                id = "uBlock0@raymondhill.net",
                name = "uBlock Origin",
                category = "Bloqueador de Anuncios",
                description = "Bloquea publicidad intrusiva, banners, ventanas emergentes y rastreadores web. Acelera la carga y ahorra datos móviles.",
                downloadUrl = "https://addons.mozilla.org/firefox/downloads/latest/ublock-origin/latest.xpi",
                iconType = "SHIELD",
                iconUrl = "https://raw.githubusercontent.com/gorhill/uBlock/master/src/img/icon_128.png"
            ),
            RecommendedExtension(
                id = "addon@darkreader.org",
                name = "Dark Reader",
                category = "Modo Oscuro Universal",
                description = "Convierte automáticamente los fondos blancos brillantes de cualquier sitio web en tonos oscuros para cuidar la vista y ahorrar batería OLED.",
                downloadUrl = "https://addons.mozilla.org/firefox/downloads/latest/darkreader/latest.xpi",
                iconType = "DARK_MODE",
                iconUrl = "https://raw.githubusercontent.com/darkreader/darkreader/master/src/icons/dr_128.png"
            ),
            RecommendedExtension(
                id = "{036a55b4-5e72-4d05-a9c1-bba0175b86f0}",
                name = "Traductor de Páginas Web",
                category = "Productividad",
                description = "Traduce páginas web enteras en tiempo real directamente en la misma pestaña sin salir del navegador.",
                downloadUrl = "https://addons.mozilla.org/firefox/downloads/latest/traduzir-paginas-web/latest.xpi",
                iconType = "TRANSLATE",
                iconUrl = "https://raw.githubusercontent.com/FilipePS/Traduzir-Paginas-Web/master/res/icon-128.png"
            ),
            RecommendedExtension(
                id = "{74145fec-f68b-474a-8703-00e058d04d40}",
                name = "ClearURLs",
                category = "Privacidad y Anti-Rastreo",
                description = "Elimina campos de rastreo y espionaje (como utm_source o fbclid) de las URLs para proteger tu historial de navegación.",
                downloadUrl = "https://addons.mozilla.org/firefox/downloads/latest/clearurls/latest.xpi",
                iconType = "LINK",
                iconUrl = "https://gitlab.com/KevinRoebert/ClearUrls/-/raw/master/res/img/icon_128.png"
            )
        )
    }
}

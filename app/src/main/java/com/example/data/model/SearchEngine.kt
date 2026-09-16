package com.example.data.model

/**
 * Motores de búsqueda disponibles para el navegador web.
 * Cada motor define su nombre, URL base de búsqueda y dominio principal.
 */
enum class SearchEngine(
    val displayName: String,
    val searchUrl: String,
    val homeUrl: String
) {
    DUCKDUCKGO(
        displayName = "DuckDuckGo",
        searchUrl = "https://duckduckgo.com/?q=",
        homeUrl = "https://duckduckgo.com"
    ),
    GOOGLE(
        displayName = "Google",
        searchUrl = "https://www.google.com/search?q=",
        homeUrl = "https://www.google.com"
    ),
    BING(
        displayName = "Bing",
        searchUrl = "https://www.bing.com/search?q=",
        homeUrl = "https://www.bing.com"
    ),
    BRAVE(
        displayName = "Brave Search",
        searchUrl = "https://search.brave.com/search?q=",
        homeUrl = "https://search.brave.com"
    ),
    ECOSIA(
        displayName = "Ecosia",
        searchUrl = "https://www.ecosia.org/search?q=",
        homeUrl = "https://www.ecosia.org"
    );

    /**
     * Convierte una entrada del usuario en una URL válida.
     * Si parece una URL (con protocolo o dominio válido), la normaliza.
     * De lo contrario, la busca usando el motor actual.
     */
    fun formatQueryOrUrl(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return homeUrl

        // Si ya tiene esquema http o https
        if (trimmed.startsWith("http://", ignoreCase = true) || 
            trimmed.startsWith("https://", ignoreCase = true) ||
            trimmed.startsWith("file://", ignoreCase = true) ||
            trimmed.startsWith("about:", ignoreCase = true)) {
            return trimmed
        }

        // Si contiene un punto sin espacios, probablemente es un dominio (ej: google.com, wikipedia.org/wiki)
        val hasDomainStructure = !trimmed.contains(" ") && trimmed.contains(".") && 
                trimmed.substringAfterLast(".").length in 2..6

        return if (hasDomainStructure) {
            "https://$trimmed"
        } else {
            searchUrl + java.net.URLEncoder.encode(trimmed, "UTF-8")
        }
    }
}

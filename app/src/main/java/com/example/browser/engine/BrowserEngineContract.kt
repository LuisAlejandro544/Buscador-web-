package com.example.browser.engine

/**
 * Estado que describe la página web actual y la carga en el motor de navegación.
 */
data class EnginePageState(
    val url: String = "about:home",
    val title: String = "",
    val progress: Int = 0,
    val isLoading: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val isSecure: Boolean = false,
    val isDesktopMode: Boolean = false,
    val faviconUrl: String? = null
)

/**
 * Contrato de abstracción del motor de navegación web.
 * Esta interfaz desacopla la UI y el ViewModel del motor de renderizado específico.
 * 
 * Permite que actualmente usemos WebView de Android de manera robusta,
 * y en el futuro facilita la transición e integración directa de GeckoView (Mozilla).
 */
interface BrowserEngineContract {
    /**
     * Carga una URL específica en el motor.
     */
    fun loadUrl(url: String)

    /**
     * Navega hacia atrás en el historial de la pestaña.
     */
    fun goBack()

    /**
     * Navega hacia adelante en el historial de la pestaña.
     */
    fun goForward()

    /**
     * Recarga la página actual.
     */
    fun reload()

    /**
     * Detiene la carga de la página actual.
     */
    fun stopLoading()

    /**
     * Activa o desactiva la simulación de agente de usuario de escritorio.
     */
    fun setDesktopMode(enabled: Boolean)

    /**
     * Habilita o deshabilita la ejecución de JavaScript en el motor.
     */
    fun setJavaScriptEnabled(enabled: Boolean)

    /**
     * Limpia la memoria caché del motor de renderizado.
     */
    fun clearCache()

    /**
     * Evalúa un fragmento de código JavaScript en el contexto de la página.
     */
    fun evaluateJavascript(script: String, callback: ((String) -> Unit)? = null)
}

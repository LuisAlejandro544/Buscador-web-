package com.example

import com.example.browser.engine.NativeBridge
import com.example.browser.reader.ReaderBlock
import com.example.browser.reader.ReaderExtractor
import org.junit.Assert.*
import org.junit.Test

/**
 * Pruebas unitarias para los componentes centrales de la aplicación:
 * - Extractor de Modo Lectura (ReaderExtractor): limpieza del DOM, aislamiento de artículos y metadatos.
 * - Motor de filtrado nativo / fallback (NativeBridge): coincidencia de reglas ABP/Hosts y estadísticas.
 */
class ExampleUnitTest {

    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun readerExtractor_cleansHtmlAndExtractsArticle() {
        val sampleHtml = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Título de la Noticia - Periódico Digital</title>
                <meta property="og:title" content="Descubrimiento Científico Revolucionario" />
                <meta name="author" content="Dra. Elena Ramos" />
                <meta property="article:published_time" content="2026-09-16" />
                <meta property="og:site_name" content="Ciencia Global" />
                <meta property="og:image" content="https://example.com/cover.jpg" />
            </head>
            <body>
                <nav><a href="#">Inicio</a><a href="#">Contacto</a></nav>
                <div class="ad">Publicidad molesta 1</div>
                <article>
                    <h1>Descubrimiento Científico Revolucionario</h1>
                    <p class="byline">Por Dra. Elena Ramos</p>
                    <p>Científicos del observatorio han revelado un nuevo fenómeno en galaxias lejanas con datos detallados.</p>
                    <blockquote>"Este hallazgo transformará la astrofísica moderna."</blockquote>
                    <p>Las observaciones preliminares indican una oscilación periódica que no había sido registrada con anterioridad.</p>
                    <div class="cookie-banner">Acepta nuestras cookies</div>
                </article>
                <aside class="ads">Anuncio lateral</aside>
                <footer>Derechos reservados 2026</footer>
            </body>
            </html>
        """.trimIndent()

        val article = ReaderExtractor.extractFromHtml(sampleHtml, "https://cienciaglobal.com/articulo-123")

        assertEquals("Descubrimiento Científico Revolucionario", article.title)
        assertEquals("Ciencia Global", article.siteName)
        assertEquals("https://example.com/cover.jpg", article.leadImageUrl)
        assertTrue(article.blocks.isNotEmpty())

        // Verifica que no haya rastros de anuncios ni navegación
        val hasAdContent = article.plainText.contains("Publicidad") || article.plainText.contains("cookies")
        assertFalse(hasAdContent)

        // Verifica presencia de cita
        val quoteBlock = article.blocks.filterIsInstance<ReaderBlock.Quote>().firstOrNull()
        assertNotNull(quoteBlock)
        assertTrue(quoteBlock!!.text.contains("Este hallazgo transformará"))
    }

    @Test
    fun filterEngine_blocksAdUrlsCorrectly() {
        // Verificar estado del motor
        val isReady = NativeBridge.isFilterReady()
        assertTrue(isReady)

        // Comprobar URLs conocidas de publicidad y rastreo
        val shouldBlockDoubleclick = NativeBridge.shouldBlockUrl("https://ad.doubleclick.net/pixel.js")
        assertTrue(shouldBlockDoubleclick)

        val shouldBlockGoogleAdservices = NativeBridge.shouldBlockUrl("https://googleadservices.com/pagead/conversion")
        assertTrue(shouldBlockGoogleAdservices)

        // Comprobar URL legítima no bloqueada
        val shouldBlockWikipedia = NativeBridge.shouldBlockUrl("https://es.wikipedia.org/wiki/Kotlin")
        assertFalse(shouldBlockWikipedia)

        // Añadir regla personalizada
        val newCount = NativeBridge.addFilterRules("||malicious-tracker.org^\n127.0.0.1 bad-analytics.net")
        assertTrue(newCount >= 2)

        val shouldBlockCustom = NativeBridge.shouldBlockUrl("https://malicious-tracker.org/script.js")
        assertTrue(shouldBlockCustom)

        // Verificar reporte de estadísticas en JSON
        val stats = NativeBridge.getFilterStats()
        assertTrue(stats.contains("rules_count"))
        assertTrue(stats.contains("blocked_count"))
    }
}


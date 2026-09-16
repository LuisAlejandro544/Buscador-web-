package com.example.browser.reader

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.net.URI

/**
 * Extractor y analizador de contenido web para el Modo Lectura Nativo.
 * 
 * Implementa un algoritmo heurístico inspirado en Readability:
 * - Identifica el contenedor principal del artículo (<article>, role="main", .post-content).
 * - Elimina elementos no deseados (publicidad, scripts, menús de navegación, iframes, cookies).
 * - Extrae metadatos editoriales (título, autor, fecha de publicación, dominio, imagen de portada).
 * - Convierte la jerarquía del DOM en una lista inmutable de [ReaderBlock] tipados.
 * - Calcula estadísticas como conteo de palabras y tiempo estimado de lectura.
 */
object ReaderExtractor {

    /**
     * Extrae un [ReaderArticle] a partir del HTML crudo de la página web.
     */
    fun extractFromHtml(html: String, targetUrl: String): ReaderArticle {
        if (html.isBlank()) {
            return fallbackArticle(targetUrl)
        }

        return try {
            val doc: Document = Jsoup.parse(html, targetUrl)

            // 1. Extracción de metadatos OpenGraph / Schema / Meta
            val title = extractTitle(doc)
            val byline = extractByline(doc)
            val publishedTime = extractPublishedDate(doc)
            val siteName = extractSiteName(doc, targetUrl)
            val leadImageUrl = extractLeadImage(doc)

            // 2. Limpieza de elementos ruidosos del DOM
            cleanDocument(doc)

            // 3. Localizar contenedor del contenido principal
            val articleElement = findArticleContainer(doc)

            // 4. Transformar nodos en bloques de lectura
            val blocks = mutableListOf<ReaderBlock>()
            val textBuilder = StringBuilder()

            articleElement.children().forEach { child ->
                parseElementToBlocks(child, blocks, textBuilder)
            }

            // Si no se extrajeron bloques de los hijos directos, procesar párrafos globales
            if (blocks.isEmpty()) {
                val paragraphs = articleElement.select("p")
                for (p in paragraphs) {
                    val text = p.text().trim()
                    if (text.length > 20) {
                        blocks.add(ReaderBlock.Paragraph(text))
                        textBuilder.append(text).append("\n\n")
                    }
                }
            }

            val plainText = textBuilder.toString().trim()
            val wordCount = if (plainText.isNotEmpty()) {
                plainText.split("\\s+".toRegex()).count { it.isNotBlank() }
            } else {
                0
            }
            val estimatedMinutes = (wordCount / 200).coerceAtLeast(1)

            ReaderArticle(
                url = targetUrl,
                title = title.ifBlank { "Artículo sin título" },
                byline = byline,
                publishedTime = publishedTime,
                siteName = siteName,
                leadImageUrl = leadImageUrl,
                excerpt = blocks.filterIsInstance<ReaderBlock.Paragraph>().firstOrNull()?.text?.take(200),
                blocks = blocks,
                plainText = plainText,
                wordCount = wordCount,
                estimatedReadingTimeMinutes = estimatedMinutes
            )
        } catch (_: Throwable) {
            fallbackArticle(targetUrl)
        }
    }

    private fun extractTitle(doc: Document): String {
        val ogTitle = doc.select("meta[property=og:title]").attr("content").trim()
        if (ogTitle.isNotBlank()) return ogTitle

        val twitterTitle = doc.select("meta[name=twitter:title]").attr("content").trim()
        if (twitterTitle.isNotBlank()) return twitterTitle

        val h1 = doc.select("h1").firstOrNull()?.text()?.trim().orEmpty()
        if (h1.isNotBlank()) return h1

        return doc.title().trim()
    }

    private fun extractByline(doc: Document): String? {
        val authorMeta = doc.select("meta[name=author]").attr("content").trim()
        if (authorMeta.isNotBlank()) return authorMeta

        val articleAuthor = doc.select("meta[property=article:author]").attr("content").trim()
        if (articleAuthor.isNotBlank()) return articleAuthor

        val relAuthor = doc.select("[rel=author]").firstOrNull()?.text()?.trim()
        if (!relAuthor.isNullOrBlank()) return relAuthor

        val bylineClass = doc.select(".byline, .author, .entry-author").firstOrNull()?.text()?.trim()
        if (!bylineClass.isNullOrBlank() && bylineClass.length < 80) return bylineClass

        return null
    }

    private fun extractPublishedDate(doc: Document): String? {
        val dateMeta = doc.select("meta[property=article:published_time]").attr("content").trim()
        if (dateMeta.isNotBlank()) return dateMeta.take(10)

        val timeTag = doc.select("time").firstOrNull()?.text()?.trim()
        if (!timeTag.isNullOrBlank() && timeTag.length < 40) return timeTag

        return null
    }

    private fun extractSiteName(doc: Document, url: String): String {
        val ogSite = doc.select("meta[property=og:site_name]").attr("content").trim()
        if (ogSite.isNotBlank()) return ogSite

        return try {
            val host = URI(url).host
            host?.removePrefix("www.") ?: "Web"
        } catch (_: Throwable) {
            "Artículo Web"
        }
    }

    private fun extractLeadImage(doc: Document): String? {
        val ogImage = doc.select("meta[property=og:image]").attr("content").trim()
        if (ogImage.isNotBlank() && (ogImage.startsWith("http://") || ogImage.startsWith("https://"))) {
            return ogImage
        }

        val firstImg = doc.select("article img, .post-content img, main img").firstOrNull()
        val src = firstImg?.absUrl("src") ?: firstImg?.attr("src")
        if (!src.isNullOrBlank() && (src.startsWith("http://") || src.startsWith("https://"))) {
            return src
        }

        return null
    }

    private fun cleanDocument(doc: Document) {
        val noisySelectors = listOf(
            "script", "style", "noscript", "iframe", "svg", "button", "input", "form",
            "nav", "footer", "header:not(article header)", "aside",
            ".ad", ".ads", ".advertisement", ".banner", ".cookie-banner",
            ".social-share", ".share-buttons", ".comments", "#comments",
            ".related-posts", ".newsletter-signup", ".popup", ".modal"
        )
        for (selector in noisySelectors) {
            doc.select(selector).remove()
        }
    }

    private fun findArticleContainer(doc: Document): Element {
        val candidateSelectors = listOf(
            "article",
            "[role=main]",
            "main",
            ".post-content",
            ".entry-content",
            ".article-body",
            ".story-body",
            "#content",
            ".content"
        )

        for (selector in candidateSelectors) {
            val element = doc.select(selector).firstOrNull()
            if (element != null && element.text().length > 100) {
                return element
            }
        }

        return doc.body() ?: doc
    }

    private fun parseElementToBlocks(
        element: Element,
        blocks: MutableList<ReaderBlock>,
        textBuilder: StringBuilder
    ) {
        val tag = element.tagName().lowercase()
        val text = element.text().trim()

        when (tag) {
            "h1", "h2" -> {
                if (text.isNotBlank() && text.length < 150) {
                    blocks.add(ReaderBlock.Heading(level = 2, text = text))
                    textBuilder.append("\n## ").append(text).append("\n\n")
                }
            }
            "h3", "h4", "h5", "h6" -> {
                if (text.isNotBlank() && text.length < 150) {
                    blocks.add(ReaderBlock.Subtitle(text = text))
                    textBuilder.append("\n### ").append(text).append("\n\n")
                }
            }
            "p" -> {
                if (text.isNotBlank() && text.length > 20) {
                    blocks.add(ReaderBlock.Paragraph(text = text))
                    textBuilder.append(text).append("\n\n")
                }
            }
            "blockquote" -> {
                if (text.isNotBlank()) {
                    blocks.add(ReaderBlock.Quote(text = text))
                    textBuilder.append("> ").append(text).append("\n\n")
                }
            }
            "img" -> {
                val src = element.absUrl("src").ifBlank { element.attr("src") }
                if (src.startsWith("http://") || src.startsWith("https://")) {
                    val alt = element.attr("alt").trim().ifBlank { null }
                    blocks.add(ReaderBlock.Image(url = src, caption = alt))
                }
            }
            "ul", "ol" -> {
                val isOrdered = tag == "ol"
                val items = element.select("li").map { it.text().trim() }.filter { it.isNotBlank() }
                if (items.isNotEmpty()) {
                    blocks.add(ReaderBlock.ListBlock(items = items, isOrdered = isOrdered))
                    items.forEachIndexed { i, item ->
                        val prefix = if (isOrdered) "${i + 1}. " else "- "
                        textBuilder.append(prefix).append(item).append("\n")
                    }
                    textBuilder.append("\n")
                }
            }
            else -> {
                // Si es un div contenedor, procesar recursivamente sus hijos
                if (element.children().isNotEmpty()) {
                    element.children().forEach { child ->
                        parseElementToBlocks(child, blocks, textBuilder)
                    }
                } else if (text.length > 40 && (tag == "section" || tag == "div")) {
                    blocks.add(ReaderBlock.Paragraph(text = text))
                    textBuilder.append(text).append("\n\n")
                }
            }
        }
    }

    private fun fallbackArticle(url: String): ReaderArticle {
        val host = try {
            URI(url).host ?: "Página Web"
        } catch (_: Throwable) {
            "Página Web"
        }

        return ReaderArticle(
            url = url,
            title = "Vista de lectura no disponible para esta página",
            siteName = host,
            excerpt = "Esta página no contiene un formato de artículo identificable o no pudo ser procesada.",
            plainText = "Esta página no contiene un formato de artículo identificable.",
            wordCount = 0,
            estimatedReadingTimeMinutes = 1
        )
    }
}

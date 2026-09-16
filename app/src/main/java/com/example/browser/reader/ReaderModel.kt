package com.example.browser.reader

import androidx.compose.ui.graphics.Color

/**
 * Bloques estructurados que componen el contenido limpio de un artículo en Modo Lectura.
 */
sealed class ReaderBlock {
    data class Paragraph(val text: String) : ReaderBlock()
    data class Heading(val level: Int, val text: String) : ReaderBlock()
    data class Subtitle(val text: String) : ReaderBlock()
    data class Quote(val text: String, val cite: String? = null) : ReaderBlock()
    data class Image(val url: String, val caption: String? = null) : ReaderBlock()
    data class ListBlock(val items: List<String>, val isOrdered: Boolean = false) : ReaderBlock()
}

/**
 * Modelo de datos inmutable que representa un artículo web extraído y normalizado
 * para lectura nativa sin distracciones, banners ni publicidad.
 */
data class ReaderArticle(
    val url: String,
    val title: String,
    val byline: String? = null,
    val publishedTime: String? = null,
    val siteName: String? = null,
    val leadImageUrl: String? = null,
    val excerpt: String? = null,
    val blocks: List<ReaderBlock> = emptyList(),
    val plainText: String = "",
    val wordCount: Int = 0,
    val estimatedReadingTimeMinutes: Int = 1,
    val extractedAt: Long = System.currentTimeMillis()
)

/**
 * Temas visuales de lectura optimizados para descanso visual y contraste.
 */
enum class ReaderTheme(
    val label: String,
    val backgroundColor: Color,
    val textColor: Color,
    val accentColor: Color
) {
    SEPIA(
        label = "Sepia Cálido",
        backgroundColor = Color(0xFFF4ECD8),
        textColor = Color(0xFF3C2F2F),
        accentColor = Color(0xFF8B5A2B)
    ),
    LIGHT(
        label = "Papel Blanco",
        backgroundColor = Color(0xFFFAFAFA),
        textColor = Color(0xFF1E1E1E),
        accentColor = Color(0xFF00796B)
    ),
    DARK(
        label = "Noche OLED",
        backgroundColor = Color(0xFF121212),
        textColor = Color(0xFFE0E0E0),
        accentColor = Color(0xFF80CBC4)
    ),
    CHARCOAL(
        label = "Gris Carbón",
        backgroundColor = Color(0xFF263238),
        textColor = Color(0xFFECEFF1),
        accentColor = Color(0xFF4DB6AC)
    )
}

/**
 * Familias tipográficas para lectura prolongada.
 */
enum class ReaderFont(val label: String) {
    SERIF("Serif Clásico"),
    SANS("Sans Moderno"),
    MONO("Monospace")
}

/**
 * Configuración y preferencias visuales del usuario en el Modo Lectura.
 */
data class ReaderSettings(
    val theme: ReaderTheme = ReaderTheme.SEPIA,
    val font: ReaderFont = ReaderFont.SERIF,
    val fontSizeSp: Float = 18f,
    val lineHeightMultiplier: Float = 1.5f,
    val isSpeechPlaying: Boolean = false,
    val speechProgressIndex: Int = 0
)

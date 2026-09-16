package com.example.ui.reader

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.browser.reader.ReaderArticle
import com.example.browser.reader.ReaderBlock
import com.example.browser.reader.ReaderFont
import com.example.browser.reader.ReaderSettings
import com.example.browser.reader.ReaderTheme
import com.example.viewmodel.BrowserViewModel

/**
 * Pantalla dedicada de Modo Lectura Nativo.
 * 
 * Ofrece una experiencia de lectura editorial libre de distracciones, anuncios y scripts:
 * - Selección de temas tipográficos de confort visual (Sepia Cálido, Papel Blanco, Noche OLED, Gris Carbón).
 * - Control de tamaño de texto y familias tipográficas (Serif, Sans, Mono).
 * - Lectura en voz alta integrada (Text-To-Speech).
 * - Guardado directo en marcadores y compartición de artículos.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    viewModel: BrowserViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val article by viewModel.readerArticle.collectAsState()
    val isLoading by viewModel.isReaderLoading.collectAsState()
    val errorMessage by viewModel.readerErrorMessage.collectAsState()
    val settings by viewModel.readerSettings.collectAsState()

    var showAppearanceSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val currentTheme = settings.theme
    val currentFont = when (settings.font) {
        ReaderFont.SERIF -> FontFamily.Serif
        ReaderFont.SANS -> FontFamily.SansSerif
        ReaderFont.MONO -> FontFamily.Monospace
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = article?.siteName ?: "Modo Lectura",
                            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = "Lectura Nativa Sin Distracciones",
                            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                            color = currentTheme.textColor.copy(alpha = 0.7f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            viewModel.stopReaderSpeech()
                            onNavigateBack()
                        },
                        modifier = Modifier.testTag("reader_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver al navegador",
                            tint = currentTheme.textColor
                        )
                    }
                },
                actions = {
                    // Botón Text-To-Speech (Lectura en voz alta)
                    IconButton(
                        onClick = { viewModel.toggleReaderSpeech() },
                        modifier = Modifier.testTag("reader_tts_button")
                    ) {
                        Icon(
                            imageVector = if (settings.isSpeechPlaying) Icons.Default.Stop else Icons.Default.VolumeUp,
                            contentDescription = if (settings.isSpeechPlaying) "Detener lectura de voz" else "Escuchar artículo",
                            tint = if (settings.isSpeechPlaying) Color(0xFFE53935) else currentTheme.textColor
                        )
                    }

                    // Botón de personalización visual (Tema, tipografía y tamaño)
                    IconButton(
                        onClick = { showAppearanceSheet = true },
                        modifier = Modifier.testTag("reader_appearance_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatSize,
                            contentDescription = "Personalizar apariencia",
                            tint = currentTheme.textColor
                        )
                    }

                    // Botón compartir artículo
                    IconButton(
                        onClick = {
                            article?.let { item ->
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    putExtra(Intent.EXTRA_SUBJECT, item.title)
                                    putExtra(Intent.EXTRA_TEXT, "${item.title}\n\n${item.url}")
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Compartir artículo"))
                            }
                        },
                        modifier = Modifier.testTag("reader_share_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Compartir artículo",
                            tint = currentTheme.textColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = currentTheme.backgroundColor,
                    titleContentColor = currentTheme.textColor
                )
            )
        },
        containerColor = currentTheme.backgroundColor
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(currentTheme.backgroundColor)
        ) {
            when {
                isLoading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = currentTheme.accentColor,
                            modifier = Modifier.testTag("reader_loading_indicator")
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Extrayendo artículo limpio...",
                            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                            color = currentTheme.textColor,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Aislando el contenido de publicidad, rastreadores y elementos ruidosos",
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                            color = currentTheme.textColor.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                errorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "No se pudo cargar el modo lectura",
                            style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                            color = currentTheme.textColor,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = errorMessage ?: "",
                            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                            color = currentTheme.textColor.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = onNavigateBack,
                            colors = ButtonDefaults.buttonColors(containerColor = currentTheme.accentColor),
                            modifier = Modifier.testTag("reader_error_back_button")
                        ) {
                            Text("Volver a la web original", color = Color.White)
                        }
                    }
                }

                article != null -> {
                    val currentArticle = article!!

                    Column(modifier = Modifier.fillMaxSize()) {
                        // Banner flotante de síntesis de voz activa
                        AnimatedVisibility(
                            visible = settings.isSpeechPlaying,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Surface(
                                color = currentTheme.accentColor.copy(alpha = 0.15f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.VolumeUp,
                                            contentDescription = null,
                                            tint = currentTheme.accentColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Leyendo en voz alta...",
                                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                                            color = currentTheme.textColor,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.stopReaderSpeech() },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Detener",
                                            tint = currentTheme.textColor
                                        )
                                    }
                                }
                            }
                        }

                        // Lista scrolleable con el contenido del artículo
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp),
                            contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp)
                        ) {
                            // Encabezado del artículo
                            item {
                                ArticleHeader(
                                    article = currentArticle,
                                    theme = currentTheme,
                                    fontFamily = currentFont
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                HorizontalDivider(
                                    color = currentTheme.textColor.copy(alpha = 0.15f),
                                    thickness = 1.dp
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                            }

                            // Bloques de contenido limpio
                            items(currentArticle.blocks) { block ->
                                ArticleBlockItem(
                                    block = block,
                                    theme = currentTheme,
                                    fontFamily = currentFont,
                                    fontSizeSp = settings.fontSizeSp,
                                    lineHeightMultiplier = settings.lineHeightMultiplier
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            // Pie de lectura con acciones
                            item {
                                Spacer(modifier = Modifier.height(32.dp))
                                HorizontalDivider(
                                    color = currentTheme.textColor.copy(alpha = 0.15f),
                                    thickness = 1.dp
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                ArticleFooterActions(
                                    article = currentArticle,
                                    theme = currentTheme,
                                    onSaveBookmark = {
                                        viewModel.addBookmarkManual(
                                            title = currentArticle.title,
                                            url = currentArticle.url
                                        )
                                    },
                                    onReturnToWeb = onNavigateBack
                                )
                            }
                        }
                    }
                }
            }

            // Hoja modal de personalización visual (Bottom Sheet)
            if (showAppearanceSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showAppearanceSheet = false },
                    sheetState = sheetState,
                    containerColor = currentTheme.backgroundColor
                ) {
                    ReaderAppearanceControls(
                        settings = settings,
                        onThemeSelected = { viewModel.setReaderTheme(it) },
                        onFontSelected = { viewModel.setReaderFont(it) },
                        onFontSizeAdjust = { viewModel.adjustReaderFontSize(it) },
                        onDismiss = { showAppearanceSheet = false }
                    )
                }
            }
        }
    }
}

/**
 * Cabecera editorial del artículo: título, autor, metadatos y foto de portada.
 */
@Composable
private fun ArticleHeader(
    article: ReaderArticle,
    theme: ReaderTheme,
    fontFamily: FontFamily
) {
    Column {
        // Título del artículo
        Text(
            text = article.title,
            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium.copy(
                fontSize = 26.sp,
                lineHeight = 34.sp,
                fontFamily = fontFamily,
                fontWeight = FontWeight.Bold
            ),
            color = theme.textColor
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Fila de metadatos (Autor, Tiempo de lectura, Fecha)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            article.byline?.let { author ->
                Text(
                    text = "Por $author",
                    style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                    color = theme.accentColor,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Text(
                text = "⏱️ ${article.estimatedReadingTimeMinutes} min",
                style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                color = theme.textColor.copy(alpha = 0.7f)
            )

            article.publishedTime?.let { date ->
                Text(
                    text = "• $date",
                    style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                    color = theme.textColor.copy(alpha = 0.6f)
                )
            }
        }

        // Imagen de portada (si existe)
        article.leadImageUrl?.let { imageUrl ->
            Spacer(modifier = Modifier.height(16.dp))
            AsyncImage(
                model = imageUrl,
                contentDescription = "Imagen principal del artículo",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, theme.textColor.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )
        }
    }
}

/**
 * Renderizado de un bloque de contenido editorial individual.
 */
@Composable
private fun ArticleBlockItem(
    block: ReaderBlock,
    theme: ReaderTheme,
    fontFamily: FontFamily,
    fontSizeSp: Float,
    lineHeightMultiplier: Float
) {
    when (block) {
        is ReaderBlock.Heading -> {
            Text(
                text = block.text,
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge.copy(
                    fontSize = (fontSizeSp + 4).sp,
                    lineHeight = ((fontSizeSp + 4) * 1.3f).sp,
                    fontFamily = fontFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = theme.textColor,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        is ReaderBlock.Subtitle -> {
            Text(
                text = block.text,
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(
                    fontSize = (fontSizeSp + 2).sp,
                    fontFamily = fontFamily,
                    fontWeight = FontWeight.SemiBold
                ),
                color = theme.textColor.copy(alpha = 0.9f),
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        is ReaderBlock.Paragraph -> {
            Text(
                text = block.text,
                style = androidx.compose.material3.MaterialTheme.typography.bodyLarge.copy(
                    fontSize = fontSizeSp.sp,
                    lineHeight = (fontSizeSp * lineHeightMultiplier).sp,
                    fontFamily = fontFamily,
                    fontWeight = FontWeight.Normal
                ),
                color = theme.textColor
            )
        }

        is ReaderBlock.Quote -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(48.dp)
                        .background(theme.accentColor, RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = block.text,
                    style = androidx.compose.material3.MaterialTheme.typography.bodyLarge.copy(
                        fontSize = (fontSizeSp - 1).sp,
                        lineHeight = ((fontSizeSp - 1) * lineHeightMultiplier).sp,
                        fontFamily = fontFamily,
                        fontStyle = FontStyle.Italic
                    ),
                    color = theme.textColor.copy(alpha = 0.9f)
                )
            }
        }

        is ReaderBlock.Image -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AsyncImage(
                    model = block.url,
                    contentDescription = block.caption ?: "Imagen del artículo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
                block.caption?.let { caption ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = caption,
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                        color = theme.textColor.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        is ReaderBlock.ListBlock -> {
            Column(modifier = Modifier.fillMaxWidth()) {
                block.items.forEachIndexed { index, item ->
                    Row(
                        modifier = Modifier.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = if (block.isOrdered) "${index + 1}. " else "• ",
                            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(
                                fontSize = fontSizeSp.sp,
                                fontFamily = fontFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = theme.accentColor
                        )
                        Text(
                            text = item,
                            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(
                                fontSize = fontSizeSp.sp,
                                lineHeight = (fontSizeSp * 1.4f).sp,
                                fontFamily = fontFamily
                            ),
                            color = theme.textColor
                        )
                    }
                }
            }
        }
    }
}

/**
 * Controles modales para personalizar temas de confort visual, familias de fuentes y tamaños.
 */
@Composable
private fun ReaderAppearanceControls(
    settings: ReaderSettings,
    onThemeSelected: (ReaderTheme) -> Unit,
    onFontSelected: (ReaderFont) -> Unit,
    onFontSizeAdjust: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Text(
            text = "Personalizar Vista de Lectura",
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = settings.theme.textColor
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Selector de Tema Cromático
        Text(
            text = "Tema y Confort Visual",
            style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
            color = settings.theme.textColor.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ReaderTheme.entries.forEach { theme ->
                val isSelected = theme == settings.theme
                Card(
                    colors = CardDefaults.cardColors(containerColor = theme.backgroundColor),
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) theme.accentColor else Color.Gray.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onThemeSelected(theme) }
                        .testTag("theme_${theme.name.lowercase()}"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = theme.label.split(" ").first(),
                            color = theme.textColor,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            style = androidx.compose.material3.MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Selector de Tipografía
        Text(
            text = "Familia Tipográfica",
            style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
            color = settings.theme.textColor.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ReaderFont.entries.forEach { font ->
                FilterChip(
                    selected = font == settings.font,
                    onClick = { onFontSelected(font) },
                    label = { Text(font.label) },
                    modifier = Modifier.weight(1f).testTag("font_${font.name.lowercase()}"),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = settings.theme.accentColor.copy(alpha = 0.2f),
                        selectedLabelColor = settings.theme.accentColor
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Control de Tamaño de Fuente
        Text(
            text = "Tamaño de Texto (${settings.fontSizeSp.toInt()} sp)",
            style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
            color = settings.theme.textColor.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { onFontSizeAdjust(-2f) },
                enabled = settings.fontSizeSp > 14f,
                modifier = Modifier.testTag("font_size_decrease_button")
            ) {
                Text("A -", fontWeight = FontWeight.Bold, color = settings.theme.textColor)
            }

            Text(
                text = "Aa",
                fontSize = settings.fontSizeSp.sp,
                fontWeight = FontWeight.Bold,
                color = settings.theme.textColor
            )

            OutlinedButton(
                onClick = { onFontSizeAdjust(2f) },
                enabled = settings.fontSizeSp < 28f,
                modifier = Modifier.testTag("font_size_increase_button")
            ) {
                Text("A +", fontWeight = FontWeight.Bold, color = settings.theme.textColor)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Acciones finales al concluir la lectura del artículo.
 */
@Composable
private fun ArticleFooterActions(
    article: ReaderArticle,
    theme: ReaderTheme,
    onSaveBookmark: () -> Unit,
    onReturnToWeb: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Fin del artículo",
            style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
            color = theme.textColor.copy(alpha = 0.5f),
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onSaveBookmark,
                modifier = Modifier.weight(1f).testTag("reader_save_bookmark_button")
            ) {
                Icon(Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Marcar")
            }

            Button(
                onClick = onReturnToWeb,
                colors = ButtonDefaults.buttonColors(containerColor = theme.accentColor),
                modifier = Modifier.weight(1f).testTag("reader_return_web_button")
            ) {
                Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Ver web")
            }
        }
    }
}

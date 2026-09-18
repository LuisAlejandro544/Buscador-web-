@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.example.ui.browser

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.entity.BookmarkEntity
import com.example.data.local.entity.HistoryEntity
import com.example.data.local.entity.ShortcutEntity
import com.example.data.model.SearchEngine

/**
 * Representa un acceso rápido predeterminado en la pantalla de inicio.
 */
data class QuickAccessItem(
    val title: String,
    val url: String,
    val iconVector: ImageVector,
    val color: Color
)

/**
 * Pantalla de inicio (Start Page / Speed Dial) del navegador.
 * Se muestra cuando no hay una página web cargada o cuando el usuario abre una nueva pestaña.
 * Incluye barra de búsqueda integrada con el motor seleccionado, accesos directos, marcadores
 * y el historial reciente.
 */
@Composable
fun BrowserStartPage(
    searchEngine: SearchEngine,
    bookmarks: List<BookmarkEntity>,
    recentHistory: List<HistoryEntity>,
    shortcuts: List<ShortcutEntity> = emptyList(),
    isIncognito: Boolean,
    isProtected: Boolean = false,
    onNavigateToUrl: (String) -> Unit,
    onAddShortcut: (title: String, url: String, iconType: String, colorHex: String) -> Unit = { _, _, _, _ -> },
    onUpdateShortcut: (ShortcutEntity) -> Unit = {},
    onDeleteShortcut: (Long) -> Unit = {},
    onResetDefaultShortcuts: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    val emeraldColor = Color(0xFF00897B)

    // Estados de diálogo para añadir y editar accesos directos
    var isAddDialogOpen by remember { mutableStateOf(false) }
    var shortcutToEdit by remember { mutableStateOf<ShortcutEntity?>(null) }

    // Fallback de accesos por defecto si la lista de la BD aún estuviera cargando
    val fallbackLinks = remember {
        listOf(
            ShortcutEntity(id = -1, title = "DuckDuckGo", url = "https://duckduckgo.com", iconType = "SEARCH", colorHex = "#DE5833", orderIndex = 0),
            ShortcutEntity(id = -2, title = "Wikipedia", url = "https://es.wikipedia.org", iconType = "LANGUAGE", colorHex = "#1E293B", orderIndex = 1),
            ShortcutEntity(id = -3, title = "GitHub", url = "https://github.com", iconType = "CODE", colorHex = "#24292E", orderIndex = 2),
            ShortcutEntity(id = -4, title = "Reddit", url = "https://www.reddit.com", iconType = "PUBLIC", colorHex = "#FF4500", orderIndex = 3),
            ShortcutEntity(id = -5, title = "YouTube", url = "https://m.youtube.com", iconType = "SPEED", colorHex = "#FF0000", orderIndex = 4),
            ShortcutEntity(id = -6, title = "Noticias", url = "https://news.google.com", iconType = "LANGUAGE", colorHex = "#1976D2", orderIndex = 5)
        )
    }

    val displayShortcuts = if (shortcuts.isNotEmpty()) shortcuts else fallbackLinks

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Encabezado de bienvenida y logotipo
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                            )
                        )
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_browser_logo),
                        contentDescription = "Logotipo del navegador",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = when {
                        isProtected -> "Pestaña Protegida"
                        isIncognito -> "Modo Incógnito Avanzado"
                        else -> "Navegador Web"
                    },
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Indicador de seguridad
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = when {
                        isProtected -> emeraldColor.copy(alpha = 0.15f)
                        isIncognito -> MaterialTheme.colorScheme.tertiaryContainer 
                        else -> MaterialTheme.colorScheme.secondaryContainer
                    },
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when {
                                isProtected -> Icons.Default.Security
                                isIncognito -> Icons.Default.Security
                                else -> Icons.Default.Lock
                            },
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = when {
                                isProtected -> emeraldColor
                                isIncognito -> MaterialTheme.colorScheme.onTertiaryContainer
                                else -> MaterialTheme.colorScheme.onSecondaryContainer
                            }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when {
                                isProtected -> "Burbuja aislada: Cookies y sesiones sin tocar tu perfil principal"
                                isIncognito -> "Blindaje Activo: RFP, Anti-Fugas WebRTC, DoH, dFPI y Purga de RAM"
                                else -> "Motor Mozilla GeckoView Omni: Renderizado nativo independiente"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = when {
                                isProtected -> emeraldColor
                                isIncognito -> MaterialTheme.colorScheme.onTertiaryContainer
                                else -> MaterialTheme.colorScheme.onSecondaryContainer
                            }
                        )
                    }
                }
            }
        }

        // Panel explicativo de seguridad cuando está en Modo Incógnito
        if (isIncognito) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Seguridad Real Más Allá de un Incógnito Genérico",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        IncognitoFeatureRow("Anti-Huella Digital (RFP)", "Simula resoluciones y perfiles uniformes estilo Tor para engañar rastreadores.")
                        IncognitoFeatureRow("Anti Fugas IP (WebRTC)", "PeerConnection desactivado y puertos STUN aislados para no revelar tu IP real.")
                        IncognitoFeatureRow("DNS Cifrado (DoH)", "Consultas DNS sobre HTTPS directo a Cloudflare/Mozilla sin espionaje de tu operador.")
                        IncognitoFeatureRow("Total Cookie Protection (dFPI)", "Contenedor hermético que impide a sitios rastrear entre webs distintas.")
                        IncognitoFeatureRow("Purga Instantánea de RAM", "Destrucción total de memoria caché volátil y recolección de memoria al cerrar.")

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "💡 Seguimos incorporando activamente muchas más funciones de seguridad y privacidad para llevar la navegación anónima al máximo nivel.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Barra de búsqueda en la página de inicio
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("start_page_search_input"),
                placeholder = {
                    Text("Buscar en ${searchEngine.displayName} o ingresar URL...")
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Buscar",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    if (query.isNotBlank()) {
                        onNavigateToUrl(query)
                    }
                }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            )
        }

        // Accesos directos configurables (Speed Dial)
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Accesos Rápidos",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Toca para abrir • Mantén presionado para editar",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Botón para restablecer los accesos predeterminados si el usuario lo desea
                        IconButton(
                            onClick = onResetDefaultShortcuts,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("reset_shortcuts_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Restablecer accesos predeterminados",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Botón para añadir un nuevo acceso directo
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .clickable { isAddDialogOpen = true }
                                .testTag("add_shortcut_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Agregar acceso rápido",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Añadir",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Cuadrícula dinámica de accesos rápidos (filas de 4 elementos)
                val chunkedShortcuts = displayShortcuts.chunked(4)
                chunkedShortcuts.forEachIndexed { rowIndex, rowShortcuts ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start)
                    ) {
                        rowShortcuts.forEach { item ->
                            Box(modifier = Modifier.weight(1f)) {
                                ConfigurableQuickAccessButton(
                                    item = item,
                                    onClick = { onNavigateToUrl(item.url) },
                                    onLongClick = { shortcutToEdit = item }
                                )
                            }
                        }
                        // Rellenar espacios vacíos en la última fila para mantener el espaciado simétrico
                        val emptySlots = 4 - rowShortcuts.size
                        repeat(emptySlots) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                    if (rowIndex < chunkedShortcuts.size - 1) {
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }
            }
        }

        // Marcadores destacados
        if (bookmarks.isNotEmpty()) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tus Marcadores",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        items(bookmarks) { bookmark ->
                            Card(
                                modifier = Modifier
                                    .width(140.dp)
                                    .clickable { onNavigateToUrl(bookmark.url) }
                                    .testTag("bookmark_chip_${bookmark.id}"),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.Bookmark,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = bookmark.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = bookmark.url,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Historial reciente
        if (recentHistory.isNotEmpty() && !isIncognito) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Visitados recientemente",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                            recentHistory.take(4).forEach { entry ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onNavigateToUrl(entry.url) }
                                        .padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Language,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = entry.title,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = entry.url,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Diálogo para Añadir nuevo acceso directo
    if (isAddDialogOpen) {
        EditShortcutDialog(
            initialShortcut = null,
            onDismissRequest = { isAddDialogOpen = false },
            onSave = { title, url, iconType, colorHex ->
                onAddShortcut(title, url, iconType, colorHex)
                isAddDialogOpen = false
            }
        )
    }

    // Diálogo para Editar o Eliminar acceso directo seleccionado
    shortcutToEdit?.let { shortcut ->
        EditShortcutDialog(
            initialShortcut = shortcut,
            onDismissRequest = { shortcutToEdit = null },
            onSave = { title, url, iconType, colorHex ->
                onUpdateShortcut(
                    shortcut.copy(
                        title = title,
                        url = url,
                        iconType = iconType,
                        colorHex = colorHex
                    )
                )
                shortcutToEdit = null
            },
            onDelete = {
                onDeleteShortcut(shortcut.id)
                shortcutToEdit = null
            }
        )
    }
}

/**
 * Botón circular interactivo para accesos rápidos configurables por el usuario.
 * Admite clic simple para navegar y clic prolongado (long click) para editar o borrar.
 */
@Composable
private fun ConfigurableQuickAccessButton(
    item: ShortcutEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val buttonColor = remember(item.colorHex) {
        ShortcutDesignSystem.parseColor(item.colorHex)
    }
    val iconVector = remember(item.iconType) {
        ShortcutDesignSystem.getIconForType(item.iconType)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(vertical = 4.dp)
            .testTag("quick_link_${item.title}")
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(buttonColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = iconVector,
                contentDescription = item.title,
                tint = buttonColor,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Botón circular para accesos rápidos en la pantalla de inicio.
 */
@Composable
private fun QuickAccessButton(
    item: QuickAccessItem,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp)
            .clickable { onClick() }
            .testTag("quick_link_${item.title}")
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(item.color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.iconVector,
                contentDescription = item.title,
                tint = item.color,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Fila descriptiva para cada escudo de protección activa del Modo Incógnito Avanzado.
 */
@Composable
private fun IncognitoFeatureRow(title: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "•",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 6.dp)
        )
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

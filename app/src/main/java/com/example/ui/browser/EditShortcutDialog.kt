package com.example.ui.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ShortcutEntity

/**
 * Modelo de opción de color para personalizar accesos directos.
 */
data class ShortcutColorOption(val name: String, val hex: String, val color: Color)

/**
 * Modelo de opción de icono para personalizar accesos directos.
 */
data class ShortcutIconOption(val type: String, val icon: ImageVector, val label: String)

/**
 * Proveedor de iconos y colores para los accesos directos configurables.
 */
object ShortcutDesignSystem {
    val colorOptions = listOf(
        ShortcutColorOption("Esmeralda", "#00897B", Color(0xFF00897B)),
        ShortcutColorOption("Azul Marino", "#1976D2", Color(0xFF1976D2)),
        ShortcutColorOption("Naranja", "#FF6F00", Color(0xFFFF6F00)),
        ShortcutColorOption("Rojo", "#D32F2F", Color(0xFFD32F2F)),
        ShortcutColorOption("Púrpura", "#7B1FA2", Color(0xFF7B1FA2)),
        ShortcutColorOption("Grafito", "#374151", Color(0xFF374151)),
        ShortcutColorOption("Turquesa", "#00ACC1", Color(0xFF00ACC1)),
        ShortcutColorOption("Ámbar", "#FFA000", Color(0xFFFFA000))
    )

    val iconOptions = listOf(
        ShortcutIconOption("LANGUAGE", Icons.Default.Language, "Web"),
        ShortcutIconOption("SEARCH", Icons.Default.Search, "Búsqueda"),
        ShortcutIconOption("SPEED", Icons.Default.Speed, "Multimedia"),
        ShortcutIconOption("PUBLIC", Icons.Default.Public, "Comunidad"),
        ShortcutIconOption("CODE", Icons.Default.Code, "Desarrollo"),
        ShortcutIconOption("STAR", Icons.Default.Star, "Destacado"),
        ShortcutIconOption("BOOKMARK", Icons.Default.Bookmark, "Favorito"),
        ShortcutIconOption("STORE", Icons.Default.Store, "Tienda")
    )

    fun getIconForType(type: String): ImageVector {
        return iconOptions.find { it.type.equals(type, ignoreCase = true) }?.icon ?: Icons.Default.Language
    }

    fun parseColor(hex: String, defaultColor: Color = Color(0xFF00897B)): Color {
        return try {
            val cleanHex = hex.removePrefix("#")
            val longColor = cleanHex.toLong(16)
            if (cleanHex.length == 6) {
                Color(longColor or 0x00000000FF000000)
            } else {
                Color(longColor)
            }
        } catch (_: Exception) {
            defaultColor
        }
    }
}

/**
 * Diálogo interactivo para añadir un nuevo acceso directo configurable
 * o editar uno existente, permitiendo al usuario ingresar título, URL,
 * elegir icono y paleta cromática.
 */
@Composable
fun EditShortcutDialog(
    initialShortcut: ShortcutEntity? = null,
    onDismissRequest: () -> Unit,
    onSave: (title: String, url: String, iconType: String, colorHex: String) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var title by remember { mutableStateOf(initialShortcut?.title ?: "") }
    var url by remember { mutableStateOf(initialShortcut?.url ?: "") }
    var selectedIconType by remember { mutableStateOf(initialShortcut?.iconType ?: "LANGUAGE") }
    var selectedColorHex by remember { mutableStateOf(initialShortcut?.colorHex ?: "#00897B") }

    var isTitleError by remember { mutableStateOf(false) }
    var isUrlError by remember { mutableStateOf(false) }

    val isEditing = initialShortcut != null

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = if (isEditing) "Editar Acceso Rápido" else "Nuevo Acceso Rápido",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Vista previa interactiva
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val previewColor = ShortcutDesignSystem.parseColor(selectedColorHex)
                    val previewIcon = ShortcutDesignSystem.getIconForType(selectedIconType)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(90.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(previewColor.copy(alpha = 0.18f))
                                .border(2.dp, previewColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = previewIcon,
                                contentDescription = null,
                                tint = previewColor,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = title.ifBlank { "Vista Previa" },
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 1
                        )
                    }
                }

                // Campo de Título
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        if (isTitleError && it.isNotBlank()) isTitleError = false
                    },
                    label = { Text("Nombre o Título") },
                    placeholder = { Text("Ej: Wikipedia, Mi Blog, Tienda") },
                    singleLine = true,
                    isError = isTitleError,
                    supportingText = if (isTitleError) { { Text("Ingresa un nombre para el acceso") } } else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("shortcut_title_input")
                )

                // Campo de URL
                OutlinedTextField(
                    value = url,
                    onValueChange = {
                        url = it
                        if (isUrlError && it.isNotBlank()) isUrlError = false
                    },
                    label = { Text("Dirección Web (URL)") },
                    placeholder = { Text("ejemplo.com o https://...") },
                    singleLine = true,
                    isError = isUrlError,
                    supportingText = if (isUrlError) { { Text("Ingresa una URL válida") } } else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("shortcut_url_input")
                )

                // Selector de Icono
                Text(
                    text = "Seleccionar Icono:",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(ShortcutDesignSystem.iconOptions) { option ->
                        val isSelected = option.type.equals(selectedIconType, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedIconType = option.type },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = option.icon,
                                contentDescription = option.label,
                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                       else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                // Selector de Color
                Text(
                    text = "Color de Acento:",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(ShortcutDesignSystem.colorOptions) { colorOption ->
                        val isSelected = colorOption.hex.equals(selectedColorHex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(colorOption.color)
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColorHex = colorOption.hex }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanUrl = url.trim()
                    if (cleanUrl.isBlank()) {
                        isUrlError = true
                        return@Button
                    }
                    val cleanTitle = title.trim().ifBlank {
                        cleanUrl.removePrefix("https://").removePrefix("http://").substringBefore("/")
                    }
                    onSave(cleanTitle, cleanUrl, selectedIconType, selectedColorHex)
                },
                modifier = Modifier.testTag("shortcut_save_button")
            ) {
                Text(if (isEditing) "Guardar Cambios" else "Crear Acceso")
            }
        },
        dismissButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isEditing && onDelete != null) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.testTag("shortcut_delete_button")
                    ) {
                        Text("Eliminar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                OutlinedButton(onClick = onDismissRequest) {
                    Text("Cancelar")
                }
            }
        }
    )
}

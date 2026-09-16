package com.example.ui.browser

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Barra de herramientas inferior del navegador.
 * 
 * Contiene los controles de navegación rápida:
 * - Historial atrás / adelante
 * - Página de inicio
 * - Alternar marcador de la página actual
 * - Gestor de pestañas con contador visual de pestañas abiertas
 * - Botón de menú de acciones
 */
@Composable
fun BrowserBottomBar(
    canGoBack: Boolean,
    canGoForward: Boolean,
    isBookmarked: Boolean,
    tabCount: Int,
    onGoBack: () -> Unit,
    onGoForward: () -> Unit,
    onGoHome: () -> Unit,
    onToggleBookmark: () -> Unit,
    onOpenTabs: () -> Unit,
    onOpenMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    BottomAppBar(
        modifier = modifier.height(60.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onGoBack,
                enabled = canGoBack,
                modifier = Modifier.testTag("back_button")
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Atrás",
                    tint = if (canGoBack) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }

            IconButton(
                onClick = onGoForward,
                enabled = canGoForward,
                modifier = Modifier.testTag("forward_button")
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Adelante",
                    tint = if (canGoForward) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }

            IconButton(
                onClick = onGoHome,
                modifier = Modifier.testTag("home_button")
            ) {
                Icon(Icons.Default.Home, contentDescription = "Página de inicio")
            }

            IconButton(
                onClick = onToggleBookmark,
                modifier = Modifier.testTag("bookmark_button")
            ) {
                Icon(
                    imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = if (isBookmarked) "Quitar de marcadores" else "Añadir a marcadores",
                    tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }

            IconButton(
                onClick = onOpenTabs,
                modifier = Modifier.testTag("tabs_button")
            ) {
                BadgedBox(
                    badge = {
                        if (tabCount > 0) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ) {
                                Text(
                                    text = if (tabCount > 99) ":D" else "$tabCount",
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                ) {
                    Icon(Icons.Default.Tab, contentDescription = "Pestañas abiertas")
                }
            }

            Box {
                IconButton(
                    onClick = onOpenMenu,
                    modifier = Modifier.testTag("menu_button")
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Más opciones")
                }
            }
        }
    }
}

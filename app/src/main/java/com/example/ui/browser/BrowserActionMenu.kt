package com.example.ui.browser

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/**
 * Menú desplegable contextual del navegador web.
 */
@Composable
fun BrowserActionMenu(
    expanded: Boolean,
    isIncognito: Boolean,
    isBookmarked: Boolean,
    isHome: Boolean,
    isDesktopMode: Boolean,
    onDismissRequest: () -> Unit,
    onNewTab: () -> Unit,
    onNewProtectedTab: () -> Unit,
    onToggleIncognito: () -> Unit,
    onToggleBookmark: () -> Unit,
    onNavigateToBookmarks: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToDownloads: () -> Unit,
    onNavigateToExtensions: () -> Unit,
    onNavigateToCookies: () -> Unit,
    onToggleDesktopMode: () -> Unit,
    onShare: () -> Unit,
    onNavigateToAccounts: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToReaderMode: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val emeraldColor = Color(0xFF00897B)

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier.testTag("browser_actions_dropdown")
    ) {
        DropdownMenuItem(
            text = { Text("Nueva pestaña") },
            leadingIcon = { Icon(Icons.Default.Tab, contentDescription = null) },
            onClick = {
                onDismissRequest()
                onNewTab()
            },
            modifier = Modifier.testTag("menu_new_tab")
        )

        DropdownMenuItem(
            text = { Text("Nueva pestaña protegida") },
            leadingIcon = { Icon(Icons.Default.Shield, contentDescription = null, tint = emeraldColor) },
            onClick = {
                onDismissRequest()
                onNewProtectedTab()
            },
            modifier = Modifier.testTag("menu_new_protected_tab")
        )

        DropdownMenuItem(
            text = { Text(if (isIncognito) "Modo normal" else "Modo incógnito") },
            leadingIcon = { Icon(Icons.Default.Security, contentDescription = null) },
            onClick = {
                onDismissRequest()
                onToggleIncognito()
            },
            modifier = Modifier.testTag("menu_toggle_incognito")
        )

        DropdownMenuItem(
            text = { Text(if (isBookmarked) "Quitar de marcadores" else "Agregar a marcadores") },
            leadingIcon = {
                Icon(
                    if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = null,
                    tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            },
            enabled = !isHome,
            onClick = {
                onDismissRequest()
                onToggleBookmark()
            },
            modifier = Modifier.testTag("menu_toggle_bookmark")
        )

        DropdownMenuItem(
            text = { Text("Modo Lectura Nativo") },
            leadingIcon = {
                Icon(
                    Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            enabled = !isHome,
            onClick = {
                onDismissRequest()
                onNavigateToReaderMode()
            },
            modifier = Modifier.testTag("menu_reader_mode")
        )

        DropdownMenuItem(
            text = { Text("Marcadores") },
            leadingIcon = { Icon(Icons.Default.Bookmark, contentDescription = null) },
            onClick = {
                onDismissRequest()
                onNavigateToBookmarks()
            },
            modifier = Modifier.testTag("menu_bookmarks")
        )

        DropdownMenuItem(
            text = { Text("Historial") },
            leadingIcon = { Icon(Icons.Default.History, contentDescription = null) },
            onClick = {
                onDismissRequest()
                onNavigateToHistory()
            },
            modifier = Modifier.testTag("menu_history")
        )

        DropdownMenuItem(
            text = { Text("Descargas") },
            leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) },
            onClick = {
                onDismissRequest()
                onNavigateToDownloads()
            },
            modifier = Modifier.testTag("menu_downloads")
        )

        DropdownMenuItem(
            text = { Text("Extensiones") },
            leadingIcon = { Icon(Icons.Default.Extension, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            onClick = {
                onDismissRequest()
                onNavigateToExtensions()
            },
            modifier = Modifier.testTag("menu_extensions")
        )

        DropdownMenuItem(
            text = { Text("Cookies de navegación") },
            leadingIcon = { Icon(Icons.Default.Cookie, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            onClick = {
                onDismissRequest()
                onNavigateToCookies()
            },
            modifier = Modifier.testTag("menu_cookies")
        )

        DropdownMenuItem(
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Sitio para ordenador")
                    Spacer(modifier = Modifier.weight(1f))
                    if (isDesktopMode) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Activo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            },
            leadingIcon = { Icon(Icons.Default.Computer, contentDescription = null) },
            onClick = {
                onDismissRequest()
                onToggleDesktopMode()
            },
            modifier = Modifier.testTag("menu_desktop_site")
        )

        if (!isHome) {
            DropdownMenuItem(
                text = { Text("Compartir enlace") },
                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                onClick = {
                    onDismissRequest()
                    onShare()
                },
                modifier = Modifier.testTag("menu_share")
            )
        }

        DropdownMenuItem(
            text = { Text("Cuentas y acceso web") },
            leadingIcon = { Icon(Icons.Default.AccountCircle, contentDescription = null) },
            onClick = {
                onDismissRequest()
                onNavigateToAccounts()
            },
            modifier = Modifier.testTag("menu_accounts")
        )

        DropdownMenuItem(
            text = { Text("Ajustes") },
            leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
            onClick = {
                onDismissRequest()
                onNavigateToSettings()
            },
            modifier = Modifier.testTag("menu_settings")
        )
    }
}

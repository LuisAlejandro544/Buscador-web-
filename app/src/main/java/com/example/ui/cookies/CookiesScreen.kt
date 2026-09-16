package com.example.ui.cookies

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.CookieEntity
import com.example.viewmodel.BrowserViewModel

/**
 * Filtros de visualización para la auditoría de cookies.
 */
enum class CookieFilterTab {
    ALL, TRACKERS, PROTECTED, BY_DOMAIN
}

/**
 * Pantalla del Administrador de Cookies de Navegación.
 * 
 * Permite auditar, filtrar y eliminar cookies con precisión:
 * - Filtros por tipo: Todas, Rastreadores/Publicidad, Aisladas en burbujas y Agrupadas por sitio.
 * - Búsqueda en tiempo real por nombre de cookie o dominio web.
 * - Purga selectiva (solo rastreadores, cookies de pestañas protegidas o borrado total).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CookiesScreen(
    viewModel: BrowserViewModel,
    onNavigateBack: () -> Unit
) {
    val allCookies by viewModel.allCookies.collectAsState()
    val totalCount by viewModel.cookieCount.collectAsState()
    val trackerCount by viewModel.trackerCookieCount.collectAsState()
    val protectedCount by viewModel.protectedCookieCount.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(CookieFilterTab.ALL) }
    var showClearAllConfirmation by remember { mutableStateOf(false) }
    var showClearTrackersConfirmation by remember { mutableStateOf(false) }

    val emeraldColor = Color(0xFF00897B)

    // Filtrar cookies según la búsqueda y el filtro seleccionado
    val filteredCookies = remember(allCookies, searchQuery, selectedFilter) {
        var list = allCookies
        if (searchQuery.isNotBlank()) {
            list = list.filter {
                it.domain.contains(searchQuery, ignoreCase = true) ||
                it.name.contains(searchQuery, ignoreCase = true)
            }
        }
        when (selectedFilter) {
            CookieFilterTab.ALL -> list
            CookieFilterTab.TRACKERS -> list.filter { it.isTracker }
            CookieFilterTab.PROTECTED -> list.filter { it.isProtected }
            CookieFilterTab.BY_DOMAIN -> list
        }
    }

    // Agrupación de cookies por dominio para la vista por sitio
    val domainGroups = remember(filteredCookies) {
        filteredCookies.groupBy { it.domain }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cookies de navegación", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("cookies_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (trackerCount > 0) {
                        IconButton(
                            onClick = { showClearTrackersConfirmation = true },
                            modifier = Modifier.testTag("clear_trackers_top_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Radar,
                                contentDescription = "Eliminar rastreadores",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    if (allCookies.isNotEmpty()) {
                        IconButton(
                            onClick = { showClearAllConfirmation = true },
                            modifier = Modifier.testTag("clear_all_cookies_button")
                        ) {
                            Icon(Icons.Default.ClearAll, contentDescription = "Borrar todas las cookies")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Cabecera de métricas visuales
            CookieMetricsHeader(
                totalCount = totalCount,
                trackerCount = trackerCount,
                protectedCount = protectedCount
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Campo de búsqueda en tiempo real
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("cookie_search_input"),
                placeholder = { Text("Buscar cookie o dominio (ej: google, _ga)...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Limpiar búsqueda")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Chips de filtrado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == CookieFilterTab.ALL,
                    onClick = { selectedFilter = CookieFilterTab.ALL },
                    label = { Text("Todas") },
                    modifier = Modifier.testTag("chip_all_cookies")
                )
                FilterChip(
                    selected = selectedFilter == CookieFilterTab.TRACKERS,
                    onClick = { selectedFilter = CookieFilterTab.TRACKERS },
                    label = { Text("Rastreadores") },
                    leadingIcon = { Icon(Icons.Default.Radar, contentDescription = null, modifier = Modifier.size(14.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    modifier = Modifier.testTag("chip_tracker_cookies")
                )
                FilterChip(
                    selected = selectedFilter == CookieFilterTab.PROTECTED,
                    onClick = { selectedFilter = CookieFilterTab.PROTECTED },
                    label = { Text("Aisladas") },
                    leadingIcon = { Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(14.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = emeraldColor.copy(alpha = 0.2f),
                        selectedLabelColor = emeraldColor
                    ),
                    modifier = Modifier.testTag("chip_protected_cookies")
                )
                FilterChip(
                    selected = selectedFilter == CookieFilterTab.BY_DOMAIN,
                    onClick = { selectedFilter = CookieFilterTab.BY_DOMAIN },
                    label = { Text("Por Sitio") },
                    leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(14.dp)) },
                    modifier = Modifier.testTag("chip_domain_cookies")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Lista de contenido: Por Dominio o Individual
            if (filteredCookies.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Cookie,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "No se encontraron cookies para '$searchQuery'"
                                   else "No hay cookies registradas",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (selectedFilter == CookieFilterTab.BY_DOMAIN) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(domainGroups.keys.toList(), key = { it }) { domain ->
                        val cookiesInDomain = domainGroups[domain] ?: emptyList()
                        DomainCookieGroupCard(
                            domain = domain,
                            cookies = cookiesInDomain,
                            onDeleteCookie = { cookie -> viewModel.deleteCookie(cookie.id, cookie.domain) },
                            onDeleteDomain = { host -> viewModel.deleteCookiesByDomain(host) }
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredCookies, key = { it.id }) { cookie ->
                        CookieDetailCard(
                            cookie = cookie,
                            onDelete = { item -> viewModel.deleteCookie(item.id, item.domain) }
                        )
                    }
                }
            }
        }

        // Diálogo de confirmación para eliminar rastreadores
        if (showClearTrackersConfirmation) {
            AlertDialog(
                onDismissRequest = { showClearTrackersConfirmation = false },
                icon = { Icon(Icons.Default.Radar, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                title = { Text("Eliminar todos los rastreadores") },
                text = { Text("¿Deseas eliminar las $trackerCount cookies clasificadas como rastreadores de publicidad y analítica?") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteTrackerCookies()
                            showClearTrackersConfirmation = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.testTag("confirm_delete_trackers_btn")
                    ) {
                        Text("Eliminar rastreadores")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearTrackersConfirmation = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        // Diálogo de confirmación para vaciar todas las cookies
        if (showClearAllConfirmation) {
            AlertDialog(
                onDismissRequest = { showClearAllConfirmation = false },
                icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                title = { Text("Vaciar todas las cookies") },
                text = { Text("¿Deseas eliminar las $totalCount cookies registradas en el navegador y en las sesiones web nativas de GeckoView? Se cerrarán las sesiones en sitios webs.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.clearAllCookies()
                            showClearAllConfirmation = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.testTag("confirm_clear_all_cookies_btn")
                    ) {
                        Text("Vaciar todo")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearAllConfirmation = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}

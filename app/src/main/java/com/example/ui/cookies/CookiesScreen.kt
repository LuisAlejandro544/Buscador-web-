package com.example.ui.cookies

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.CookieEntity
import com.example.viewmodel.BrowserViewModel

/**
 * Filtro de visualización de cookies de navegación.
 */
enum class CookieFilterTab {
    ALL, TRACKERS, BY_SITE
}

/**
 * Pantalla dedicada de gestión y auditoría de Cookies de Navegación.
 * 
 * Permite a los usuarios inspeccionar todas las cookies depositadas por los sitios web,
 * categorizarlas entre cookies funcionales y rastreadores de publicidad/analítica,
 * filtrar por dominio y realizar eliminaciones selectivas o masivas.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CookiesScreen(
    viewModel: BrowserViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allCookies by viewModel.allCookies.collectAsStateWithLifecycle()
    val trackerCookies by viewModel.trackerCookies.collectAsStateWithLifecycle()
    val totalCount by viewModel.cookieCount.collectAsStateWithLifecycle()
    val trackerCount by viewModel.trackerCookieCount.collectAsStateWithLifecycle()
    val domains by viewModel.cookieDomains.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(CookieFilterTab.ALL) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var showDeleteTrackersDialog by remember { mutableStateOf(false) }
    var domainToDelete by remember { mutableStateOf<String?>(null) }

    // Filtrar la lista según la búsqueda y la pestaña seleccionada
    val filteredCookies = remember(allCookies, trackerCookies, searchQuery, selectedTab) {
        val baseList = when (selectedTab) {
            CookieFilterTab.ALL -> allCookies
            CookieFilterTab.TRACKERS -> trackerCookies
            CookieFilterTab.BY_SITE -> allCookies
        }
        if (searchQuery.isBlank()) {
            baseList
        } else {
            baseList.filter {
                it.domain.contains(searchQuery, ignoreCase = true) ||
                        it.name.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        modifier = modifier.testTag("cookies_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Cookies de navegación",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$totalCount almacenadas • $trackerCount rastreadores",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("cookies_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver al navegador"
                        )
                    }
                },
                actions = {
                    if (trackerCount > 0) {
                        IconButton(
                            onClick = { showDeleteTrackersDialog = true },
                            modifier = Modifier.testTag("btn_delete_trackers")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Eliminar rastreadores",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    if (totalCount > 0) {
                        IconButton(
                            onClick = { showDeleteAllDialog = true },
                            modifier = Modifier.testTag("btn_clear_all_cookies")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Borrar todas las cookies",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tarjeta superior con métricas de privacidad
            CookieMetricsHeader(
                totalCookies = totalCount,
                trackerCount = trackerCount,
                siteCount = domains.size,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Barra de búsqueda rápida
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag("cookies_search_input"),
                placeholder = { Text("Buscar por sitio o cookie...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpiar búsqueda")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Chips de filtrado de pestañas
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedTab == CookieFilterTab.ALL,
                    onClick = { selectedTab = CookieFilterTab.ALL },
                    label = { Text("Todas ($totalCount)") },
                    leadingIcon = { Icon(Icons.Default.Cookie, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.testTag("tab_all_cookies")
                )
                FilterChip(
                    selected = selectedTab == CookieFilterTab.TRACKERS,
                    onClick = { selectedTab = CookieFilterTab.TRACKERS },
                    label = { Text("Rastreadores ($trackerCount)") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = if (trackerCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    modifier = Modifier.testTag("tab_tracker_cookies")
                )
                FilterChip(
                    selected = selectedTab == CookieFilterTab.BY_SITE,
                    onClick = { selectedTab = CookieFilterTab.BY_SITE },
                    label = { Text("Por Sitio") },
                    leadingIcon = { Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.testTag("tab_by_site_cookies")
                )
            }

            // Lista de contenido según pestaña activa
            if (filteredCookies.isEmpty()) {
                EmptyCookiesState(searchQuery = searchQuery)
            } else {
                if (selectedTab == CookieFilterTab.BY_SITE) {
                    // Vista agrupada por dominios
                    val grouped = remember(filteredCookies) {
                        filteredCookies.groupBy { it.domain }
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(grouped.keys.toList(), key = { it }) { domain ->
                            val domainCookies = grouped[domain] ?: emptyList()
                            DomainCookieGroupCard(
                                domain = domain,
                                cookies = domainCookies,
                                onDeleteDomain = { domainToDelete = domain },
                                onDeleteSingleCookie = { id -> viewModel.deleteCookie(id, domain) }
                            )
                        }
                    }
                } else {
                    // Vista plana detallada
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredCookies, key = { it.id }) { cookie ->
                            CookieDetailCard(
                                cookie = cookie,
                                onDelete = { viewModel.deleteCookie(cookie.id, cookie.domain) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Diálogos de confirmación
    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text("¿Borrar todas las cookies?") },
            text = { Text("Se eliminarán todas las sesiones activas, preferencias guardadas y rastreadores de todos los sitios web visitados.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllCookies()
                        showDeleteAllDialog = false
                    },
                    modifier = Modifier.testTag("confirm_clear_all_cookies")
                ) {
                    Text("Borrar Todo", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showDeleteTrackersDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteTrackersDialog = false },
            title = { Text("¿Eliminar rastreadores?") },
            text = { Text("Se eliminarán las $trackerCount cookies identificadas como rastreadores publicitarios y analíticos de terceros.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTrackerCookies()
                        showDeleteTrackersDialog = false
                    },
                    modifier = Modifier.testTag("confirm_delete_trackers")
                ) {
                    Text("Eliminar Rastreadores", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteTrackersDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    domainToDelete?.let { domain ->
        AlertDialog(
            onDismissRequest = { domainToDelete = null },
            title = { Text("¿Eliminar cookies de $domain?") },
            text = { Text("Se cerrará la sesión en este sitio y se eliminarán todas las cookies asociadas.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCookiesByDomain(domain)
                        domainToDelete = null
                    }
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { domainToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

/**
 * Encabezado con 3 tarjetas métricas de estado de privacidad.
 */
@Composable
fun CookieMetricsHeader(
    totalCookies: Int,
    trackerCount: Int,
    siteCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MetricItem(
                count = totalCookies.toString(),
                label = "Total Cookies",
                icon = Icons.Default.Cookie,
                iconTint = MaterialTheme.colorScheme.primary
            )
            MetricItem(
                count = trackerCount.toString(),
                label = "Rastreadores",
                icon = Icons.Default.Shield,
                iconTint = if (trackerCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
            )
            MetricItem(
                count = siteCount.toString(),
                label = "Sitios Web",
                icon = Icons.Default.Language,
                iconTint = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

@Composable
fun MetricItem(
    count: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = count, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * Tarjeta para mostrar y gestionar una cookie individual con sus atributos técnicos.
 */
@Composable
fun CookieDetailCard(
    cookie: CookieEntity,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showValue by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (cookie.isTracker) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.12f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (cookie.isTracker) Icons.Default.Shield else Icons.Default.Cookie,
                        contentDescription = null,
                        tint = if (cookie.isTracker) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = cookie.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = cookie.domain,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar cookie",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Etiquetas de estado
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (cookie.isTracker) {
                    BadgeChip(
                        label = "Rastreador",
                        backgroundColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                        textColor = MaterialTheme.colorScheme.error
                    )
                } else {
                    BadgeChip(
                        label = cookie.category,
                        backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        textColor = MaterialTheme.colorScheme.primary
                    )
                }

                if (cookie.isSecure) {
                    BadgeChip(
                        label = "HTTPS Seguro",
                        backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
                        textColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                if (cookie.isHttpOnly) {
                    BadgeChip(
                        label = "HttpOnly",
                        backgroundColor = MaterialTheme.colorScheme.tertiaryContainer,
                        textColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Valor de la cookie (ocultable para privacidad)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { showValue = !showValue },
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (showValue) cookie.value else "Valor: •••••••••••• (Toca para ver)",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        maxLines = if (showValue) 3 else 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = if (showValue) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Tarjeta agrupada por dominio con opción de expandir/contraer y eliminar todas las cookies del sitio.
 */
@Composable
fun DomainCookieGroupCard(
    domain: String,
    cookies: List<CookieEntity>,
    onDeleteDomain: () -> Unit,
    onDeleteSingleCookie: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val trackersInDomain = cookies.count { it.isTracker }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = domain,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "${cookies.size} cookies",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (trackersInDomain > 0) {
                                Text(
                                    text = "• $trackersInDomain rastreadores",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDeleteDomain, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar cookies de este dominio",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    cookies.forEach { cookie ->
                        CookieDetailCard(
                            cookie = cookie,
                            onDelete = { onDeleteSingleCookie(cookie.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BadgeChip(
    label: String,
    backgroundColor: Color,
    textColor: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun EmptyCookiesState(searchQuery: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Cookie,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (searchQuery.isNotEmpty()) "No se encontraron cookies coincidentes" else "No hay cookies guardadas",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (searchQuery.isNotEmpty()) "Prueba con otro término de búsqueda" else "Las cookies generadas por los sitios web aparecerán aquí.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

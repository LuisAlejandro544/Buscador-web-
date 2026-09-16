package com.example.ui.tabs

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.TabEntity
import com.example.viewmodel.BrowserViewModel

/**
 * Pantalla de gestión de pestañas del navegador.
 * Muestra las pestañas activas en cuadrícula, permitiendo cambiar entre ellas,
 * cerrarlas individualmente, cerrar todas o crear nuevas pestañas normales o de incógnito.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabsScreen(
    viewModel: BrowserViewModel,
    onNavigateBack: () -> Unit
) {
    val normalTabs by viewModel.normalTabs.collectAsState()
    val incognitoTabs by viewModel.incognitoTabs.collectAsState()
    val activeTabId by viewModel.activeTabId.collectAsState()
    val isIncognito by viewModel.isIncognitoMode.collectAsState()

    var selectedTabIndex by remember(isIncognito) {
        mutableIntStateOf(if (isIncognito) 1 else 0)
    }

    var showCloseAllDialog by remember { mutableStateOf(false) }

    val currentDisplayTabs = if (selectedTabIndex == 1) incognitoTabs else normalTabs

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Pestañas (${currentDisplayTabs.size})",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("tabs_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (currentDisplayTabs.isNotEmpty()) {
                        IconButton(
                            onClick = { showCloseAllDialog = true },
                            modifier = Modifier.testTag("close_all_tabs_button")
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Cerrar todas las pestañas")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val isIncog = selectedTabIndex == 1
                    viewModel.createNewTab("about:home", isIncognito = isIncog)
                    onNavigateBack()
                },
                modifier = Modifier.testTag("new_tab_fab"),
                containerColor = if (selectedTabIndex == 1) MaterialTheme.colorScheme.tertiary
                                 else MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nueva pestaña")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Selector de Modo Normal vs Incógnito
            PrimaryTabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = {
                        selectedTabIndex = 0
                        viewModel.toggleIncognitoMode(false)
                    },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Normales (${normalTabs.size})")
                        }
                    },
                    modifier = Modifier.testTag("tab_mode_normal")
                )

                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = {
                        selectedTabIndex = 1
                        viewModel.toggleIncognitoMode(true)
                    },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Privadas (${incognitoTabs.size})")
                        }
                    },
                    modifier = Modifier.testTag("tab_mode_incognito")
                )
            }

            if (currentDisplayTabs.isEmpty()) {
                // Estado vacío
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = if (selectedTabIndex == 1) Icons.Default.Security else Icons.Default.Public,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (selectedTabIndex == 1) "No hay pestañas privadas abiertas" else "No hay pestañas abiertas",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Presiona el botón + para abrir una nueva pestaña",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Cuadrícula de tarjetas de pestañas
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(currentDisplayTabs, key = { it.id }) { tab ->
                        TabCardItem(
                            tab = tab,
                            isActive = tab.id == activeTabId,
                            onClick = {
                                viewModel.switchToTab(tab.id)
                                onNavigateBack()
                            },
                            onClose = {
                                viewModel.closeTab(tab.id)
                            }
                        )
                    }
                }
            }
        }
    }

    // Diálogo de confirmación para cerrar todas las pestañas
    if (showCloseAllDialog) {
        AlertDialog(
            onDismissRequest = { showCloseAllDialog = false },
            title = { Text("Cerrar todas las pestañas") },
            text = { Text("¿Deseas cerrar todas las pestañas de este modo?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCloseAllDialog = false
                        viewModel.closeAllTabs(selectedTabIndex == 1)
                        onNavigateBack()
                    }
                ) {
                    Text("Cerrar todas", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCloseAllDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

/**
 * Tarjeta individual que muestra una pestaña en la cuadrícula.
 */
@Composable
private fun TabCardItem(
    tab: TabEntity,
    isActive: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit
) {
    val borderColor = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                         else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
            .clickable { onClick() }
            .testTag("tab_card_${tab.id}"),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(if (isActive) 2.dp else 1.dp, borderColor),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
        ) {
            // Barra superior de la tarjeta (icono, título y botón cerrar)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (tab.isIncognito) Icons.Default.Lock else Icons.Default.Language,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (tab.url == "about:home") "Inicio" else tab.title,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(24.dp)
                        .testTag("close_tab_${tab.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar pestaña",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Vista previa simulada de la página
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp)),
                color = MaterialTheme.colorScheme.surface
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (tab.url == "about:home") "Página de Inicio" else tab.url,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Indicador de pestaña activa
            if (isActive) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Pestaña activa",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Activa",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

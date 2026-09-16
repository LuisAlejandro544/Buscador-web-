package com.example.ui.tabs

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Shield
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.TabEntity
import com.example.viewmodel.BrowserViewModel

/**
 * Pantalla de gestión de pestañas del navegador.
 * Permite alternar entre pestañas Normales, Protegidas (contenedores aislados) e Incógnito,
 * cerrarlas individualmente, cerrar todas o abrir nuevas pestañas con aislamiento de cookies.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabsScreen(
    viewModel: BrowserViewModel,
    onNavigateBack: () -> Unit
) {
    val normalTabs by viewModel.normalTabs.collectAsState()
    val protectedTabs by viewModel.protectedTabs.collectAsState()
    val incognitoTabs by viewModel.incognitoTabs.collectAsState()
    val activeTabId by viewModel.activeTabId.collectAsState()
    val isIncognito by viewModel.isIncognitoMode.collectAsState()
    val isProtected by viewModel.isProtectedMode.collectAsState()

    // 0: Normales, 1: Protegidas, 2: Privadas
    var selectedTabIndex by remember(isIncognito, isProtected) {
        mutableIntStateOf(
            when {
                isProtected -> 1
                isIncognito -> 2
                else -> 0
            }
        )
    }

    var showCloseAllDialog by remember { mutableStateOf(false) }

    val currentDisplayTabs = when (selectedTabIndex) {
        1 -> protectedTabs
        2 -> incognitoTabs
        else -> normalTabs
    }

    val currentTabMode = when (selectedTabIndex) {
        1 -> BrowserViewModel.TabMode.PROTECTED
        2 -> BrowserViewModel.TabMode.INCOGNITO
        else -> BrowserViewModel.TabMode.NORMAL
    }

    val emeraldColor = Color(0xFF00897B)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (selectedTabIndex) {
                            1 -> "Pestañas Protegidas (${currentDisplayTabs.size})"
                            2 -> "Pestañas Privadas (${currentDisplayTabs.size})"
                            else -> "Pestañas (${currentDisplayTabs.size})"
                        },
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
                    when (selectedTabIndex) {
                        1 -> viewModel.createProtectedTab("about:home")
                        2 -> viewModel.createNewTab("about:home", isIncognito = true, isProtected = false)
                        else -> viewModel.createNewTab("about:home", isIncognito = false, isProtected = false)
                    }
                    onNavigateBack()
                },
                modifier = Modifier.testTag("new_tab_fab"),
                containerColor = when (selectedTabIndex) {
                    1 -> emeraldColor
                    2 -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.primary
                }
            ) {
                Icon(
                    imageVector = when (selectedTabIndex) {
                        1 -> Icons.Default.Shield
                        else -> Icons.Default.Add
                    },
                    contentDescription = "Nueva pestaña"
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Selector de tres modos: Normales, Protegidas y Privadas
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
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Normal (${normalTabs.size})")
                        }
                    },
                    modifier = Modifier.testTag("tab_mode_normal")
                )

                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = {
                        selectedTabIndex = 1
                        viewModel.switchToProtectedMode()
                    },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = emeraldColor, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Protegidas (${protectedTabs.size})", color = if (selectedTabIndex == 1) emeraldColor else Color.Unspecified)
                        }
                    },
                    modifier = Modifier.testTag("tab_mode_protected")
                )

                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = {
                        selectedTabIndex = 2
                        viewModel.toggleIncognitoMode(true)
                    },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Privadas (${incognitoTabs.size})")
                        }
                    },
                    modifier = Modifier.testTag("tab_mode_incognito")
                )
            }

            // Banner explicativo para Modo Protegido
            if (selectedTabIndex == 1) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = emeraldColor.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = emeraldColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Aislamiento Total de Cookies y Sesión",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = emeraldColor
                            )
                            Text(
                                text = "Las cookies y cuentas de esta pestaña no tocan tus pestañas normales ni otras cuentas.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
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
                            imageVector = when (selectedTabIndex) {
                                1 -> Icons.Default.Shield
                                2 -> Icons.Default.Lock
                                else -> Icons.Default.Public
                            },
                            contentDescription = null,
                            tint = when (selectedTabIndex) {
                                1 -> emeraldColor
                                2 -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.primary
                            }.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = when (selectedTabIndex) {
                                1 -> "No hay pestañas protegidas abiertas"
                                2 -> "No hay pestañas privadas abiertas"
                                else -> "No hay pestañas abiertas"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = when (selectedTabIndex) {
                                1 -> "Abre una pestaña protegida para aislar cookies de sitios web sospechosos o iniciar una sesión secundaria"
                                2 -> "Abre una pestaña de incógnito para no registrar historial"
                                else -> "Presiona el botón + para abrir una nueva pestaña"
                            },
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
                    contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp),
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
            text = {
                Text(
                    when (selectedTabIndex) {
                        1 -> "¿Deseas cerrar todas las pestañas protegidas y purgar sus datos aislados?"
                        2 -> "¿Deseas cerrar todas las pestañas privadas?"
                        else -> "¿Deseas cerrar todas las pestañas normales?"
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCloseAllDialog = false
                        viewModel.closeAllTabs(currentTabMode)
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
 * Tarjeta individual que muestra una pestaña en la cuadrícula con soporte para pestañas protegidas.
 */
@Composable
private fun TabCardItem(
    tab: TabEntity,
    isActive: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit
) {
    val emeraldColor = Color(0xFF00897B)
    val borderColor = when {
        isActive && tab.isProtected -> emeraldColor
        isActive -> MaterialTheme.colorScheme.primary
        tab.isProtected -> emeraldColor.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.outlineVariant
    }
    val containerColor = when {
        isActive && tab.isProtected -> emeraldColor.copy(alpha = 0.15f)
        isActive -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
        tab.isProtected -> emeraldColor.copy(alpha = 0.07f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
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
                    imageVector = when {
                        tab.isProtected -> Icons.Default.Shield
                        tab.isIncognito -> Icons.Default.Lock
                        else -> Icons.Default.Language
                    },
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = when {
                        tab.isProtected -> emeraldColor
                        isActive -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (tab.url == "about:home") {
                        if (tab.isProtected) "Protegida" else "Inicio"
                    } else tab.title,
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

            Spacer(modifier = Modifier.height(6.dp))

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
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (tab.isProtected) {
                            Text(
                                text = "🛡️ Contenedor Aislado",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                color = emeraldColor
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                        }
                        Text(
                            text = if (tab.url == "about:home") "Página de Inicio" else tab.url,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
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
                        tint = if (tab.isProtected) emeraldColor else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Activa",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (tab.isProtected) emeraldColor else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

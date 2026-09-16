package com.example.ui.tabs

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.BrowserViewModel

/**
 * Pantalla de gestión de pestañas del navegador.
 * 
 * Características clave:
 * - Soporte para 3 modalidades: Normales, Protegidas (aislamiento de cookies) e Incógnito.
 * - En los modos Protegidas e Incógnito las pestañas NO se crean automáticamente; deben crearse de forma manual.
 * - Muestra la miniatura gráfica real de dónde el usuario dejó cada página web.
 * - Sistema de reposo/hibernación inteligente tras 5 minutos de inactividad:
 *   libera la memoria RAM del teléfono y suspende el proceso de GeckoView sin perder la URL ni la miniatura.
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

    // Estado reactivo de hibernación (pestañas en reposo tras 5 minutos) y miniaturas en caché
    val hibernatedTabIds by viewModel.hibernatedTabIds.collectAsState()
    val tabThumbnails by viewModel.tabThumbnails.collectAsState()

    // 0: Normales, 1: Protegidas, 2: Incógnito
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
                            2 -> "Pestañas de Incógnito (${currentDisplayTabs.size})"
                            else -> "Pestañas (${currentDisplayTabs.size})"
                        },
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("tabs_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver al navegador")
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
            // Selector de modos: Normales, Protegidas e Incógnito
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
                            Text("Incógnito (${incognitoTabs.size})")
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
            } else if (selectedTabIndex == 2) {
                // Banner explicativo de blindaje avanzado para Modo Incógnito
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Blindaje Avanzado: RFP, Anti-WebRTC, DoH y Purga RAM",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Protección real contra huella digital, sin fugas de IP, con DNS cifrado y destrucción total de RAM.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (currentDisplayTabs.isEmpty()) {
                // Estado vacío con acción manual obligatoria para Protegidas e Incógnito
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
                                2 -> "No hay pestañas de incógnito abiertas"
                                else -> "No hay pestañas abiertas"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = when (selectedTabIndex) {
                                1 -> "Las pestañas protegidas aíslan completamente cookies y cuentas secundarias. Crea una manualmente."
                                2 -> "Las pestañas de incógnito no guardan historial ni caché. Crea una manualmente para comenzar."
                                else -> "Presiona el botón inferior para abrir una nueva pestaña."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        // Botón de creación manual
                        if (selectedTabIndex == 1) {
                            Button(
                                onClick = {
                                    viewModel.createProtectedTab("about:home")
                                    onNavigateBack()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = emeraldColor),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("create_protected_tab_manual_button")
                            ) {
                                Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Crear pestaña protegida manualmente")
                            }
                        } else if (selectedTabIndex == 2) {
                            Button(
                                onClick = {
                                    viewModel.createNewTab("about:home", isIncognito = true, isProtected = false)
                                    onNavigateBack()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("create_incognito_tab_manual_button")
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Crear pestaña de incógnito manualmente")
                            }
                        }
                    }
                }
            } else {
                // Cuadrícula de tarjetas de pestañas con miniaturas y detección de reposo
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
                            isHibernated = tab.id in hibernatedTabIds,
                            thumbnail = tabThumbnails[tab.id],
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
                        2 -> "¿Deseas cerrar todas las pestañas de incógnito?"
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

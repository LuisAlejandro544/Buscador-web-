package com.example.ui.extension

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import com.example.browser.extension.WebExtensionModel
import com.example.viewmodel.BrowserViewModel
import kotlinx.coroutines.launch

/**
 * Pantalla del Gestor de Extensiones y Complementos WebExtension (GeckoView).
 * 
 * Organizada en tres pestañas modulares:
 * 1. [InstalledExtensionsTab]: Extensiones instaladas y switches de activación.
 * 2. [RecommendedCatalogTab]: Catálogo oficial de Mozilla Add-ons (uBlock, Dark Reader, Privacy Badger, etc.).
 * 3. [CustomUrlInstallerTab]: Instalador de paquetes .xpi por enlace HTTPS directo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtensionsScreen(
    viewModel: BrowserViewModel,
    onNavigateBack: () -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Instaladas", "Catálogo Recomendado", "Instalar por URL")

    val installedExtensions by viewModel.installedExtensions.collectAsState()
    val downloadProgressMap by viewModel.extensionDownloadProgress.collectAsState()
    val isLoading by viewModel.isExtensionsLoading.collectAsState()
    val catalog = viewModel.recommendedExtensions

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var extensionToUninstall by remember { mutableStateOf<WebExtensionModel?>(null) }

    LaunchedEffect(Unit) {
        viewModel.refreshInstalledExtensions()
    }

    val installedIds = remember(installedExtensions) {
        installedExtensions.map { it.id }.toSet()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Extensiones y Complementos", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("extensions_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refreshInstalledExtensions() },
                        modifier = Modifier.testTag("refresh_extensions_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refrescar")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            PrimaryTabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            val badge = if (index == 0 && installedExtensions.isNotEmpty()) " (${installedExtensions.size})" else ""
                            Text(text = "$title$badge", fontWeight = FontWeight.SemiBold)
                        },
                        modifier = Modifier.testTag("extension_tab_$index")
                    )
                }
            }

            when (selectedTabIndex) {
                0 -> InstalledExtensionsTab(
                    installedExtensions = installedExtensions,
                    isLoading = isLoading,
                    onToggleEnabled = { id, enabled -> viewModel.toggleExtensionEnabled(id, enabled) },
                    onUninstall = { ext -> extensionToUninstall = ext }
                )
                1 -> RecommendedCatalogTab(
                    catalog = catalog,
                    installedIds = installedIds,
                    downloadProgressMap = downloadProgressMap,
                    onInstall = { item ->
                        viewModel.downloadAndInstallExtension(
                            recommended = item,
                            onSuccess = {
                                scope.launch {
                                    snackbarHostState.showSnackbar("¡${item.name} instalada correctamente!")
                                }
                            },
                            onError = { error ->
                                scope.launch {
                                    snackbarHostState.showSnackbar("Error: $error")
                                }
                            }
                        )
                    }
                )
                2 -> CustomUrlInstallerTab(
                    onInstallUrl = { url ->
                        viewModel.installCustomExtension(
                            downloadUrl = url,
                            onSuccess = {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Extensión instalada con éxito")
                                    selectedTabIndex = 0
                                }
                            },
                            onError = { error ->
                                scope.launch {
                                    snackbarHostState.showSnackbar(error)
                                }
                            }
                        )
                    }
                )
            }
        }

        // Diálogo de confirmación para desinstalar extensión
        extensionToUninstall?.let { ext ->
            AlertDialog(
                onDismissRequest = { extensionToUninstall = null },
                icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                title = { Text("¿Desinstalar ${ext.name}?") },
                text = { Text("Se eliminará esta extensión del motor de navegación y se perderán sus configuraciones personalizadas.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.uninstallExtension(ext.id)
                            extensionToUninstall = null
                            scope.launch {
                                snackbarHostState.showSnackbar("Extensión desinstalada")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.testTag("confirm_uninstall_extension_btn")
                    ) {
                        Text("Desinstalar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { extensionToUninstall = null }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}

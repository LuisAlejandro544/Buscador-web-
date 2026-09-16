package com.example.ui.extension

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.browser.extension.RecommendedExtension
import com.example.browser.extension.WebExtensionModel
import com.example.viewmodel.BrowserViewModel
import kotlinx.coroutines.launch

/**
 * Pantalla completa e independiente para la gestión y descarga de extensiones WebExtension.
 * 
 * Cumple con la directriz de pantallas dedicadas:
 * Ofrece tres pestañas funcionales:
 * 1. "Instaladas": Visualización, activación/desactivación y desinstalación de extensiones activas.
 * 2. "Recomendadas": Catálogo oficial de Mozilla Add-ons (uBlock Origin, Dark Reader, TWP, ClearURLs) con descarga bajo demanda.
 * 3. "Instalar URL": Instalador directo para paquetes .xpi seguros vía HTTPS.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtensionsScreen(
    viewModel: BrowserViewModel,
    onNavigateBack: () -> Unit
) {
    val installedList by viewModel.installedExtensions.collectAsStateWithLifecycle()
    val downloadProgressMap by viewModel.extensionDownloadProgress.collectAsStateWithLifecycle()
    val isExtensionsLoading by viewModel.isExtensionsLoading.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Instaladas, 1: Recomendadas, 2: Añadir URL
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var extensionToDelete by remember { mutableStateOf<WebExtensionModel?>(null) }
    var customUrlInput by remember { mutableStateOf("") }
    var isInstallingCustom by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshInstalledExtensions()
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("extensions_screen"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Extensiones del Navegador",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("extensions_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Regresar al navegador"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refreshInstalledExtensions() },
                        modifier = Modifier.testTag("extensions_refresh_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Actualizar extensiones"
                        )
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
            // Pestañas superiores de navegación de extensiones
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Instaladas (${installedList.size})") },
                    modifier = Modifier.testTag("tab_installed_extensions")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Recomendadas") },
                    modifier = Modifier.testTag("tab_recommended_extensions")
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Instalar URL") },
                    modifier = Modifier.testTag("tab_custom_url_extension")
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                when (selectedTab) {
                    0 -> InstalledExtensionsTab(
                        installedList = installedList,
                        isLoading = isExtensionsLoading,
                        onToggle = { ext, enabled ->
                            viewModel.toggleExtensionEnabled(ext.id, enabled)
                        },
                        onDeleteClick = { ext ->
                            extensionToDelete = ext
                        },
                        onExploreCatalog = { selectedTab = 1 }
                    )
                    1 -> RecommendedCatalogTab(
                        catalog = viewModel.recommendedExtensions,
                        installedList = installedList,
                        downloadProgressMap = downloadProgressMap,
                        onInstall = { recommended ->
                            scope.launch {
                                snackbarHostState.showSnackbar("Descargando ${recommended.name} desde Mozilla...")
                            }
                            viewModel.downloadAndInstallExtension(
                                recommended = recommended,
                                onSuccess = {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("¡${recommended.name} instalada y activada!")
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
                        urlInput = customUrlInput,
                        onUrlChange = { customUrlInput = it },
                        isInstalling = isInstallingCustom,
                        onInstallClick = {
                            isInstallingCustom = true
                            viewModel.installCustomExtension(
                                downloadUrl = customUrlInput,
                                onSuccess = {
                                    isInstallingCustom = false
                                    customUrlInput = ""
                                    selectedTab = 0
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Extensión instalada exitosamente")
                                    }
                                },
                                onError = { err ->
                                    isInstallingCustom = false
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Error al instalar: $err")
                                    }
                                }
                            )
                        }
                    )
                }
            }
        }
    }

    // Diálogo de confirmación para desinstalar
    extensionToDelete?.let { ext ->
        AlertDialog(
            onDismissRequest = { extensionToDelete = null },
            title = { Text("Desinstalar extensión") },
            text = { Text("¿Deseas desinstalar \"${ext.name}\"? Se eliminarán sus datos asociados.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.uninstallExtension(ext.id)
                        extensionToDelete = null
                        scope.launch {
                            snackbarHostState.showSnackbar("Extensión desinstalada")
                        }
                    }
                ) {
                    Text("Desinstalar")
                }
            },
            dismissButton = {
                TextButton(onClick = { extensionToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

/**
 * Pestaña con la lista de extensiones instaladas y activas.
 */
@Composable
private fun InstalledExtensionsTab(
    installedList: List<WebExtensionModel>,
    isLoading: Boolean,
    onToggle: (WebExtensionModel, Boolean) -> Unit,
    onDeleteClick: (WebExtensionModel) -> Unit,
    onExploreCatalog: () -> Unit
) {
    if (isLoading && installedList.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    if (installedList.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Extension,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No tienes extensiones instaladas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Agrega bloqueadores de anuncios como uBlock Origin, modo oscuro o traductores desde nuestro catálogo oficial.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onExploreCatalog,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Download, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Explorar Extensiones Recomendadas")
            }
        }
        return
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(installedList, key = { it.id }) { ext ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("installed_ext_${ext.id}"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Extension,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = ext.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Versión ${ext.version}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = ext.isEnabled,
                            onCheckedChange = { onToggle(ext, it) },
                            modifier = Modifier.testTag("switch_ext_${ext.id}")
                        )
                    }

                    if (ext.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = ext.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { onDeleteClick(ext) },
                            modifier = Modifier.testTag("delete_ext_${ext.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Desinstalar extensión",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Pestaña con el catálogo oficial de Mozilla Add-ons listo para descargar e instalar.
 */
@Composable
private fun RecommendedCatalogTab(
    catalog: List<RecommendedExtension>,
    installedList: List<WebExtensionModel>,
    downloadProgressMap: Map<String, Float>,
    onInstall: (RecommendedExtension) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Descargas oficiales directas desde Mozilla Add-ons (AMO). Ningún binario con licencias restrictivas está incluido en el APK.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        items(catalog, key = { it.id }) { extension ->
            val isInstalled = installedList.any { it.id == extension.id }
            val progress = downloadProgressMap[extension.id]
            val isDownloading = progress != null

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("catalog_ext_${extension.id}"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getExtensionIcon(extension.iconType),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = extension.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = extension.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (isInstalled) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Instalada",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Button(
                                onClick = { onInstall(extension) },
                                enabled = !isDownloading,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("install_btn_${extension.id}")
                            ) {
                                if (isDownloading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Instalar")
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = extension.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Barra de progreso animada de descarga
                    if (progress != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(2.5.dp)),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Pestaña para instalar una extensión mediante una URL directa (.xpi) vía HTTPS.
 */
@Composable
private fun CustomUrlInstallerTab(
    urlInput: String,
    onUrlChange: (String) -> Unit,
    isInstalling: Boolean,
    onInstallClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        Text(
            text = "Instalar desde Enlace Directo",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Introduce el enlace HTTPS a un archivo .xpi oficial de Mozilla Add-ons u otro repositorio seguro.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = urlInput,
            onValueChange = onUrlChange,
            label = { Text("URL de la extensión (.xpi)") },
            placeholder = { Text("https://addons.mozilla.org/...") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("custom_ext_url_input"),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onInstallClick,
            enabled = urlInput.isNotBlank() && !isInstalling,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("custom_ext_install_button"),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isInstalling) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Descargando e instalando...")
            } else {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Descargar e Instalar")
            }
        }
    }
}

private fun getExtensionIcon(iconType: String): ImageVector {
    return when (iconType) {
        "SHIELD" -> Icons.Default.Shield
        "DARK_MODE" -> Icons.Default.DarkMode
        "TRANSLATE" -> Icons.Default.Translate
        "LINK" -> Icons.Default.Link
        else -> Icons.Default.Extension
    }
}

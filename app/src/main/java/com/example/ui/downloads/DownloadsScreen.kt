package com.example.ui.downloads

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.viewmodel.BrowserViewModel

/**
 * Pantalla del Gestor de Descargas del Navegador.
 * 
 * Presenta:
 * - Sección de descargas activas en curso con velocidad y progreso en tiempo real.
 * - Historial de archivos descargados con categorías temáticas por formato e iconos visuales.
 * - Acciones para abrir, pausar, reanudar o vaciar el registro.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    viewModel: BrowserViewModel,
    onNavigateBack: () -> Unit
) {
    val downloadsHistory by viewModel.downloads.collectAsState()
    val activeDownloadsMap by viewModel.activeDownloads.collectAsState()
    var showClearAllConfirmation by remember { mutableStateOf(false) }

    val activeDownloadsList = remember(activeDownloadsMap) {
        activeDownloadsMap.values.toList()
    }

    val hasAnyDownloads = activeDownloadsList.isNotEmpty() || downloadsHistory.isNotEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Descargas", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("downloads_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (downloadsHistory.isNotEmpty()) {
                        IconButton(
                            onClick = { showClearAllConfirmation = true },
                            modifier = Modifier.testTag("clear_all_downloads_btn")
                        ) {
                            Icon(Icons.Default.ClearAll, contentDescription = "Vaciar historial")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (!hasAnyDownloads) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No tienes descargas recientes",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Los archivos que descargues de la web aparecerán aquí",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Sección de descargas activas en curso
                if (activeDownloadsList.isNotEmpty()) {
                    item {
                        Text(
                            text = "En curso (${activeDownloadsList.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    items(activeDownloadsList, key = { it.id }) { active ->
                        ActiveDownloadCard(
                            download = active,
                            onPause = { viewModel.pauseDownload(active.id) },
                            onResume = { viewModel.resumeDownload(active.id) },
                            onCancel = { viewModel.cancelDownload(active.id) },
                            onRetry = { viewModel.retryDownload(active.id) }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                // Sección del historial de descargas finalizadas
                if (downloadsHistory.isNotEmpty()) {
                    item {
                        Text(
                            text = "Completadas (${downloadsHistory.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    items(downloadsHistory, key = { it.id }) { item ->
                        DownloadItemCard(
                            download = item,
                            onOpen = { download -> viewModel.openDownload(download) },
                            onDelete = { id -> viewModel.deleteDownload(id) }
                        )
                    }
                }
            }
        }

        // Diálogo para vaciar todo el historial
        if (showClearAllConfirmation) {
            AlertDialog(
                onDismissRequest = { showClearAllConfirmation = false },
                icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                title = { Text("Vaciar historial de descargas") },
                text = { Text("¿Deseas eliminar todos los registros del historial de descargas? Los archivos físicos guardados en el dispositivo no se borrarán.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.clearAllDownloads()
                            showClearAllConfirmation = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.testTag("confirm_clear_all_downloads_dialog_btn")
                    ) {
                        Text("Vaciar historial")
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

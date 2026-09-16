package com.example.ui.downloads

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.browser.download.DownloadManagerHelper
import com.example.browser.download.DownloadProgressState
import com.example.data.local.entity.DownloadEntity
import com.example.viewmodel.BrowserViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pantalla del Gestor de Descargas Avanzado.
 * 
 * Ofrece:
 * - Monitoreo visual en tiempo real de descargas activas con barra de progreso,
 *   velocidad instantánea (MB/s o KB/s), bytes transferidos y peso total del archivo.
 * - Controles interactivos directos para Pausar, Reanudar y Cancelar descargas en curso.
 * - Historial completo de descargas finalizadas con apertura segura mediante FileProvider.
 * - Búsqueda en tiempo real y acceso directo a la carpeta de almacenamiento.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    viewModel: BrowserViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val downloads by viewModel.downloads.collectAsState()
    val activeDownloadsMap by viewModel.activeDownloads.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showClearConfirmation by remember { mutableStateOf(false) }

    // Descargas actualmente activas, pausadas o con fallo
    val activeDownloadsList = remember(activeDownloadsMap) {
        activeDownloadsMap.values.filter {
            it.status == DownloadProgressState.STATUS_DOWNLOADING ||
            it.status == DownloadProgressState.STATUS_PAUSED ||
            it.status == DownloadProgressState.STATUS_FAILED
        }.sortedByDescending { it.id }
    }

    // Filtrar descargas completadas del historial
    val filteredHistory = remember(downloads, searchQuery) {
        val completedOnly = downloads.filter { it.status == DownloadEntity.STATUS_COMPLETED }
        if (searchQuery.isBlank()) {
            completedOnly
        } else {
            completedOnly.filter { it.fileName.contains(searchQuery, ignoreCase = true) }
        }
    }

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
                    if (downloads.isNotEmpty()) {
                        IconButton(
                            onClick = { showClearConfirmation = true },
                            modifier = Modifier.testTag("downloads_clear_all_button")
                        ) {
                            Icon(Icons.Default.ClearAll, contentDescription = "Borrar historial")
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
        ) {
            // Barra de búsqueda rápida de descargas
            if (downloads.isNotEmpty() || activeDownloadsList.isNotEmpty()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("downloads_search_input"),
                    placeholder = { Text("Buscar en descargas...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )
            }

            // Barra de estado y acceso a la carpeta de descargas del sistema
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (activeDownloadsList.isNotEmpty()) {
                        "${activeDownloadsList.size} en curso • ${filteredHistory.size} finalizadas"
                    } else {
                        "Total: ${filteredHistory.size} archivos"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FilledTonalButton(
                    onClick = { openSystemDownloadsFolder(context) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("downloads_open_folder_button")
                ) {
                    Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Carpeta del sistema", fontSize = 13.sp)
                }
            }

            if (activeDownloadsList.isEmpty() && filteredHistory.isEmpty()) {
                // Estado vacío amigable
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            modifier = Modifier.size(80.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "No se encontraron descargas" else "Sin descargas recientes",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "Prueba con otro término de búsqueda."
                            else "Los archivos que descargues en el navegador se listarán aquí con velocidad y progreso en tiempo real.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Sección 1: Descargas Activas en Tiempo Real
                    if (activeDownloadsList.isNotEmpty()) {
                        item(key = "header_active_downloads") {
                            Text(
                                text = "Descargas en curso (${activeDownloadsList.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }

                        items(activeDownloadsList, key = { "active_${it.id}" }) { state ->
                            ActiveDownloadCard(
                                state = state,
                                onPause = { viewModel.pauseDownload(state.id) },
                                onResume = { viewModel.resumeDownload(state.id) },
                                onCancel = { viewModel.cancelDownload(state.id) },
                                onRetry = { viewModel.retryDownload(state.id) }
                            )
                        }

                        item(key = "spacer_between_sections") {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    // Sección 2: Historial de Descargas Completadas
                    if (filteredHistory.isNotEmpty()) {
                        item(key = "header_completed_downloads") {
                            Text(
                                text = "Descargas completadas",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }

                        items(filteredHistory, key = { "history_${it.id}" }) { download ->
                            DownloadItemCard(
                                download = download,
                                onOpen = { viewModel.openDownload(download) },
                                onDelete = { viewModel.deleteDownload(download.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Diálogo de confirmación para vaciar el historial de descargas
    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text("Vaciar historial de descargas") },
            text = { Text("¿Deseas eliminar el registro histórico de descargas? Los archivos guardados permanecerán intactos en tu dispositivo.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllDownloads()
                        showClearConfirmation = false
                    },
                    modifier = Modifier.testTag("confirm_clear_downloads_button")
                ) {
                    Text("Vaciar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmation = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

/**
 * Tarjeta interactiva para una descarga activa en tiempo real.
 * Muestra barra de progreso, velocidad en MB/s o KB/s, peso acumulado y botones Pausar/Reanudar/Cancelar.
 */
@Composable
private fun ActiveDownloadCard(
    state: DownloadProgressState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit
) {
    val fileIcon = getIconForFile(state.fileName, state.mimeType)
    val formattedSpeed = DownloadManagerHelper.formatFileSize(state.speedBytesPerSec) + "/s"
    val formattedDownloaded = DownloadManagerHelper.formatFileSize(state.downloadedBytes)
    val formattedTotal = if (state.totalBytes > 0) DownloadManagerHelper.formatFileSize(state.totalBytes) else "Desconocido"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("active_download_card_${state.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = fileIcon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.fileName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        when (state.status) {
                            DownloadProgressState.STATUS_DOWNLOADING -> {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = formattedSpeed,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = " • $formattedDownloaded / $formattedTotal",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            DownloadProgressState.STATUS_PAUSED -> {
                                Text(
                                    text = "Pausada • $formattedDownloaded de $formattedTotal",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            DownloadProgressState.STATUS_FAILED -> {
                                Text(
                                    text = "Error de red: ${state.errorMessage ?: "Desconocido"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Barra de progreso visual
            if (state.totalBytes > 0) {
                LinearProgressIndicator(
                    progress = { (state.downloadedBytes.toFloat() / state.totalBytes.toFloat()).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .testTag("download_progress_bar_${state.id}"),
                    color = if (state.isPaused) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            } else {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .testTag("download_progress_bar_indeterminate_${state.id}"),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Botones de acción (Pausar, Reanudar, Cancelar o Reintentar)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (state.status == DownloadProgressState.STATUS_DOWNLOADING) {
                    FilledTonalButton(
                        onClick = onPause,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("pause_download_btn_${state.id}")
                    ) {
                        Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pausar", fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = onCancel,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("cancel_download_btn_${state.id}")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cancelar", fontSize = 12.sp)
                    }
                } else if (state.status == DownloadProgressState.STATUS_PAUSED) {
                    Button(
                        onClick = onResume,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("resume_download_btn_${state.id}")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reanudar", fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = onCancel,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("cancel_download_btn_${state.id}")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cancelar", fontSize = 12.sp)
                    }
                } else if (state.status == DownloadProgressState.STATUS_FAILED) {
                    Button(
                        onClick = onRetry,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("retry_download_btn_${state.id}")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reintentar", fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = onCancel,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("dismiss_failed_download_btn_${state.id}")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Descartar", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

/**
 * Tarjeta individual para mostrar un archivo descargado del historial completado.
 */
@Composable
private fun DownloadItemCard(
    download: DownloadEntity,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    val fileIcon = getIconForFile(download.fileName, download.mimeType)
    val formattedDate = remember(download.timestamp) {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        sdf.format(Date(download.timestamp))
    }
    val formattedSize = remember(download.fileSizeBytes) {
        DownloadManagerHelper.formatFileSize(download.fileSizeBytes)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() }
            .testTag("download_card_${download.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(46.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = fileIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = download.fileName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formattedSize,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = " • $formattedDate",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(
                onClick = onOpen,
                modifier = Modifier.testTag("download_open_btn_${download.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.OpenInNew,
                    contentDescription = "Abrir archivo",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("download_delete_btn_${download.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Eliminar de la lista",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                )
            }
        }
    }
}

/**
 * Selecciona el icono visual idóneo según la extensión del archivo o su tipo MIME.
 */
private fun getIconForFile(fileName: String, mimeType: String?): ImageVector {
    val lowerName = fileName.lowercase()
    return when {
        lowerName.endsWith(".apk") -> Icons.Default.Android
        lowerName.endsWith(".pdf") || lowerName.endsWith(".doc") || lowerName.endsWith(".txt") -> Icons.Default.Description
        lowerName.endsWith(".zip") || lowerName.endsWith(".rar") || lowerName.endsWith(".tar") || lowerName.endsWith(".7z") -> Icons.Default.Folder
        lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || lowerName.endsWith(".png") || lowerName.endsWith(".webp") -> Icons.Default.Image
        lowerName.endsWith(".mp4") || lowerName.endsWith(".mkv") || lowerName.endsWith(".webm") -> Icons.Default.VideoFile
        lowerName.endsWith(".mp3") || lowerName.endsWith(".ogg") || lowerName.endsWith(".m4a") -> Icons.Default.AudioFile
        mimeType?.startsWith("image/") == true -> Icons.Default.Image
        mimeType?.startsWith("video/") == true -> Icons.Default.VideoFile
        mimeType?.startsWith("audio/") == true -> Icons.Default.AudioFile
        else -> Icons.Default.InsertDriveFile
    }
}

/**
 * Lanza la acción estándar para visualizar la carpeta pública de descargas de Android.
 */
private fun openSystemDownloadsFolder(context: Context) {
    try {
        val intent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        try {
            val fallback = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(fallback)
        } catch (_: Exception) {
            // Ignorar si el dispositivo no cuenta con explorador compatible
        }
    }
}

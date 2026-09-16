package com.example.ui.downloads

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.browser.download.DownloadProgressState

/**
 * Tarjeta interactiva para descargas activas en curso con velocidad y progreso en vivo.
 */
@Composable
fun ActiveDownloadCard(
    download: DownloadProgressState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val style = DownloadFileIconHelper.getStyleForFile(download.fileName, download.mimeType)
    val isIndeterminate = download.totalBytes <= 0L
    val progressFloat = if (isIndeterminate) 0f else (download.progressPercentage / 100f).coerceIn(0f, 1f)

    val formattedDownloaded = DownloadFileIconHelper.formatFileSize(download.downloadedBytes)
    val formattedTotal = if (isIndeterminate) "Tamaño desconocido" else DownloadFileIconHelper.formatFileSize(download.totalBytes)
    val formattedSpeed = DownloadFileIconHelper.formatSpeed(download.speedBytesPerSec)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("active_download_card_${download.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(42.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = style.tint.copy(alpha = 0.15f)
                ) {
                    Icon(
                        imageVector = style.icon,
                        contentDescription = null,
                        tint = style.tint,
                        modifier = Modifier
                            .padding(10.dp)
                            .size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = download.fileName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$formattedDownloaded / $formattedTotal • $formattedSpeed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }

                when (download.status) {
                    DownloadProgressState.STATUS_DOWNLOADING -> {
                        IconButton(
                            onClick = onPause,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("pause_download_btn_${download.id}")
                        ) {
                            Icon(Icons.Default.Pause, contentDescription = "Pausar", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    DownloadProgressState.STATUS_PAUSED -> {
                        IconButton(
                            onClick = onResume,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("resume_download_btn_${download.id}")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Reanudar", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    DownloadProgressState.STATUS_FAILED -> {
                        IconButton(
                            onClick = onRetry,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("retry_download_btn_${download.id}")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reintentar", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    else -> {}
                }

                IconButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("cancel_download_btn_${download.id}")
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Cancelar", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (isIndeterminate) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else {
                LinearProgressIndicator(
                    progress = { progressFloat },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = when (download.status) {
                        DownloadProgressState.STATUS_DOWNLOADING -> "Descargando..."
                        DownloadProgressState.STATUS_PAUSED -> "Pausada"
                        DownloadProgressState.STATUS_FAILED -> "Error en la descarga"
                        else -> "Completada"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (download.status == DownloadProgressState.STATUS_FAILED) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                if (!isIndeterminate) {
                    Text(
                        text = "${download.progressPercentage}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad Room que representa un archivo descargado o en proceso de descarga en el navegador.
 * 
 * Almacena metadatos esenciales como URL de origen, nombre de archivo, tipo MIME,
 * tamaño en bytes, ID del DownloadManager del sistema Android, estado y marca temporal.
 */
@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val url: String,
    val fileName: String,
    val mimeType: String? = null,
    val fileSizeBytes: Long = 0L,
    val downloadedBytes: Long = 0L,
    val downloadManagerId: Long? = null,
    val localUri: String? = null,
    val status: String = STATUS_COMPLETED, // COMPLETED, DOWNLOADING, PAUSED, FAILED, CANCELLED
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        const val STATUS_DOWNLOADING = "DOWNLOADING"
        const val STATUS_PAUSED = "PAUSED"
        const val STATUS_COMPLETED = "COMPLETED"
        const val STATUS_FAILED = "FAILED"
        const val STATUS_CANCELLED = "CANCELLED"
    }
}

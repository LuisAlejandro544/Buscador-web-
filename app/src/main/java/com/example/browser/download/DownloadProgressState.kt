package com.example.browser.download

/**
 * Modelo de estado en tiempo real para una descarga activa o gestionada.
 *
 * @property id Identificador único de la descarga en base de datos.
 * @property url URL de origen del archivo.
 * @property fileName Nombre del archivo en el sistema de almacenamiento.
 * @property mimeType Tipo MIME del contenido.
 * @property totalBytes Tamaño total del archivo en bytes (0 si es indeterminado).
 * @property downloadedBytes Cantidad acumulada de bytes descargados hasta el momento.
 * @property speedBytesPerSec Velocidad de descarga instantánea en bytes por segundo.
 * @property status Estado actual: DOWNLOADING, PAUSED, COMPLETED, FAILED, CANCELLED.
 * @property localFilePath Ruta absoluta local en disco del archivo descargado.
 * @property errorMessage Mensaje de error descriptivo en caso de fallo.
 */
data class DownloadProgressState(
    val id: Long,
    val url: String,
    val fileName: String,
    val mimeType: String? = null,
    val totalBytes: Long = 0L,
    val downloadedBytes: Long = 0L,
    val speedBytesPerSec: Long = 0L,
    val status: String = STATUS_DOWNLOADING,
    val localFilePath: String? = null,
    val errorMessage: String? = null
) {
    /**
     * Calcula el porcentaje completado de 0 a 100 (o -1 si el tamaño es desconocido).
     */
    val progressPercentage: Int
        get() = if (totalBytes > 0) {
            ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100)
        } else {
            -1
        }

    /**
     * Retorna si la descarga está actualmente en transferencia activa de datos.
     */
    val isActivelyDownloading: Boolean
        get() = status == STATUS_DOWNLOADING

    /**
     * Retorna si la descarga se encuentra pausada temporalmente por el usuario.
     */
    val isPaused: Boolean
        get() = status == STATUS_PAUSED

    /**
     * Retorna si la descarga concluyó exitosamente.
     */
    val isCompleted: Boolean
        get() = status == STATUS_COMPLETED

    companion object {
        const val STATUS_DOWNLOADING = "DOWNLOADING"
        const val STATUS_PAUSED = "PAUSED"
        const val STATUS_COMPLETED = "COMPLETED"
        const val STATUS_FAILED = "FAILED"
        const val STATUS_CANCELLED = "CANCELLED"
    }
}

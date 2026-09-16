package com.example.viewmodel.delegates

import android.app.Application
import com.example.browser.download.DownloadEngine
import com.example.browser.download.DownloadManagerHelper
import com.example.browser.download.DownloadProgressState
import com.example.data.local.entity.DownloadEntity
import com.example.data.repository.BrowserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Delegado de gestión de descargas para el navegador.
 * 
 * Responsabilidades:
 * - Supervisar descargas en curso y su progreso en tiempo real (bytes, velocidad y estado).
 * - Controlar acciones de descarga: pausar, reanudar, cancelar y reintentar descargas fallidas.
 * - Gestionar el registro histórico de descargas y su apertura segura con FileProvider.
 * - Limpieza de registros y cancelación en masa.
 */
class DownloadDelegate(
    private val application: Application,
    private val repository: BrowserRepository,
    private val scope: CoroutineScope
) {
    // Flujo del historial de descargas desde Room
    val downloads: StateFlow<List<DownloadEntity>> = repository.getAllDownloads()
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Flujo en tiempo real de descargas activas conectadas al Foreground Service
    val activeDownloads: StateFlow<Map<Long, DownloadProgressState>> = DownloadEngine.activeDownloads

    /**
     * Inicia una nueva descarga delegando en el DownloadManagerHelper nativo.
     */
    fun initiateDownload(
        url: String,
        contentDisposition: String? = null,
        mimeType: String? = null,
        contentLength: Long = 0L
    ) {
        DownloadManagerHelper.startDownload(
            context = application,
            url = url,
            contentDisposition = contentDisposition,
            mimeType = mimeType,
            contentLength = contentLength
        )
    }

    /**
     * Pausa una descarga activa identificada por su ID.
     */
    fun pauseDownload(id: Long) {
        DownloadEngine.pauseDownload(application, id)
    }

    /**
     * Reanuda una descarga pausada.
     */
    fun resumeDownload(id: Long) {
        DownloadEngine.resumeDownload(application, id)
    }

    /**
     * Cancela una descarga activa o en pausa.
     */
    fun cancelDownload(id: Long) {
        DownloadEngine.cancelDownload(application, id)
    }

    /**
     * Reintenta una descarga fallida.
     */
    fun retryDownload(id: Long) {
        DownloadEngine.retryDownload(application, id)
    }

    /**
     * Abre un archivo descargado usando el FileProvider del sistema.
     */
    fun openDownload(download: DownloadEntity) {
        DownloadManagerHelper.openDownloadedFile(application, download)
    }

    /**
     * Elimina el registro de una descarga del historial y cancela si estuviera activa.
     */
    fun deleteDownload(id: Long) {
        DownloadEngine.cancelDownload(application, id)
        scope.launch {
            repository.deleteDownload(id)
        }
    }

    /**
     * Borra todo el historial de descargas finalizadas.
     */
    fun clearAllDownloads() {
        scope.launch {
            repository.clearAllDownloads()
        }
    }
}

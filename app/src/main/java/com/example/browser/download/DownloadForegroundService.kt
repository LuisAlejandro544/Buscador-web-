package com.example.browser.download

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.example.browser.lifecycle.AppHibernationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Servicio en primer plano (Foreground Service) para el mantenimiento resiliente de descargas.
 *
 * Mantiene viva la tarea de transferencia de archivos incluso cuando el usuario sale del navegador
 * o utiliza otras aplicaciones, consumiendo el mínimo estricto de memoria RAM necesario.
 *
 * Funciones clave:
 * - Muestra y refresca la notificación interactiva continua en la barra de notificaciones.
 * - Atiende las acciones de Pausar, Reanudar y Cancelar emitidas desde los botones de la notificación.
 * - Al terminar todas las descargas, detiene el servicio automáticamente y notifica al
 *   [AppHibernationManager] para iniciar la cuenta regresiva de 20 segundos de apagado de RAM.
 */
class DownloadForegroundService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var observerJob: Job? = null

    companion object {
        const val FOREGROUND_NOTIFICATION_ID = 8801
    }

    override fun onCreate() {
        super.onCreate()
        observeActiveDownloads()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val downloadId = intent?.getLongExtra(DownloadNotificationHelper.EXTRA_DOWNLOAD_ID, -1L) ?: -1L

        if (downloadId != -1L) {
            when (action) {
                DownloadNotificationHelper.ACTION_PAUSE -> {
                    DownloadEngine.pauseDownload(this, downloadId)
                }
                DownloadNotificationHelper.ACTION_RESUME -> {
                    DownloadEngine.resumeDownload(this, downloadId)
                }
                DownloadNotificationHelper.ACTION_CANCEL -> {
                    DownloadEngine.cancelDownload(this, downloadId)
                }
            }
        }

        return START_NOT_STICKY
    }

    /**
     * Observa las descargas activas y mantiene actualizada la notificación en la barra de estado.
     */
    private fun observeActiveDownloads() {
        observerJob?.cancel()
        observerJob = serviceScope.launch {
            DownloadEngine.activeDownloads.collectLatest { downloadsMap ->
                val activeOrPausedList = downloadsMap.values.filter {
                    it.status == DownloadProgressState.STATUS_DOWNLOADING ||
                    it.status == DownloadProgressState.STATUS_PAUSED
                }

                if (activeOrPausedList.isNotEmpty()) {
                    // Tomar la primera descarga en progreso para proyectarla en la notificación
                    val primaryDownload = activeOrPausedList.firstOrNull { it.status == DownloadProgressState.STATUS_DOWNLOADING }
                        ?: activeOrPausedList.first()

                    val notification = DownloadNotificationHelper.buildProgressNotification(
                        context = this@DownloadForegroundService,
                        state = primaryDownload
                    )

                    // Iniciar como servicio en primer plano compatible con Android 14+
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        ServiceCompat.startForeground(
                            this@DownloadForegroundService,
                            FOREGROUND_NOTIFICATION_ID,
                            notification,
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                            } else {
                                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                            }
                        )
                    } else {
                        startForeground(FOREGROUND_NOTIFICATION_ID, notification)
                    }
                } else {
                    // No hay descargas activas o en pausa: liberar el servicio y entrar en reposo
                    ServiceCompat.stopForeground(this@DownloadForegroundService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    AppHibernationManager.onDownloadFinished(this@DownloadForegroundService)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

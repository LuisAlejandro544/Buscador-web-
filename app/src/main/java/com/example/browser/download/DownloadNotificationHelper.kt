package com.example.browser.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.example.MainActivity
import java.io.File

/**
 * Gestor de notificaciones nativas para descargas en el Navegador.
 *
 * Proporciona:
 * 1. Notificación emergente (Heads-Up) de inicio: descartable deslizando horizontalmente
 *    y con auto-eliminación programada tras 7 minutos (setTimeoutAfter).
 * 2. Notificación interactiva en la barra de estado con barra de progreso real, velocidad
 *    instantánea y botones de control: Pausar, Reanudar y Cancelar.
 * 3. Notificación al completar la descarga con apertura segura vía FileProvider.
 */
object DownloadNotificationHelper {

    const val CHANNEL_ALERTS_ID = "browser_downloads_alerts"
    const val CHANNEL_PROGRESS_ID = "browser_downloads_progress"
    const val CHANNEL_COMPLETE_ID = "browser_downloads_complete"

    const val ACTION_PAUSE = "com.example.browser.action.PAUSE_DOWNLOAD"
    const val ACTION_RESUME = "com.example.browser.action.RESUME_DOWNLOAD"
    const val ACTION_CANCEL = "com.example.browser.action.CANCEL_DOWNLOAD"
    const val EXTRA_DOWNLOAD_ID = "extra_download_id"

    private const val ALERT_TIMEOUT_MS = 7 * 60 * 1000L // 7 minutos para auto-desaparecer

    /**
     * Inicializa los canales de notificación necesarios en Android 8.0+.
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Canal 1: Avisos de inicio emergentes (Alta prioridad para Heads-Up, deslizable)
            val alertsChannel = NotificationChannel(
                CHANNEL_ALERTS_ID,
                "Avisos de inicio de descargas",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones emergentes de inicio de descargas que desaparecen en 7 minutos o al deslizar"
                enableVibration(true)
            }

            // Canal 2: Progreso en barra de notificaciones (Baja prioridad para actualización silenciosa constante)
            val progressChannel = NotificationChannel(
                CHANNEL_PROGRESS_ID,
                "Progreso y controles de descarga",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Barra continua con velocidad, progreso y controles de pausar/cancelar"
                setShowBadge(false)
            }

            // Canal 3: Descargas completadas
            val completeChannel = NotificationChannel(
                CHANNEL_COMPLETE_ID,
                "Descargas completadas",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificaciones cuando un archivo termina de descargarse"
            }

            notificationManager.createNotificationChannels(listOf(alertsChannel, progressChannel, completeChannel))
        }
    }

    /**
     * Muestra la notificación nativa de inicio.
     * Descartable deslizando a izquierda/derecha y desaparece sola a los 7 minutos.
     */
    fun showDownloadStartedAlert(context: Context, downloadId: Long, fileName: String) {
        createNotificationChannels(context)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Intent para abrir la aplicación al pulsar
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            downloadId.toInt() + 1000,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Descarga iniciada")
            .setContentText("$fileName • Desliza para descartar o toca para abrir")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setOngoing(false) // Permite descartar deslizando horizontalmente
            .setTimeoutAfter(ALERT_TIMEOUT_MS) // Desaparece automáticamente tras 7 minutos
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify((downloadId + 5000).toInt(), notification)
    }

    /**
     * Construye la notificación fija en la barra de estado con acciones interactivas.
     */
    fun buildProgressNotification(
        context: Context,
        state: DownloadProgressState
    ): Notification {
        createNotificationChannels(context)

        // PendingIntent para abrir la app
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            state.id.toInt(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Acción Cancelar
        val cancelIntent = Intent(context, DownloadForegroundService::class.java).apply {
            action = ACTION_CANCEL
            putExtra(EXTRA_DOWNLOAD_ID, state.id)
        }
        val cancelPendingIntent = PendingIntent.getService(
            context,
            (state.id * 10 + 1).toInt(),
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_PROGRESS_ID)
            .setSmallIcon(
                if (state.isPaused) android.R.drawable.ic_media_pause
                else android.R.drawable.stat_sys_download
            )
            .setContentTitle(state.fileName)
            .setContentIntent(contentPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)

        val formattedSpeed = DownloadManagerHelper.formatFileSize(state.speedBytesPerSec) + "/s"
        val formattedCurrent = DownloadManagerHelper.formatFileSize(state.downloadedBytes)
        val formattedTotal = if (state.totalBytes > 0) DownloadManagerHelper.formatFileSize(state.totalBytes) else "Desconocido"

        if (state.isPaused) {
            builder.setContentText("Pausada • $formattedCurrent de $formattedTotal")
            builder.setProgress(100, state.progressPercentage.coerceAtLeast(0), false)

            // Botón Reanudar
            val resumeIntent = Intent(context, DownloadForegroundService::class.java).apply {
                action = ACTION_RESUME
                putExtra(EXTRA_DOWNLOAD_ID, state.id)
            }
            val resumePendingIntent = PendingIntent.getService(
                context,
                (state.id * 10 + 2).toInt(),
                resumeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_media_play, "Reanudar", resumePendingIntent)
            builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancelar", cancelPendingIntent)
        } else {
            // En descarga activa
            val progressText = if (state.totalBytes > 0) {
                "$formattedSpeed • $formattedCurrent / $formattedTotal (${state.progressPercentage}%)"
            } else {
                "$formattedSpeed • $formattedCurrent descargados"
            }
            builder.setContentText(progressText)

            if (state.totalBytes > 0) {
                builder.setProgress(100, state.progressPercentage, false)
            } else {
                builder.setProgress(100, 0, true) // Indeterminado
            }

            // Botón Pausar
            val pauseIntent = Intent(context, DownloadForegroundService::class.java).apply {
                action = ACTION_PAUSE
                putExtra(EXTRA_DOWNLOAD_ID, state.id)
            }
            val pausePendingIntent = PendingIntent.getService(
                context,
                (state.id * 10 + 3).toInt(),
                pauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_media_pause, "Pausar", pausePendingIntent)
            builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancelar", cancelPendingIntent)
        }

        return builder.build()
    }

    /**
     * Muestra la notificación de descarga finalizada con éxito.
     */
    fun showDownloadCompletedNotification(
        context: Context,
        downloadId: Long,
        fileName: String,
        filePath: String?,
        mimeType: String?
    ) {
        createNotificationChannels(context)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Descartar notificación de aviso anterior si aún estuviera visible
        notificationManager.cancel((downloadId + 5000).toInt())

        val builder = NotificationCompat.Builder(context, CHANNEL_COMPLETE_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Descarga completada")
            .setContentText(fileName)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        if (!filePath.isNullOrBlank()) {
            val file = File(filePath)
            if (file.exists()) {
                try {
                    val contentUri: Uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(contentUri, mimeType ?: "*/*")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                    }
                    val pendingIntent = PendingIntent.getActivity(
                        context,
                        (downloadId + 2000).toInt(),
                        viewIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    builder.setContentIntent(pendingIntent)
                } catch (_: Exception) {}
            }
        }

        notificationManager.notify((downloadId + 9000).toInt(), builder.build())
    }

    /**
     * Cancela la notificación de una descarga específica.
     */
    fun cancelNotification(context: Context, notificationId: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationId)
    }
}

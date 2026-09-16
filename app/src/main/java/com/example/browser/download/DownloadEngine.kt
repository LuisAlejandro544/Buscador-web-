package com.example.browser.download

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.browser.lifecycle.AppHibernationManager
import com.example.browser.sound.SoundEffectManager
import com.example.data.local.BrowserDatabase
import com.example.data.local.entity.DownloadEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Motor central de descargas resilientes para el Navegador Web.
 *
 * Características:
 * - Descargas concurrentes reales gestionadas por OkHttp y Coroutines de Kotlin.
 * - Soporte nativo para pausar y reanudar mediante cabeceras HTTP Range (bytes=N-).
 * - Medición precisa de velocidad de transferencia en tiempo real (bytes/segundo).
 * - Sincronización automática con la base de datos Room y notificaciones del sistema.
 * - Coordinación con AppHibernationManager para ahorro extremo de batería y RAM.
 */
object DownloadEngine {

    private const val TAG = "DownloadEngine"
    private const val BUFFER_SIZE = 32 * 1024 // Buffer de 32 KB para alto rendimiento en descargas

    private val scope = CoroutineScope(Dispatchers.IO)

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    // Estado reactivo de descargas activas en memoria para la UI y el servicio
    private val _activeDownloads = MutableStateFlow<Map<Long, DownloadProgressState>>(emptyMap())
    val activeDownloads: StateFlow<Map<Long, DownloadProgressState>> = _activeDownloads.asStateFlow()

    // Tareas coroutine y flags de control para pausa y cancelación
    private val downloadJobs = ConcurrentHashMap<Long, Job>()
    private val pauseFlags = ConcurrentHashMap<Long, Boolean>()
    private val cancelFlags = ConcurrentHashMap<Long, Boolean>()

    /**
     * Comprueba si existe alguna descarga en estado activo de transferencia.
     */
    fun hasActiveDownloads(): Boolean {
        return _activeDownloads.value.values.any { it.status == DownloadProgressState.STATUS_DOWNLOADING }
    }

    /**
     * Inicia o pone en cola una nueva descarga.
     */
    fun enqueueDownload(
        context: Context,
        url: String,
        contentDisposition: String? = null,
        mimeType: String? = null,
        contentLength: Long = 0L
    ): Long {
        val appContext = context.applicationContext
        val fileName = DownloadManagerHelper.resolveFileName(url, contentDisposition, mimeType)

        // Crear o preparar el archivo de destino local
        val downloadsDir = appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: appContext.filesDir
        val destinationFile = getUniqueFile(downloadsDir, fileName)

        // Crear registro en base de datos Room
        val database = BrowserDatabase.getInstance(appContext)
        val downloadEntity = DownloadEntity(
            url = url,
            fileName = destinationFile.name,
            mimeType = mimeType,
            fileSizeBytes = contentLength,
            downloadedBytes = 0L,
            localUri = destinationFile.absolutePath,
            status = DownloadProgressState.STATUS_DOWNLOADING,
            timestamp = System.currentTimeMillis()
        )

        var insertedId = 0L
        val syncJob = scope.launch {
            insertedId = database.downloadDao().insertDownload(downloadEntity)

            val initialState = DownloadProgressState(
                id = insertedId,
                url = url,
                fileName = destinationFile.name,
                mimeType = mimeType,
                totalBytes = contentLength,
                downloadedBytes = 0L,
                speedBytesPerSec = 0L,
                status = DownloadProgressState.STATUS_DOWNLOADING,
                localFilePath = destinationFile.absolutePath
            )

            updateState(initialState)

            // Mostrar notificación nativa flotante de inicio (descartable y timeout de 7 minutos)
            DownloadNotificationHelper.showDownloadStartedAlert(appContext, insertedId, destinationFile.name)

            // Iniciar o sincronizar el servicio en primer plano
            startForegroundService(appContext)

            // Comenzar descarga en streaming
            launchDownloadTask(appContext, insertedId, url, destinationFile, contentLength, mimeType)
        }

        return insertedId
    }

    /**
     * Pausa una descarga activa conservando los bytes ya descargados.
     */
    fun pauseDownload(context: Context, downloadId: Long) {
        val currentState = _activeDownloads.value[downloadId] ?: return
        if (currentState.status != DownloadProgressState.STATUS_DOWNLOADING) return

        pauseFlags[downloadId] = true
        downloadJobs[downloadId]?.cancel()
        downloadJobs.remove(downloadId)

        val pausedState = currentState.copy(
            status = DownloadProgressState.STATUS_PAUSED,
            speedBytesPerSec = 0L
        )
        updateState(pausedState)

        scope.launch {
            val database = BrowserDatabase.getInstance(context)
            database.downloadDao().getDownloadById(downloadId)?.let { entity ->
                database.downloadDao().updateDownload(
                    entity.copy(
                        status = DownloadEntity.STATUS_PAUSED,
                        downloadedBytes = pausedState.downloadedBytes
                    )
                )
            }
        }
    }

    /**
     * Reanuda una descarga pausada solicitando solo los bytes restantes mediante HTTP Range.
     */
    fun resumeDownload(context: Context, downloadId: Long) {
        val currentState = _activeDownloads.value[downloadId] ?: return
        if (currentState.status != DownloadProgressState.STATUS_PAUSED) return

        pauseFlags.remove(downloadId)
        cancelFlags.remove(downloadId)

        val targetFile = currentState.localFilePath?.let { File(it) } ?: return
        val resumedState = currentState.copy(
            status = DownloadProgressState.STATUS_DOWNLOADING,
            speedBytesPerSec = 0L
        )
        updateState(resumedState)

        startForegroundService(context)

        launchDownloadTask(
            context = context.applicationContext,
            downloadId = downloadId,
            url = currentState.url,
            destinationFile = targetFile,
            initialTotalBytes = currentState.totalBytes,
            mimeType = currentState.mimeType
        )
    }

    /**
     * Cancela definitivamente una descarga y limpia los recursos asociados.
     */
    fun cancelDownload(context: Context, downloadId: Long) {
        val currentState = _activeDownloads.value[downloadId] ?: return

        cancelFlags[downloadId] = true
        pauseFlags.remove(downloadId)
        downloadJobs[downloadId]?.cancel()
        downloadJobs.remove(downloadId)

        val cancelledState = currentState.copy(
            status = DownloadProgressState.STATUS_CANCELLED,
            speedBytesPerSec = 0L
        )
        updateState(cancelledState)

        scope.launch {
            val database = BrowserDatabase.getInstance(context)
            database.downloadDao().getDownloadById(downloadId)?.let { entity ->
                database.downloadDao().updateDownload(
                    entity.copy(status = DownloadEntity.STATUS_CANCELLED)
                )
            }
            // Eliminar archivo parcial en caso de cancelación
            currentState.localFilePath?.let { path ->
                try { File(path).delete() } catch (_: Exception) {}
            }
            AppHibernationManager.onDownloadFinished(context)
        }
    }

    /**
     * Reintenta una descarga fallida o cancelada desde el principio.
     */
    fun retryDownload(context: Context, downloadId: Long) {
        val currentState = _activeDownloads.value[downloadId] ?: return
        val targetFile = currentState.localFilePath?.let { File(it) } ?: return

        try { targetFile.delete() } catch (_: Exception) {}

        pauseFlags.remove(downloadId)
        cancelFlags.remove(downloadId)

        val restartingState = currentState.copy(
            downloadedBytes = 0L,
            speedBytesPerSec = 0L,
            status = DownloadProgressState.STATUS_DOWNLOADING,
            errorMessage = null
        )
        updateState(restartingState)

        startForegroundService(context)
        launchDownloadTask(
            context = context.applicationContext,
            downloadId = downloadId,
            url = currentState.url,
            destinationFile = targetFile,
            initialTotalBytes = currentState.totalBytes,
            mimeType = currentState.mimeType
        )
    }

    /**
     * Tarea principal de streaming en segundo plano con cálculo de velocidad.
     */
    private fun launchDownloadTask(
        context: Context,
        downloadId: Long,
        url: String,
        destinationFile: File,
        initialTotalBytes: Long,
        mimeType: String?
    ) {
        val job = scope.launch {
            var downloadedBytes = if (destinationFile.exists()) destinationFile.length() else 0L
            var totalBytes = initialTotalBytes

            try {
                val requestBuilder = Request.Builder().url(url)
                // Solicitar Range solo si ya tenemos contenido parcial
                if (downloadedBytes > 0L) {
                    requestBuilder.header("Range", "bytes=$downloadedBytes-")
                }

                val response = httpClient.newCall(requestBuilder.build()).execute()
                if (!response.isSuccessful && response.code != 206) {
                    throw Exception("Respuesta del servidor no exitosa: HTTP ${response.code}")
                }

                val body = response.body ?: throw Exception("El cuerpo de la respuesta es nulo")
                val responseContentLength = body.contentLength()

                // Si el servidor soporta Range, el total es lo descargado + lo restante
                if (response.code == 206 && responseContentLength > 0) {
                    totalBytes = downloadedBytes + responseContentLength
                } else if (response.code == 200 && responseContentLength > 0) {
                    // El servidor no soportó Range y devolvió el archivo completo
                    totalBytes = responseContentLength
                    downloadedBytes = 0L
                }

                val isAppend = response.code == 206 && downloadedBytes > 0L
                val outputStream = FileOutputStream(destinationFile, isAppend)
                val inputStream: InputStream = body.byteStream()

                val buffer = ByteArray(BUFFER_SIZE)
                var bytesRead: Int
                var lastSpeedCalcTime = System.currentTimeMillis()
                var bytesSinceLastCalc = 0L
                var currentSpeed = 0L

                outputStream.use { out ->
                    inputStream.use { input ->
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            // Verificar interrupción por pausa o cancelación
                            if (pauseFlags[downloadId] == true) {
                                return@launch
                            }
                            if (cancelFlags[downloadId] == true) {
                                return@launch
                            }

                            out.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead
                            bytesSinceLastCalc += bytesRead

                            val now = System.currentTimeMillis()
                            val timeDelta = now - lastSpeedCalcTime

                            // Actualizar cálculo de velocidad y estado cada 500 ms
                            if (timeDelta >= 500) {
                                currentSpeed = ((bytesSinceLastCalc * 1000.0) / timeDelta).toLong()
                                lastSpeedCalcTime = now
                                bytesSinceLastCalc = 0L

                                val updatedState = _activeDownloads.value[downloadId]?.copy(
                                    downloadedBytes = downloadedBytes,
                                    totalBytes = totalBytes,
                                    speedBytesPerSec = currentSpeed,
                                    status = DownloadProgressState.STATUS_DOWNLOADING
                                )
                                if (updatedState != null) {
                                    updateState(updatedState)
                                }
                            }
                        }
                    }
                }

                // Descarga terminada satisfactoriamente
                val completedState = _activeDownloads.value[downloadId]?.copy(
                    downloadedBytes = downloadedBytes,
                    totalBytes = if (totalBytes > 0) totalBytes else downloadedBytes,
                    speedBytesPerSec = 0L,
                    status = DownloadProgressState.STATUS_COMPLETED
                )
                if (completedState != null) {
                    updateState(completedState)
                }

                // Actualizar base de datos
                val database = BrowserDatabase.getInstance(context)
                database.downloadDao().getDownloadById(downloadId)?.let { entity ->
                    database.downloadDao().updateDownload(
                        entity.copy(
                            status = DownloadEntity.STATUS_COMPLETED,
                            fileSizeBytes = downloadedBytes,
                            downloadedBytes = downloadedBytes,
                            localUri = destinationFile.absolutePath
                        )
                    )
                }

                // Notificar al usuario que la descarga finalizó
                DownloadNotificationHelper.showDownloadCompletedNotification(
                    context = context,
                    downloadId = downloadId,
                    fileName = destinationFile.name,
                    filePath = destinationFile.absolutePath,
                    mimeType = mimeType
                )

                // Reproducir aleatoriamente uno de los sonidos de logro de descarga completada
                SoundEffectManager.playRandomDownloadSuccess(context)

                Log.d(TAG, "Descarga completada con éxito: ${destinationFile.name}")
            } catch (e: Exception) {
                if (pauseFlags[downloadId] != true && cancelFlags[downloadId] != true) {
                    Log.e(TAG, "Fallo en la descarga $downloadId", e)
                    val errorState = _activeDownloads.value[downloadId]?.copy(
                        status = DownloadProgressState.STATUS_FAILED,
                        speedBytesPerSec = 0L,
                        errorMessage = e.localizedMessage ?: "Error de red"
                    )
                    if (errorState != null) {
                        updateState(errorState)
                    }

                    val database = BrowserDatabase.getInstance(context)
                    database.downloadDao().getDownloadById(downloadId)?.let { entity ->
                        database.downloadDao().updateDownload(
                            entity.copy(status = DownloadEntity.STATUS_FAILED)
                        )
                    }
                }
            } finally {
                downloadJobs.remove(downloadId)
                pauseFlags.remove(downloadId)
                cancelFlags.remove(downloadId)

                // Informar al gestor de hibernación para que inicie la cuenta de 20s si no quedan descargas
                AppHibernationManager.onDownloadFinished(context)
            }
        }

        downloadJobs[downloadId] = job
    }

    /**
     * Actualiza el StateFlow en memoria con un nuevo estado de descarga.
     */
    private fun updateState(state: DownloadProgressState) {
        val current = _activeDownloads.value.toMutableMap()
        current[state.id] = state
        _activeDownloads.value = current
    }

    /**
     * Genera un archivo con nombre único para evitar sobreescrituras accidentales.
     */
    private fun getUniqueFile(directory: File, baseFileName: String): File {
        var file = File(directory, baseFileName)
        if (!file.exists()) return file

        val nameWithoutExt = baseFileName.substringBeforeLast('.')
        val ext = if (baseFileName.contains('.')) "." + baseFileName.substringAfterLast('.') else ""

        var counter = 1
        while (file.exists()) {
            file = File(directory, "$nameWithoutExt ($counter)$ext")
            counter++
        }
        return file
    }

    /**
     * Inicia el Foreground Service de descargas.
     */
    private fun startForegroundService(context: Context) {
        try {
            val intent = Intent(context, DownloadForegroundService::class.java)
            ContextCompat.startForegroundService(context, intent)
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo arrancar el servicio de descargas", e)
        }
    }
}

package com.example.browser.download

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import android.widget.Toast
import com.example.data.local.entity.DownloadEntity
import java.io.File
import java.text.DecimalFormat
import kotlin.math.log10
import kotlin.math.pow

/**
 * Utilidad y gestor de descargas para GeckoView en Android.
 * 
 * Gestiona la integración con el servicio DownloadManager del sistema Android:
 * - Extrae el nombre de archivo a partir de la URL y las cabeceras HTTP (Content-Disposition).
 * - Programa descargas en segundo plano en el directorio público de Descargas.
 * - Muestra notificaciones del sistema durante y tras la finalización.
 * - Proporciona resolución de archivos y lanzamiento de Intents para abrir documentos y APKs.
 */
object DownloadManagerHelper {

    /**
     * Inicia una nueva descarga usando el servicio seguro del sistema Android.
     * 
     * @param context Contexto de la aplicación.
     * @param url URL pública del recurso a descargar.
     * @param contentDisposition Cabecera opcional Content-Disposition de la respuesta HTTP.
     * @param mimeType Tipo MIME sugerido del archivo.
     * @param contentLength Tamaño en bytes reportado por el servidor web (0 si es desconocido).
     * @return Entidad [DownloadEntity] preparada para guardarse en la base de datos local Room.
     */
    fun startDownload(
        context: Context,
        url: String,
        contentDisposition: String? = null,
        mimeType: String? = null,
        contentLength: Long = 0L
    ): DownloadEntity {
        val appContext = context.applicationContext
        val fileName = resolveFileName(url, contentDisposition, mimeType)
        val resolvedMimeType = resolveMimeType(fileName, mimeType)

        var downloadManagerId: Long? = null
        try {
            val downloadManager = appContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setTitle(fileName)
                setDescription("Descargando desde el navegador...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
                if (!resolvedMimeType.isNullOrBlank()) {
                    setMimeType(resolvedMimeType)
                }
            }
            downloadManagerId = downloadManager.enqueue(request)
            Toast.makeText(appContext, "Descargando: $fileName", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(appContext, "Error al iniciar descarga: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }

        return DownloadEntity(
            url = url,
            fileName = fileName,
            mimeType = resolvedMimeType,
            fileSizeBytes = contentLength,
            downloadManagerId = downloadManagerId,
            status = if (downloadManagerId != null) DownloadEntity.STATUS_DOWNLOADING else DownloadEntity.STATUS_FAILED,
            timestamp = System.currentTimeMillis()
        )
    }

    /**
     * Determina el nombre de archivo más exacto posible para la descarga.
     */
    fun resolveFileName(url: String, contentDisposition: String?, mimeType: String?): String {
        return try {
            val guessed = URLUtil.guessFileName(url, contentDisposition, mimeType)
            if (guessed.isNotBlank() && guessed != "downloadfile") {
                guessed
            } else {
                val lastSegment = Uri.parse(url).lastPathSegment
                if (!lastSegment.isNullOrBlank() && lastSegment.contains(".")) {
                    lastSegment
                } else {
                    "descarga_${System.currentTimeMillis()}" + (mimeTypeToExtension(mimeType) ?: ".bin")
                }
            }
        } catch (_: Exception) {
            "archivo_${System.currentTimeMillis()}"
        }
    }

    /**
     * Resuelve el tipo MIME a partir de la extensión o el valor proporcionado por el servidor.
     */
    private fun resolveMimeType(fileName: String, providedMime: String?): String? {
        if (!providedMime.isNullOrBlank() && providedMime != "application/octet-stream") {
            return providedMime
        }
        val extension = fileName.substringAfterLast('.', "")
        return if (extension.isNotBlank()) {
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase()) ?: providedMime
        } else {
            providedMime
        }
    }

    /**
     * Mapea un tipo MIME a su extensión común de archivo.
     */
    private fun mimeTypeToExtension(mimeType: String?): String? {
        if (mimeType == null) return null
        val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
        return if (!ext.isNullOrBlank()) ".$ext" else null
    }

    /**
     * Formatea un tamaño numérico en bytes a un formato legible por humanos (B, KB, MB, GB).
     */
    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "Tamaño desconocido"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt().coerceIn(0, units.size - 1)
        return DecimalFormat("#,##0.#").format(bytes / 1024.0.pow(digitGroups.toDouble())) + " " + units[digitGroups]
    }

    /**
     * Intenta abrir un archivo descargado con la aplicación adecuada en el sistema.
     */
    fun openDownloadedFile(context: Context, download: DownloadEntity) {
        try {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            var fileUri: Uri? = null

            // 1. Intentar resolver mediante el ID de DownloadManager
            download.downloadManagerId?.let { id ->
                fileUri = try {
                    downloadManager.getUriForDownloadedFile(id)
                } catch (_: Exception) {
                    null
                }
            }

            // 2. Intentar buscar el archivo directo en la carpeta pública de descargas
            if (fileUri == null) {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadsDir, download.fileName)
                if (file.exists()) {
                    fileUri = Uri.fromFile(file)
                }
            }

            if (fileUri != null) {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(fileUri, download.mimeType ?: "*/*")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                context.startActivity(intent)
            } else {
                Toast.makeText(context, "Archivo no disponible o movido", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "No se encontró aplicación para abrir este archivo", Toast.LENGTH_SHORT).show()
        }
    }
}

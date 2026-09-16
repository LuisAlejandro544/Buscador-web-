package com.example.browser.download

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.local.entity.DownloadEntity
import java.io.File
import java.text.DecimalFormat
import kotlin.math.log10
import kotlin.math.pow

/**
 * Utilidad y gestor de descargas para el Navegador Web.
 * 
 * - Extrae el nombre de archivo a partir de la URL y las cabeceras HTTP (Content-Disposition).
 * - Encola y coordina descargas de streaming mediante [DownloadEngine].
 * - Proporciona resolución segura de URIs mediante [FileProvider] para abrir documentos, imágenes y APKs.
 * - Formatea velocidades y tamaños de archivo en unidades legibles.
 */
object DownloadManagerHelper {

    /**
     * Inicia una nueva descarga resiliente a través de DownloadEngine.
     * 
     * @param context Contexto de la aplicación.
     * @param url URL pública del recurso a descargar.
     * @param contentDisposition Cabecera opcional Content-Disposition de la respuesta HTTP.
     * @param mimeType Tipo MIME sugerido del archivo.
     * @param contentLength Tamaño en bytes reportado por el servidor web (0 si es desconocido).
     * @return Identificador generado de la descarga.
     */
    fun startDownload(
        context: Context,
        url: String,
        contentDisposition: String? = null,
        mimeType: String? = null,
        contentLength: Long = 0L
    ): Long {
        return DownloadEngine.enqueueDownload(
            context = context,
            url = url,
            contentDisposition = contentDisposition,
            mimeType = mimeType,
            contentLength = contentLength
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
    fun resolveMimeType(fileName: String, providedMime: String?): String? {
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
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt().coerceIn(0, units.size - 1)
        return DecimalFormat("#,##0.#").format(bytes / 1024.0.pow(digitGroups.toDouble())) + " " + units[digitGroups]
    }

    /**
     * Intenta abrir un archivo descargado con la aplicación adecuada en el sistema usando FileProvider.
     */
    fun openDownloadedFile(context: Context, download: DownloadEntity) {
        try {
            var file: File? = null

            // 1. Intentar resolver mediante localUri guardado
            download.localUri?.let { path ->
                val f = File(path)
                if (f.exists()) file = f
            }

            // 2. Intentar buscar en la carpeta de descargas de la app
            if (file == null) {
                val appDownloads = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                if (appDownloads != null) {
                    val f = File(appDownloads, download.fileName)
                    if (f.exists()) file = f
                }
            }

            // 3. Intentar buscar en descargas públicas
            if (file == null) {
                val pubDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val f = File(pubDownloads, download.fileName)
                if (f.exists()) file = f
            }

            if (file != null && file!!.exists()) {
                val contentUri: Uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file!!
                )
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(contentUri, download.mimeType ?: resolveMimeType(download.fileName, null) ?: "*/*")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                context.startActivity(intent)
            } else {
                Toast.makeText(context, "Archivo no encontrado o movido del almacenamiento", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "No se encontró aplicación para abrir este archivo", Toast.LENGTH_SHORT).show()
        }
    }
}

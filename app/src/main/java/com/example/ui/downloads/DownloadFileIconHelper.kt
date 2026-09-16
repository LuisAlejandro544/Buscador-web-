package com.example.ui.downloads

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Utilidades para determinar el icono y color temático de un archivo descargado
 * según su extensión o tipo MIME.
 */
object DownloadFileIconHelper {

    data class FileIconStyle(
        val icon: ImageVector,
        val tint: Color
    )

    fun getStyleForFile(fileName: String, mimeType: String? = null): FileIconStyle {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        val mime = mimeType?.lowercase() ?: ""

        return when {
            extension == "apk" || mime.contains("vnd.android.package-archive") -> {
                FileIconStyle(Icons.Default.Android, Color(0xFF4CAF50))
            }
            extension == "pdf" || mime.contains("pdf") -> {
                FileIconStyle(Icons.Default.PictureAsPdf, Color(0xFFE53935))
            }
            extension in listOf("jpg", "jpeg", "png", "webp", "gif", "svg") || mime.startsWith("image/") -> {
                FileIconStyle(Icons.Default.Image, Color(0xFF1E88E5))
            }
            extension in listOf("mp4", "mkv", "webm", "avi", "mov") || mime.startsWith("video/") -> {
                FileIconStyle(Icons.Default.Movie, Color(0xFF8E24AA))
            }
            extension in listOf("mp3", "m4a", "wav", "flac", "ogg", "aac") || mime.startsWith("audio/") -> {
                FileIconStyle(Icons.Default.AudioFile, Color(0xFFFB8C00))
            }
            extension in listOf("zip", "rar", "7z", "tar", "gz") || mime.contains("zip") || mime.contains("compressed") -> {
                FileIconStyle(Icons.Default.FolderZip, Color(0xFF5E35B1))
            }
            extension in listOf("txt", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "html", "json") -> {
                FileIconStyle(Icons.Default.Description, Color(0xFF00ACC1))
            }
            else -> {
                FileIconStyle(Icons.Default.InsertDriveFile, Color(0xFF757575))
            }
        }
    }

    /**
     * Formatea una cantidad de bytes a una cadena legible (ej: 14.5 MB).
     */
    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0

        return when {
            gb >= 1.0 -> String.format("%.2f GB", gb)
            mb >= 1.0 -> String.format("%.1f MB", mb)
            kb >= 1.0 -> String.format("%.1f KB", kb)
            else -> "$bytes B"
        }
    }

    /**
     * Formatea velocidad en bytes por segundo a cadena legible (ej: 1.2 MB/s).
     */
    fun formatSpeed(bytesPerSec: Long): String {
        if (bytesPerSec <= 0L) return "0 KB/s"
        return "${formatFileSize(bytesPerSec)}/s"
    }
}

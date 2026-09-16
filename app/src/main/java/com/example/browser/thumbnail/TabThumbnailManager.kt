package com.example.browser.thumbnail

import android.graphics.Bitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * Administrador central de miniaturas y vistas previas de pestañas del navegador.
 * 
 * Permite almacenar y consultar capturas visuales de las páginas web visitadas para que el usuario
 * pueda ver exactamente en qué punto o sección dejó cada página en la pantalla de gestión de pestañas.
 * 
 * Para optimizar el rendimiento y evitar sobrecarga de memoria RAM en dispositivos móviles,
 * las imágenes se reescalan a dimensiones ligeras (360x540 máximo) manteniendo la proporción original.
 */
object TabThumbnailManager {

    // Mapa en memoria concurrente con las miniaturas activas indexadas por ID de pestaña
    private val memoryCache = ConcurrentHashMap<Long, Bitmap>()

    // Flujo de estado observable para recomponer automáticamente la interfaz de pestañas
    private val _thumbnailsFlow = MutableStateFlow<Map<Long, Bitmap>>(emptyMap())
    val thumbnailsFlow: StateFlow<Map<Long, Bitmap>> = _thumbnailsFlow.asStateFlow()

    /**
     * Guarda y optimiza una miniatura capturada para una pestaña específica.
     * 
     * @param tabId Identificador de la pestaña en la base de datos Room.
     * @param originalBitmap Captura original en alta resolución obtenida del motor GeckoView.
     */
    fun saveThumbnail(tabId: Long, originalBitmap: Bitmap) {
        try {
            // Reescalar a resolución compacta para bajo consumo de memoria RAM
            val targetWidth = 360
            val aspectRatio = originalBitmap.height.toFloat() / originalBitmap.width.toFloat()
            val targetHeight = (targetWidth * aspectRatio).toInt().coerceAtMost(540)

            val scaled = Bitmap.createScaledBitmap(originalBitmap, targetWidth, targetHeight, true)
            memoryCache[tabId] = scaled

            // Emitir copia inmutable para actualizar observadores en Jetpack Compose
            _thumbnailsFlow.value = memoryCache.toMap()
        } catch (e: Throwable) {
            // Manejo preventivo si la memoria es limitada
            memoryCache[tabId] = originalBitmap
            _thumbnailsFlow.value = memoryCache.toMap()
        }
    }

    /**
     * Obtiene la miniatura almacenada para una pestaña.
     */
    fun getThumbnail(tabId: Long): Bitmap? = memoryCache[tabId]

    /**
     * Elimina la miniatura asociada al cerrar una pestaña, liberando memoria.
     */
    fun removeThumbnail(tabId: Long) {
        memoryCache.remove(tabId)?.recycle()
        _thumbnailsFlow.value = memoryCache.toMap()
    }

    /**
     * Limpia todas las miniaturas en memoria (por ejemplo al cerrar todas las pestañas).
     */
    fun clearAll() {
        memoryCache.values.forEach { 
            try {
                if (!it.isRecycled) it.recycle() 
            } catch (_: Throwable) {}
        }
        memoryCache.clear()
        _thumbnailsFlow.value = emptyMap()
    }
}

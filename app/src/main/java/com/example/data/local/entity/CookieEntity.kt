package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad de Room para el almacenamiento, auditoría e inspección de Cookies de Navegación.
 * 
 * Permite registrar las cookies depositadas por los sitios web, identificar de forma
 * automática si corresponden a rastreadores publicitarios o analítica (Google Analytics,
 * Meta Pixel, DoubleClick, Criteo, etc.), y diferenciar si provienen de navegación estándar
 * o de una Pestaña Protegida (contenedor aislado multi-account).
 *
 * Las cookies de pestañas protegidas cuentan con un ciclo de vida efímero: se eliminan
 * automáticamente de la base de datos y de GeckoView tan pronto como se cierra la pestaña.
 */
@Entity(tableName = "cookies")
data class CookieEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val domain: String,
    val value: String,
    val path: String = "/",
    val isTracker: Boolean = false,
    val category: String = "Funcional", // Categorías: "Rastreo y Publicidad", "Analítica", "Funcional", "Seguridad", "Sesión"
    val isSecure: Boolean = true,
    val isHttpOnly: Boolean = false,
    val isProtected: Boolean = false, // True si proviene de una pestaña protegida aislada
    val contextId: String? = null, // Identificador de contenedor multi-cuenta en GeckoView
    val tabId: Long? = null, // ID de la pestaña protegida en Room
    val expiresAt: Long = 0,
    val createdAt: Long = System.currentTimeMillis()
)

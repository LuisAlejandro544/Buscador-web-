package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad de Room para el almacenamiento, auditoría e inspección de Cookies de Navegación.
 * 
 * Permite registrar las cookies depositadas por los sitios web, identificar de forma
 * automática si corresponden a rastreadores publicitarios o analítica (Google Analytics,
 * Meta Pixel, DoubleClick, Criteo, etc.), y facilitar su borrado granular por dominio o elemento.
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
    val category: String = "Funcional", // Categorías: "Rastreo y Publicidad", "Analítica", "Funcional", "Seguridad"
    val isSecure: Boolean = true,
    val isHttpOnly: Boolean = false,
    val expiresAt: Long = 0,
    val createdAt: Long = System.currentTimeMillis()
)

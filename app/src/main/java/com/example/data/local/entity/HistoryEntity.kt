package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad que almacena el registro de navegación (historial) del usuario.
 * 
 * @property id Identificador único del registro de historial
 * @property title Título de la página web visitada
 * @property url Dirección web visitada
 * @property faviconUrl URL del icono o favicon representativo de la página web
 * @property visitedAt Marca de tiempo de la visita (milisegundos)
 */
@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val url: String,
    val faviconUrl: String? = null,
    val visitedAt: Long = System.currentTimeMillis()
)

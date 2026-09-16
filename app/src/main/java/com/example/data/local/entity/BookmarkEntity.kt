package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad que representa un marcador (favorito) guardado por el usuario.
 * 
 * @property id Identificador único del marcador
 * @property title Nombre o título descriptivo del sitio web
 * @property url Dirección web completa del marcador
 * @property faviconUrl URL del icono del sitio si está disponible
 * @property createdAt Marca de tiempo de cuando fue guardado
 */
@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val url: String,
    val faviconUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad que representa un acceso directo personalizable en la pantalla de inicio (Speed Dial).
 * 
 * Permite al usuario crear, editar, reordenar y eliminar sus propios accesos directos
 * con título personalizado, URL, icono predeterminado y color temático.
 *
 * @property id Identificador único del acceso directo
 * @property title Nombre visible del sitio o acceso
 * @property url Dirección web completa de destino
 * @property iconType Tipo de icono ('SEARCH', 'LANGUAGE', 'SPEED', 'BOOKMARK', 'STAR', 'PUBLIC', 'STORE', 'CODE')
 * @property colorHex Color en formato hexadecimal para el fondo e icono
 * @property orderIndex Posición ordinal para mostrar en la cuadrícula
 * @property createdAt Fecha de creación
 */
@Entity(tableName = "shortcuts")
data class ShortcutEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val url: String,
    val iconType: String = "LANGUAGE",
    val colorHex: String = "#00897B",
    val orderIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

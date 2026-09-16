package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad que representa una pestaña abierta en el navegador.
 * Permite persistir pestañas activas para que el usuario pueda reanudar su sesión.
 * 
 * @property id Identificador único de la pestaña
 * @property title Título de la página web cargada
 * @property url Dirección URL actual de la pestaña
 * @property isIncognito Indica si la pestaña pertenece al modo incógnito / privado
 * @property isProtected Indica si la pestaña opera en un contenedor protegido con aislamiento total de cookies y almacenamiento
 * @property contextId Identificador único del contenedor de contexto para Mozilla GeckoView (Multi-Account Container)
 * @property lastActiveTimestamp Momento en que la pestaña estuvo activa por última vez
 * @property orderIndex Posición ordinal de la pestaña en la barra de pestañas
 */
@Entity(tableName = "tabs")
data class TabEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String = "Nueva Pestaña",
    val url: String = "about:blank",
    val isIncognito: Boolean = false,
    val isProtected: Boolean = false,
    val contextId: String? = null,
    val lastActiveTimestamp: Long = System.currentTimeMillis(),
    val orderIndex: Int = 0
)

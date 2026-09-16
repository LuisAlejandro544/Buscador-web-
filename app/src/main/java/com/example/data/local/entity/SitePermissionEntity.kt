package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidad Room para el almacenamiento granular de permisos otorgados o denegados a sitios web.
 * 
 * Permite recordar qué orígenes web (dominios o URLs base) tienen acceso a recursos sensibles:
 * - Micrófono ([PERMISSION_MICROPHONE])
 * - Cámara ([PERMISSION_CAMERA])
 * - Geolocalización ([PERMISSION_GEOLOCATION])
 * - Notificaciones Web ([PERMISSION_NOTIFICATION])
 * - Almacenamiento Persistente ([PERMISSION_STORAGE])
 * 
 * Los permisos pueden tener estado [STATUS_GRANTED] o [STATUS_DENIED].
 */
@Entity(
    tableName = "site_permissions",
    indices = [Index(value = ["origin", "permissionType"], unique = true)]
)
data class SitePermissionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val origin: String,
    val permissionType: String,
    val status: String,
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val PERMISSION_MICROPHONE = "MICROPHONE"
        const val PERMISSION_CAMERA = "CAMERA"
        const val PERMISSION_GEOLOCATION = "GEOLOCATION"
        const val PERMISSION_NOTIFICATION = "NOTIFICATION"
        const val PERMISSION_STORAGE = "STORAGE"

        const val STATUS_GRANTED = "GRANTED"
        const val STATUS_DENIED = "DENIED"
    }
}

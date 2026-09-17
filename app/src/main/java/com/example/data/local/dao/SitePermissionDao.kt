package com.example.data.local.dao

import androidx.annotation.WorkerThread
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.SitePermissionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) para la gestión de permisos por sitio web en Room.
 * Proporciona consultas reactivas mediante Flow y operaciones CRUD para revocar o actualizar permisos.
 * Todas las operaciones de modificación y búsqueda directa deben ejecutarse fuera del hilo principal (UI Thread).
 */
@Dao
interface SitePermissionDao {

    @Query("SELECT * FROM site_permissions ORDER BY updatedAt DESC")
    fun getAllPermissions(): Flow<List<SitePermissionEntity>>

    @Query("SELECT * FROM site_permissions WHERE origin = :origin")
    fun getPermissionsForOrigin(origin: String): Flow<List<SitePermissionEntity>>

    @Query("SELECT * FROM site_permissions WHERE origin = :origin AND permissionType = :permissionType LIMIT 1")
    suspend fun findPermission(origin: String, permissionType: String): SitePermissionEntity?

    /**
     * Consulta síncrona exclusiva para hilos de fondo (@WorkerThread).
     * Prohibido invocar desde el hilo principal de la interfaz para evitar IllegalStateException.
     */
    @WorkerThread
    @Query("SELECT * FROM site_permissions WHERE origin = :origin AND permissionType = :permissionType LIMIT 1")
    fun findPermissionSync(origin: String, permissionType: String): SitePermissionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: SitePermissionEntity): Long

    @Query("UPDATE site_permissions SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM site_permissions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM site_permissions WHERE origin = :origin")
    suspend fun deleteByOrigin(origin: String)

    @Query("DELETE FROM site_permissions")
    suspend fun clearAll()
}

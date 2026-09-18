package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.ShortcutEntity
import kotlinx.coroutines.flow.Flow

/**
 * Objeto de acceso a datos (DAO) para los accesos directos configurables de la pantalla de inicio.
 */
@Dao
interface ShortcutDao {

    @Query("SELECT * FROM shortcuts ORDER BY orderIndex ASC, id ASC")
    fun getAllShortcuts(): Flow<List<ShortcutEntity>>

    @Query("SELECT * FROM shortcuts ORDER BY orderIndex ASC, id ASC")
    suspend fun getAllShortcutsList(): List<ShortcutEntity>

    @Query("SELECT COUNT(*) FROM shortcuts")
    suspend fun getCount(): Int

    @Query("SELECT * FROM shortcuts WHERE id = :id LIMIT 1")
    suspend fun getShortcutById(id: Long): ShortcutEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShortcut(shortcut: ShortcutEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(shortcuts: List<ShortcutEntity>)

    @Update
    suspend fun updateShortcut(shortcut: ShortcutEntity)

    @Delete
    suspend fun deleteShortcut(shortcut: ShortcutEntity)

    @Query("DELETE FROM shortcuts WHERE id = :id")
    suspend fun deleteShortcutById(id: Long)

    @Query("DELETE FROM shortcuts")
    suspend fun clearAllShortcuts()
}

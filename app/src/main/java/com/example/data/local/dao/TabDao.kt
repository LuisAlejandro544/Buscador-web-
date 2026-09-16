package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.TabEntity
import kotlinx.coroutines.flow.Flow

/**
 * Objeto de acceso a datos (DAO) para la gestión de pestañas en la base de datos local.
 * Proporciona métodos reactivos basados en Flow y operaciones asíncronas suspendidas.
 */
@Dao
interface TabDao {
    @Query("SELECT * FROM tabs WHERE isIncognito = 0 AND isProtected = 0 ORDER BY orderIndex ASC, id ASC")
    fun getNormalTabs(): Flow<List<TabEntity>>

    @Query("SELECT * FROM tabs WHERE isProtected = 1 ORDER BY orderIndex ASC, id ASC")
    fun getProtectedTabs(): Flow<List<TabEntity>>

    @Query("SELECT * FROM tabs WHERE isIncognito = 1 ORDER BY orderIndex ASC, id ASC")
    fun getIncognitoTabs(): Flow<List<TabEntity>>

    @Query("SELECT * FROM tabs WHERE isIncognito = :isIncognito ORDER BY orderIndex ASC, id ASC")
    fun getTabs(isIncognito: Boolean): Flow<List<TabEntity>>

    @Query("SELECT * FROM tabs WHERE id = :id LIMIT 1")
    suspend fun getTabById(id: Long): TabEntity?

    @Query("SELECT * FROM tabs ORDER BY lastActiveTimestamp DESC LIMIT 1")
    suspend fun getMostRecentActiveTab(): TabEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTab(tab: TabEntity): Long

    @Update
    suspend fun updateTab(tab: TabEntity)

    @Delete
    suspend fun deleteTab(tab: TabEntity)

    @Query("DELETE FROM tabs WHERE id = :id")
    suspend fun deleteTabById(id: Long)

    @Query("DELETE FROM tabs WHERE isIncognito = :isIncognito AND isProtected = 0")
    suspend fun clearTabs(isIncognito: Boolean)

    @Query("DELETE FROM tabs WHERE isProtected = 1")
    suspend fun clearProtectedTabs()

    @Query("SELECT COUNT(*) FROM tabs WHERE isIncognito = :isIncognito")
    fun getTabCount(isIncognito: Boolean): Flow<Int>

    @Query("SELECT COUNT(*) FROM tabs WHERE isProtected = 1")
    fun getProtectedTabCount(): Flow<Int>
}

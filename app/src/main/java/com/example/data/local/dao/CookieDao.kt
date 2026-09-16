package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.CookieEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) para la gestión y auditoría de cookies en la base de datos Room.
 * 
 * Ofrece consultas reactivas mediante Flow para listar cookies, filtrar rastreadores,
 * buscar por dominio/nombre y efectuar eliminaciones granulares o masivas.
 */
@Dao
interface CookieDao {

    /**
     * Obtiene todas las cookies registradas, priorizando primero los rastreadores detectados.
     */
    @Query("SELECT * FROM cookies ORDER BY isTracker DESC, createdAt DESC")
    fun getAllCookies(): Flow<List<CookieEntity>>

    /**
     * Búsqueda reactiva de cookies filtradas por dominio o nombre.
     */
    @Query("SELECT * FROM cookies WHERE domain LIKE '%' || :query || '%' OR name LIKE '%' || :query || '%' ORDER BY isTracker DESC, createdAt DESC")
    fun searchCookies(query: String): Flow<List<CookieEntity>>

    /**
     * Obtiene únicamente las cookies catalogadas como rastreadores de publicidad o analítica.
     */
    @Query("SELECT * FROM cookies WHERE isTracker = 1 ORDER BY createdAt DESC")
    fun getTrackerCookies(): Flow<List<CookieEntity>>

    /**
     * Obtiene la lista única de dominios que han depositado cookies en el navegador.
     */
    @Query("SELECT DISTINCT domain FROM cookies ORDER BY domain ASC")
    fun getDistinctDomains(): Flow<List<String>>

    /**
     * Obtiene las cookies asociadas a un dominio web específico.
     */
    @Query("SELECT * FROM cookies WHERE domain = :domain ORDER BY isTracker DESC")
    fun getCookiesByDomain(domain: String): Flow<List<CookieEntity>>

    /**
     * Retorna el número total de cookies almacenadas.
     */
    @Query("SELECT COUNT(*) FROM cookies")
    fun getCookieCount(): Flow<Int>

    /**
     * Retorna la cantidad de cookies clasificadas como rastreadores.
     */
    @Query("SELECT COUNT(*) FROM cookies WHERE isTracker = 1")
    fun getTrackerCookieCount(): Flow<Int>

    /**
     * Inserta o reemplaza una cookie.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCookie(cookie: CookieEntity): Long

    /**
     * Inserción masiva de cookies.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCookies(cookies: List<CookieEntity>)

    /**
     * Elimina una cookie específica.
     */
    @Delete
    suspend fun deleteCookie(cookie: CookieEntity)

    /**
     * Elimina una cookie por su identificador primario.
     */
    @Query("DELETE FROM cookies WHERE id = :id")
    suspend fun deleteCookieById(id: Long)

    /**
     * Elimina todas las cookies pertenecientes a un dominio específico.
     */
    @Query("DELETE FROM cookies WHERE domain = :domain")
    suspend fun deleteCookiesByDomain(domain: String)

    /**
     * Elimina todas las cookies de rastreo detectadas.
     */
    @Query("DELETE FROM cookies WHERE isTracker = 1")
    suspend fun deleteTrackerCookies()

    /**
     * Limpia completamente la tabla de cookies.
     */
    @Query("DELETE FROM cookies")
    suspend fun clearAllCookies()
}

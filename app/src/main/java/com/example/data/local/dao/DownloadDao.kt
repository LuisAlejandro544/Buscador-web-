package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.DownloadEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) para las operaciones de base de datos sobre descargas.
 * Permite listar en tiempo real mediante Coroutine Flows, insertar registros nuevos,
 * actualizar su estado y eliminar descargas individuales o el registro completo.
 */
@Dao
interface DownloadDao {

    /**
     * Obtiene el listado completo de descargas ordenadas cronológicamente de más reciente a más antigua.
     */
    @Query("SELECT * FROM downloads ORDER BY timestamp DESC")
    fun getAllDownloads(): Flow<List<DownloadEntity>>

    /**
     * Obtiene una descarga específica por su identificador primario en base de datos.
     */
    @Query("SELECT * FROM downloads WHERE id = :id LIMIT 1")
    suspend fun getDownloadById(id: Long): DownloadEntity?

    /**
     * Inserta un nuevo registro de descarga.
     * @return Identificador generado de la fila insertada.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: DownloadEntity): Long

    /**
     * Actualiza la información de una descarga (por ejemplo, cambio de estado o tamaño).
     */
    @Update
    suspend fun updateDownload(download: DownloadEntity)

    /**
     * Elimina un registro de descarga por su ID.
     */
    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteDownloadById(id: Long)

    /**
     * Borra la totalidad del registro de descargas del navegador.
     */
    @Query("DELETE FROM downloads")
    suspend fun clearAllDownloads()
}

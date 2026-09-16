package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.entity.UserAccountEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) para la gestión y persistencia de Cuentas de Usuario en Room.
 * Proporciona métodos reactivos basados en Flow y transacciones atómicas para
 * vincular, activar, actualizar y desvincular cuentas de Google y otros proveedores.
 */
@Dao
interface UserAccountDao {

    /**
     * Obtiene la lista completa de todas las cuentas vinculadas en la aplicación.
     */
    @Query("SELECT * FROM user_accounts ORDER BY isActive DESC, linkedAt DESC")
    fun getAllAccounts(): Flow<List<UserAccountEntity>>

    /**
     * Obtiene la cuenta actualmente seleccionada como activa en el navegador.
     */
    @Query("SELECT * FROM user_accounts WHERE isActive = 1 LIMIT 1")
    fun getActiveAccount(): Flow<UserAccountEntity?>

    /**
     * Obtiene la cuenta activa de forma directa y síncrona.
     */
    @Query("SELECT * FROM user_accounts WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveAccountDirect(): UserAccountEntity?

    /**
     * Busca una cuenta por su correo electrónico.
     */
    @Query("SELECT * FROM user_accounts WHERE email = :email LIMIT 1")
    suspend fun getAccountByEmail(email: String): UserAccountEntity?

    /**
     * Inserta una nueva cuenta en la base de datos local.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: UserAccountEntity): Long

    /**
     * Actualiza los datos de una cuenta existente.
     */
    @Update
    suspend fun updateAccount(account: UserAccountEntity)

    /**
     * Elimina una cuenta registrada.
     */
    @Delete
    suspend fun deleteAccount(account: UserAccountEntity)

    /**
     * Elimina una cuenta por su ID.
     */
    @Query("DELETE FROM user_accounts WHERE id = :id")
    suspend fun deleteAccountById(id: Long)

    /**
     * Desactiva el estado activo de todas las cuentas registradas.
     */
    @Query("UPDATE user_accounts SET isActive = 0")
    suspend fun deactivateAllAccounts()

    /**
     * Activa una cuenta específica como la principal del navegador.
     */
    @Query("UPDATE user_accounts SET isActive = 1 WHERE id = :id")
    suspend fun setAccountActiveById(id: Long)

    /**
     * Transacción atómica para alternar la cuenta activa del navegador.
     * Desactiva todas las demás y activa la seleccionada.
     */
    @Transaction
    suspend fun switchActiveAccount(id: Long) {
        deactivateAllAccounts()
        setAccountActiveById(id)
    }

    /**
     * Modifica el permiso de inicio de sesión automático en páginas web para una cuenta.
     */
    @Query("UPDATE user_accounts SET autoSignInWeb = :enabled WHERE id = :id")
    suspend fun updateAutoSignIn(id: Long, enabled: Boolean)

    /**
     * Obtiene el número total de cuentas vinculadas.
     */
    @Query("SELECT COUNT(*) FROM user_accounts")
    fun getAccountsCount(): Flow<Int>

    /**
     * Limpia todas las cuentas vinculadas.
     */
    @Query("DELETE FROM user_accounts")
    suspend fun clearAllAccounts()
}

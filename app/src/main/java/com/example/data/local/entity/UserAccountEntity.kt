package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.security.KeyStoreCryptoHelper

/**
 * Entidad de persistencia para Cuentas de Usuario vinculadas al Navegador.
 * 
 * Permite almacenar y gestionar identidades digitales (como cuentas de Google,
 * proveedores federados o identidades locales) asociadas al navegador.
 * 
 * SEGURIDAD:
 * Los tokens sensibles ([idToken], [serverAuthCode]) se almacenan cifrados en reposo
 * mediante AES-256 GCM con claves respaldadas en el hardware seguro de Android Keystore,
 * impidiendo la exposición de credenciales si la base de datos SQLite es extraída.
 *
 * @property id Identificador único autonumérico de la cuenta registrada.
 * @property email Correo electrónico principal de la cuenta.
 * @property displayName Nombre de usuario o nombre completo visible.
 * @property photoUrl URL del avatar o foto de perfil del usuario.
 * @property provider Proveedor de identidad ("GOOGLE", "MICROSOFT", "CUSTOM").
 * @property isActive Indica si esta cuenta es la cuenta activa actualmente en el navegador.
 * @property autoSignInWeb Permite el inicio de sesión automático y autocompletado en sitios web con soporte de Google One-Tap/FedCM.
 * @property idToken Token criptográfico o credencial persistida cifrada en reposo.
 * @property serverAuthCode Código de autorización para sincronizaciones avanzadas cifrado en reposo.
 * @property linkedAt Marca temporal (timestamp en ms) en que fue vinculada la cuenta.
 */
@Entity(tableName = "user_accounts")
data class UserAccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val email: String,
    val displayName: String,
    val photoUrl: String? = null,
    val provider: String = "GOOGLE",
    val isActive: Boolean = true,
    val autoSignInWeb: Boolean = true,
    val idToken: String? = null,
    val serverAuthCode: String? = null,
    val linkedAt: Long = System.currentTimeMillis()
) {
    /**
     * Retorna el idToken descifrado en memoria para su uso seguro en flujos de autenticación.
     */
    fun getDecryptedIdToken(): String? {
        return KeyStoreCryptoHelper.decrypt(idToken)
    }

    /**
     * Retorna el serverAuthCode descifrado en memoria.
     */
    fun getDecryptedServerAuthCode(): String? {
        return KeyStoreCryptoHelper.decrypt(serverAuthCode)
    }

    companion object {
        /**
         * Crea una instancia de [UserAccountEntity] asegurando el cifrado en reposo
         * de los tokens de autenticación antes de persistirla en Room.
         */
        fun createSecure(
            id: Long = 0,
            email: String,
            displayName: String,
            photoUrl: String? = null,
            provider: String = "GOOGLE",
            isActive: Boolean = true,
            autoSignInWeb: Boolean = true,
            plainIdToken: String? = null,
            plainServerAuthCode: String? = null,
            linkedAt: Long = System.currentTimeMillis()
        ): UserAccountEntity {
            return UserAccountEntity(
                id = id,
                email = email,
                displayName = displayName,
                photoUrl = photoUrl,
                provider = provider,
                isActive = isActive,
                autoSignInWeb = autoSignInWeb,
                idToken = KeyStoreCryptoHelper.encrypt(plainIdToken),
                serverAuthCode = KeyStoreCryptoHelper.encrypt(plainServerAuthCode),
                linkedAt = linkedAt
            )
        }
    }
}


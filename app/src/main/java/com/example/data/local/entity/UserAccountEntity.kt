package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad de persistencia para Cuentas de Usuario vinculadas al Navegador.
 * 
 * Permite almacenar y gestionar identidades digitales (como cuentas de Google,
 * proveedores federados o identidades locales) asociadas al navegador.
 * Estas credenciales se utilizan para el inicio de sesión con un solo toque
 * en sitios web y la sincronización de perfiles.
 *
 * @property id Identificador único autonumérico de la cuenta registrada.
 * @property email Correo electrónico principal de la cuenta.
 * @property displayName Nombre de usuario o nombre completo visible.
 * @property photoUrl URL del avatar o foto de perfil del usuario.
 * @property provider Proveedor de identidad ("GOOGLE", "MICROSOFT", "CUSTOM").
 * @property isActive Indica si esta cuenta es la cuenta activa actualmente en el navegador.
 * @property autoSignInWeb Permite el inicio de sesión automático y autocompletado en sitios web con soporte de Google One-Tap/FedCM.
 * @property idToken Token criptográfico o credencial persistida para autenticación.
 * @property serverAuthCode Código de autorización para sincronizaciones avanzadas.
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
)

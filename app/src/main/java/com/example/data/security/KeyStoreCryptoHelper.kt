package com.example.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Utilidad de cifrado criptográfico respaldada por hardware mediante Android Keystore.
 *
 * Utiliza el algoritmo AES/GCM/NoPadding (256 bits) para proteger credenciales,
 * tokens OAuth y claves sensibles almacenadas localmente en la base de datos Room,
 * garantizando confidencialidad e integridad ante accesos no autorizados al almacenamiento.
 */
object KeyStoreCryptoHelper {

    private const val TAG = "KeyStoreCryptoHelper"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "browser_user_tokens_aes_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128

    /**
     * Obtiene o genera la clave simétrica AES-256 en el enclave seguro de Android Keystore.
     */
    @Synchronized
    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            val keySpec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()

            keyGenerator.init(keySpec)
            return keyGenerator.generateKey()
        }
        return keyStore.getKey(KEY_ALIAS, null) as SecretKey
    }

    /**
     * Cifra una cadena de texto sensible con AES-GCM, prefijando el Vector de Inicialización (IV).
     *
     * @param plainText Texto en claro a cifrar.
     * @return Cadena codificada en Base64 que contiene [IV + Texto Cifrado + Tag de Autenticación].
     */
    fun encrypt(plainText: String?): String? {
        if (plainText.isNullOrEmpty()) return null
        return try {
            val key = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)

            val iv = cipher.iv
            val cipherBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

            val combined = ByteArray(iv.size + cipherBytes.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(cipherBytes, 0, combined, iv.size, cipherBytes.size)

            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            // Seguridad Fail-Closed: nunca retornar datos sensibles en texto plano si el cifrado falla
            Log.e(TAG, "Error crítico durante el cifrado en Android Keystore: ${e.message}", e)
            null
        }
    }

    /**
     * Descifra una cadena previamente cifrada con [encrypt].
     *
     * @param encryptedText Cadena Base64 con [IV + Texto Cifrado].
     * @return Texto en claro descifrado o null si no se pudo descifrar de manera íntegra y segura.
     */
    fun decrypt(encryptedText: String?): String? {
        if (encryptedText.isNullOrEmpty()) return null
        return try {
            val decoded = Base64.decode(encryptedText, Base64.NO_WRAP)
            if (decoded.size <= GCM_IV_LENGTH) return null

            val iv = ByteArray(GCM_IV_LENGTH)
            val cipherBytes = ByteArray(decoded.size - GCM_IV_LENGTH)
            System.arraycopy(decoded, 0, iv, 0, GCM_IV_LENGTH)
            System.arraycopy(decoded, GCM_IV_LENGTH, cipherBytes, 0, cipherBytes.size)

            val key = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)

            val plainBytes = cipher.doFinal(cipherBytes)
            String(plainBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            // Seguridad Fail-Closed: si el tag de autenticación GCM o la clave fallan, rechazar los datos
            Log.e(TAG, "Error crítico durante el descifrado: los datos están corruptos o la clave no coincide: ${e.message}", e)
            null
        }
    }
}

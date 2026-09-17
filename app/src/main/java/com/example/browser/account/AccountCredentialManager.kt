package com.example.browser.account

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import java.security.MessageDigest
import java.util.UUID

/**
 * Resultado estructurado del proceso de inicio de sesión o vinculación de credencial.
 */
data class AccountSignInResult(
    val isSuccess: Boolean,
    val email: String? = null,
    val displayName: String? = null,
    val photoUrl: String? = null,
    val idToken: String? = null,
    val errorMessage: String? = null
)

/**
 * Gestor de Credenciales de Usuario y Autenticación Nativa.
 * 
 * Utiliza la API moderna Android Credential Manager (androidx.credentials) y Google ID
 * para permitir la vinculación de cuentas de Google con un solo toque, gestionando
 * de manera transparente entornos con o sin Google Play Services (ideal para distribución en Uptodown).
 */
class AccountCredentialManager(private val context: Context) {

    private val credentialManager = CredentialManager.create(context)
    private val tag = "AccountCredentialManager"

    /**
     * Resuelve de forma recursiva una instancia de [Activity] desenrollando cualquier [ContextWrapper].
     * Esto es indispensable ya que Android Credential Manager exige un contexto de ventana activa
     * para inflar el selector nativo de cuentas de usuario.
     */
    private fun findActivity(ctx: Context): Activity? {
        var current = ctx
        while (current is ContextWrapper) {
            if (current is Activity) return current
            current = current.baseContext
        }
        return null
    }

    /**
     * Inicia el flujo nativo de selección de cuenta de Google mediante Credential Manager.
     * 
     * @param activityContext Contexto visual de la pantalla (Activity) requerido para desplegar el selector nativo.
     * @param serverClientId ID de cliente OAuth 2.0 web (opcional si se utiliza vinculación local).
     * @return [AccountSignInResult] con los datos del perfil y token obtenido, o error descriptivo.
     */
    suspend fun signInWithGoogle(
        activityContext: Context? = null,
        serverClientId: String? = null
    ): AccountSignInResult {
        return try {
            // Resolver Activity para cumplir con el requisito estricto de CredentialManager
            val resolvedContext = (activityContext ?: context).let { ctx ->
                findActivity(ctx) ?: ctx
            }

            val activeManager = if (resolvedContext is Activity) {
                CredentialManager.create(resolvedContext)
            } else {
                credentialManager
            }

            val clientId = serverClientId ?: "default_web_client_id.apps.googleusercontent.com"
            
            // Generar nonce criptográfico para mitigar ataques de repetición
            val rawNonce = UUID.randomUUID().toString()
            val md = MessageDigest.getInstance("SHA-256")
            val hashedNonce = md.digest(rawNonce.toByteArray()).joinToString("") { "%02x".format(it) }

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(clientId)
                .setAutoSelectEnabled(false)
                .setNonce(hashedNonce)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val response = activeManager.getCredential(
                request = request,
                context = resolvedContext
            )

            val credential = response.credential
            when (credential) {
                is androidx.credentials.CustomCredential -> {
                    if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                        val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data)
                        AccountSignInResult(
                            isSuccess = true,
                            email = googleIdToken.id,
                            displayName = googleIdToken.displayName ?: googleIdToken.givenName ?: googleIdToken.id,
                            photoUrl = googleIdToken.profilePictureUri?.toString(),
                            idToken = googleIdToken.idToken
                        )
                    } else {
                        AccountSignInResult(
                            isSuccess = false,
                            errorMessage = "Tipo de credencial no compatible: ${credential.type}"
                        )
                    }
                }
                else -> {
                    AccountSignInResult(
                        isSuccess = false,
                        errorMessage = "Credencial devuelta no reconocida"
                    )
                }
            }
        } catch (e: GetCredentialCancellationException) {
            Log.d(tag, "El usuario canceló la selección de cuenta de Google")
            AccountSignInResult(isSuccess = false, errorMessage = "Operación cancelada por el usuario")
        } catch (e: NoCredentialException) {
            Log.w(tag, "No hay cuentas de Google configuradas en el dispositivo", e)
            AccountSignInResult(
                isSuccess = false,
                errorMessage = "No se encontraron credenciales de Google disponibles en este dispositivo"
            )
        } catch (e: GetCredentialException) {
            Log.e(tag, "Fallo al obtener credencial: ${e.message}", e)
            AccountSignInResult(
                isSuccess = false,
                errorMessage = "Error en el servicio de credenciales: ${e.message}"
            )
        } catch (e: Exception) {
            Log.e(tag, "Excepción inesperada en autenticación: ${e.message}", e)
            AccountSignInResult(
                isSuccess = false,
                errorMessage = "Error al vincular cuenta: ${e.localizedMessage ?: "Error desconocido"}"
            )
        }
    }
}

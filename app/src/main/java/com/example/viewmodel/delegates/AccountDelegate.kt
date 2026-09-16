package com.example.viewmodel.delegates

import android.app.Application
import com.example.browser.account.AccountCredentialManager
import com.example.browser.account.WebSignInBridge
import com.example.browser.account.WebSignInPrompt
import com.example.browser.engine.BrowserEngineContract
import com.example.data.local.entity.UserAccountEntity
import com.example.data.repository.BrowserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Delegado de gestión de cuentas de usuario y sincronización web asistida.
 * 
 * Responsabilidades:
 * - Gestionar cuentas vinculadas (Google / Personalizadas) y credenciales seguras.
 * - Coordinar el banner flotante de inicio de sesión con un toque (WebSignInPrompt).
 * - Evaluar e inyectar el script JS de autenticación en la página activa mediante el motor del navegador.
 */
class AccountDelegate(
    application: Application,
    private val repository: BrowserRepository,
    private val scope: CoroutineScope,
    private val getEngineController: () -> BrowserEngineContract?
) {
    val credentialManager = AccountCredentialManager(application)

    val allAccounts: StateFlow<List<UserAccountEntity>> = repository.getAllAccounts()
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeAccount: StateFlow<UserAccountEntity?> = repository.getActiveAccount()
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), null)

    val accountsCount: StateFlow<Int> = repository.getAccountsCount()
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), 0)

    private val _webSignInPrompt = MutableStateFlow<WebSignInPrompt?>(null)
    val webSignInPrompt: StateFlow<WebSignInPrompt?> = _webSignInPrompt.asStateFlow()

    fun postWebSignInPrompt(prompt: WebSignInPrompt) {
        _webSignInPrompt.value = prompt
    }

    /**
     * Vincula una cuenta de usuario en la base de datos local.
     */
    fun linkAccount(
        email: String,
        displayName: String,
        photoUrl: String? = null,
        provider: String = "GOOGLE",
        idToken: String? = null,
        autoSignInWeb: Boolean = true
    ) {
        scope.launch {
            val account = UserAccountEntity.createSecure(
                email = email,
                displayName = displayName,
                photoUrl = photoUrl,
                provider = provider,
                isActive = true,
                autoSignInWeb = autoSignInWeb,
                plainIdToken = idToken
            )
            repository.linkAccount(account)
        }
    }

    /**
     * Alterna la cuenta activa principal del navegador.
     */
    fun switchActiveAccount(accountId: Long) {
        scope.launch {
            repository.setActiveAccount(accountId)
        }
    }

    /**
     * Modifica el permiso de inicio de sesión automático web para una cuenta.
     */
    fun toggleAutoSignInWeb(accountId: Long, enabled: Boolean) {
        scope.launch {
            repository.setAutoSignInWeb(accountId, enabled)
        }
    }

    /**
     * Desvincula y elimina una cuenta registrada del navegador.
     */
    fun removeAccount(accountId: Long) {
        scope.launch {
            repository.removeAccount(accountId)
        }
    }

    /**
     * Acepta el inicio de sesión web con la cuenta activa e inyecta la asistencia en la página actual.
     */
    fun acceptWebSignInPrompt(account: UserAccountEntity) {
        _webSignInPrompt.value = null
        val script = WebSignInBridge.generateAutoSignInScript(account)
        getEngineController()?.evaluateJavascript(script)
    }

    /**
     * Descarta el prompt visual de inicio de sesión web asistido.
     */
    fun dismissWebSignInPrompt() {
        _webSignInPrompt.value = null
    }
}

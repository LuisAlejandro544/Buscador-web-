package com.example.browser.engine

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.viewmodel.BrowserViewModel
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.GeckoView
import org.mozilla.geckoview.WebResponse

/**
 * Motor de navegación web de alto rendimiento basado en Mozilla GeckoView Omni.
 * 
 * Sustituye por completo al WebView del sistema, ofreciendo:
 * - Renderizado web independiente de estándares abiertos (Gecko Engine).
 * - Protección mejorada contra el rastreo (Enhanced Tracking Protection).
 * - Aislamiento estricto de sesiones privadas e incógnito.
 * - Soporte avanzado para esquemas externos y recuperación de fallos.
 */
class GeckoViewEngine(
    private val context: Context,
    val geckoView: GeckoView,
    private val viewModel: BrowserViewModel,
    private var currentSession: GeckoSession
) : BrowserEngineContract {

    init {
        bindSession(currentSession)
    }

    /**
     * Vincula una sesión de navegación activa con la vista GeckoView y configura sus delegados.
     */
    fun bindSession(session: GeckoSession) {
        currentSession = session
        geckoView.setSession(session)
        setupDelegates(session)
    }

    /**
     * Configura los delegados de navegación, progreso y contenido para la sesión.
     */
    private fun setupDelegates(session: GeckoSession) {
        // 1. Delegado de Navegación: control de historial, cambio de URL y esquemas externos
        session.navigationDelegate = object : GeckoSession.NavigationDelegate {
            override fun onCanGoBack(s: GeckoSession, canGoBack: Boolean) {
                viewModel.onNavigationStateChanged(canGoBack, viewModel.pageState.value.canGoForward)
            }

            override fun onCanGoForward(s: GeckoSession, canGoForward: Boolean) {
                viewModel.onNavigationStateChanged(viewModel.pageState.value.canGoBack, canGoForward)
            }

            override fun onLocationChange(
                s: GeckoSession,
                url: String?,
                perms: MutableList<GeckoSession.PermissionDelegate.ContentPermission>,
                hasUserGesture: Boolean
            ) {
                url?.let { validUrl ->
                    if (validUrl.isNotBlank() && validUrl != "about:blank" && validUrl != "about:home") {
                        viewModel.onPageStarted(validUrl)
                        viewModel.onPageFinished(validUrl, viewModel.pageState.value.title)
                    }
                }
            }

            override fun onLoadRequest(
                s: GeckoSession,
                request: GeckoSession.NavigationDelegate.LoadRequest
            ): GeckoResult<AllowOrDeny>? {
                val uriString = request.uri
                // Manejo de esquemas nativos y externos (mailto, tel, intent, app links)
                if (!uriString.startsWith("http://") &&
                    !uriString.startsWith("https://") &&
                    !uriString.startsWith("about:") &&
                    !uriString.startsWith("javascript:") &&
                    !uriString.startsWith("data:")
                ) {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString)).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                        return GeckoResult.fromValue(AllowOrDeny.DENY)
                    } catch (e: Exception) {
                        return GeckoResult.fromValue(AllowOrDeny.DENY)
                    }
                }
                return GeckoResult.fromValue(AllowOrDeny.ALLOW)
            }

            override fun onNewSession(
                s: GeckoSession,
                uri: String
            ): GeckoResult<GeckoSession>? {
                // Al abrir ventanas emergentes o target=_blank, delegar a nueva pestaña
                viewModel.createNewTab(uri, viewModel.isIncognitoMode.value)
                return null
            }
        }

        // 2. Delegado de Progreso: barra de progreso, estado de seguridad SSL y carga
        session.progressDelegate = object : GeckoSession.ProgressDelegate {
            override fun onPageStart(s: GeckoSession, url: String) {
                viewModel.onPageStarted(url)
            }

            override fun onPageStop(s: GeckoSession, success: Boolean) {
                val currentUrl = viewModel.pageState.value.url
                viewModel.onPageFinished(currentUrl, viewModel.pageState.value.title)
            }

            override fun onProgressChange(s: GeckoSession, progress: Int) {
                viewModel.onProgressChanged(progress)
            }

            override fun onSecurityChange(
                s: GeckoSession,
                securityInfo: GeckoSession.ProgressDelegate.SecurityInformation
            ) {
                viewModel.onSecurityChanged(securityInfo.isSecure)
            }
        }

        // 3. Delegado de Contenido: título de página, descargas y recuperación ante caídas
        session.contentDelegate = object : GeckoSession.ContentDelegate {
            override fun onTitleChange(s: GeckoSession, title: String?) {
                val currentUrl = viewModel.pageState.value.url
                if (currentUrl.isNotBlank() && currentUrl != "about:blank" && currentUrl != "about:home") {
                    viewModel.onPageFinished(currentUrl, title)
                }
            }

            override fun onExternalResponse(s: GeckoSession, response: WebResponse) {
                val downloadUrl = response.uri
                val contentDisposition = response.headers["Content-Disposition"]
                val mimeType = response.headers["Content-Type"]
                val contentLength = response.headers["Content-Length"]?.toLongOrNull() ?: 0L
                viewModel.initiateDownload(downloadUrl, contentDisposition, mimeType, contentLength)
            }

            override fun onCrash(s: GeckoSession) {
                // Recuperación automática si el proceso de contenido de Gecko experimenta un fallo
                s.reload()
            }

            override fun onKill(s: GeckoSession) {
                s.reload()
            }
        }

        // 4. Delegado de Diálogos Web: Alertas JS, confirmaciones, prompts y selector de archivos
        session.promptDelegate = GeckoPromptHandler(context, viewModel)
    }

    override fun loadUrl(url: String) {
        if (url == "about:home" || url.isBlank()) {
            return
        }
        if (url == "about:blank") {
            currentSession.loadUri("about:blank")
            return
        }
        currentSession.loadUri(url)
    }

    override fun goBack() {
        currentSession.goBack()
    }

    override fun goForward() {
        currentSession.goForward()
    }

    override fun reload() {
        currentSession.reload()
    }

    override fun stopLoading() {
        currentSession.stop()
    }

    override fun setDesktopMode(enabled: Boolean) {
        currentSession.settings.userAgentMode = if (enabled) {
            GeckoSessionSettings.USER_AGENT_MODE_DESKTOP
        } else {
            GeckoSessionSettings.USER_AGENT_MODE_MOBILE
        }
        currentSession.settings.viewportMode = if (enabled) {
            GeckoSessionSettings.VIEWPORT_MODE_DESKTOP
        } else {
            GeckoSessionSettings.VIEWPORT_MODE_MOBILE
        }
        currentSession.reload()
    }

    override fun setJavaScriptEnabled(enabled: Boolean) {
        currentSession.settings.allowJavascript = enabled
    }

    override fun clearCache() {
        GeckoRuntimeProvider.clearAllData(context)
    }

    override fun evaluateJavascript(script: String, callback: ((String) -> Unit)?) {
        currentSession.loadUri("javascript:$script")
        callback?.invoke("")
    }

    /**
     * Libera la vinculación con la vista cuando el composable sale de pantalla.
     */
    fun release() {
        geckoView.releaseSession()
    }
}

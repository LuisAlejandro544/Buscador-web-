package com.example.browser.engine

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import com.example.data.local.entity.SitePermissionEntity
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

                // 1. Bloqueo estricto de esquemas de archivos locales y proveedores de contenido desde la web
                if (uriString.startsWith("file:", ignoreCase = true) ||
                    uriString.startsWith("content:", ignoreCase = true)
                ) {
                    return GeckoResult.fromValue(AllowOrDeny.DENY)
                }

                // 2. Evaluación prioritaria de Ciberseguridad: Anti-Phishing, Malware y Fraude Web
                val detectedThreat = viewModel.evaluateNavigationSecurity(uriString)
                if (detectedThreat != null) {
                    return GeckoResult.fromValue(AllowOrDeny.DENY)
                }

                // 3. Intercepción preventiva de publicidad y rastreo con el motor adblock nativo en Rust (core-native)
                val currentParentUrl = viewModel.pageState.value.url
                if (viewModel.shouldBlockUrlRequest(uriString, currentParentUrl, "subdocument")) {
                    return GeckoResult.fromValue(AllowOrDeny.DENY)
                }

                // 3. Permitir protocolos web estándar y recursos embebidos
                if (uriString.startsWith("http://", ignoreCase = true) ||
                    uriString.startsWith("https://", ignoreCase = true) ||
                    uriString.startsWith("about:", ignoreCase = true) ||
                    uriString.startsWith("javascript:", ignoreCase = true) ||
                    uriString.startsWith("data:", ignoreCase = true)
                ) {
                    return GeckoResult.fromValue(AllowOrDeny.ALLOW)
                }

                // 3. Manejo seguro y sanitizado del esquema intent://
                if (uriString.startsWith("intent:", ignoreCase = true)) {
                    try {
                        val parsedIntent = Intent.parseUri(uriString, Intent.URI_INTENT_SCHEME)
                        // Sanitización crítica: neutralizar componentes internos, selectores y forzar navegabilidad
                        parsedIntent.addCategory(Intent.CATEGORY_BROWSABLE)
                        parsedIntent.component = null
                        parsedIntent.selector = null

                        // Bloquear cualquier intento de llamar a componentes del propio paquete del navegador
                        if (parsedIntent.`package` == context.packageName) {
                            return GeckoResult.fromValue(AllowOrDeny.DENY)
                        }

                        if (context.packageManager.resolveActivity(parsedIntent, 0) != null) {
                            parsedIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            context.startActivity(parsedIntent)
                        }
                    } catch (_: Exception) {
                        // Denegar en caso de esquema inválido o excepción
                    }
                    return GeckoResult.fromValue(AllowOrDeny.DENY)
                }

                // 4. Manejo de esquemas externos conocidos y seguros (mailto, tel, sms, geo, market)
                try {
                    val parsedUri = Uri.parse(uriString)
                    val scheme = parsedUri.scheme?.lowercase()
                    val safeSchemes = setOf("mailto", "tel", "sms", "geo", "market")
                    if (scheme in safeSchemes) {
                        val externalIntent = Intent(Intent.ACTION_VIEW, parsedUri).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            addCategory(Intent.CATEGORY_BROWSABLE)
                        }
                        if (context.packageManager.resolveActivity(externalIntent, 0) != null) {
                            context.startActivity(externalIntent)
                        }
                    }
                } catch (_: Exception) {
                    // Ignorar esquemas desconocidos o corruptos
                }

                return GeckoResult.fromValue(AllowOrDeny.DENY)
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

        // 5. Delegado de Permisos: Control granular persistente por sitio y políticas de bloqueo silencioso
        session.permissionDelegate = object : GeckoSession.PermissionDelegate {
            override fun onMediaPermissionRequest(
                s: GeckoSession,
                uri: String,
                video: Array<GeckoSession.PermissionDelegate.MediaSource>?,
                audio: Array<GeckoSession.PermissionDelegate.MediaSource>?,
                callback: GeckoSession.PermissionDelegate.MediaCallback
            ) {
                // Denegar estrictamente en incógnito para neutralizar fugas de IP por WebRTC
                if (s.settings.usePrivateMode) {
                    callback.reject()
                    return
                }

                // Extraer el dominio base u origen limpio
                val origin = try {
                    val parsed = Uri.parse(uri)
                    val host = parsed.host
                    if (!host.isNullOrBlank()) "${parsed.scheme ?: "https"}://$host" else uri
                } catch (_: Throwable) {
                    uri
                }

                // 1. Verificar política de bloqueo silencioso para multimedia
                if (viewModel.blockMediaPrompts.value) {
                    callback.reject()
                    return
                }

                val hasVideo = !video.isNullOrEmpty()
                val hasAudio = !audio.isNullOrEmpty()

                // 2. Comprobar permisos persistentes almacenados en la base de datos
                val videoPerm = if (hasVideo) viewModel.findSitePermissionSync(origin, SitePermissionEntity.PERMISSION_CAMERA) else null
                val audioPerm = if (hasAudio) viewModel.findSitePermissionSync(origin, SitePermissionEntity.PERMISSION_MICROPHONE) else null

                // Si alguno está explícitamente bloqueado, rechazar
                if ((hasVideo && videoPerm?.status == SitePermissionEntity.STATUS_DENIED) ||
                    (hasAudio && audioPerm?.status == SitePermissionEntity.STATUS_DENIED)
                ) {
                    callback.reject()
                    return
                }

                // Si ambos están permitidos explícitamente, conceder de inmediato
                val isVideoSatisfied = !hasVideo || videoPerm?.status == SitePermissionEntity.STATUS_GRANTED
                val isAudioSatisfied = !hasAudio || audioPerm?.status == SitePermissionEntity.STATUS_GRANTED
                if (isVideoSatisfied && isAudioSatisfied && (videoPerm != null || audioPerm != null)) {
                    callback.grant(video?.firstOrNull(), audio?.firstOrNull())
                    return
                }

                // 3. No decidido previamente: Mostrar diálogo interactivo nativo al usuario
                val permType = if (hasVideo && hasAudio) {
                    SitePermissionEntity.PERMISSION_CAMERA
                } else if (hasVideo) {
                    SitePermissionEntity.PERMISSION_CAMERA
                } else {
                    SitePermissionEntity.PERMISSION_MICROPHONE
                }

                val mediaLabel = if (hasVideo && hasAudio) {
                    "la cámara y el micrófono"
                } else if (hasVideo) {
                    "la cámara"
                } else {
                    "el micrófono"
                }

                viewModel.postWebPrompt(
                    WebPromptRequest.Permission(
                        origin = origin,
                        permissionType = permType,
                        title = "Permiso de multimedia",
                        message = "El sitio \"$origin\" solicita acceso a $mediaLabel.",
                        onGrant = { remember ->
                            if (remember) {
                                if (hasVideo) viewModel.saveSitePermission(origin, SitePermissionEntity.PERMISSION_CAMERA, SitePermissionEntity.STATUS_GRANTED)
                                if (hasAudio) viewModel.saveSitePermission(origin, SitePermissionEntity.PERMISSION_MICROPHONE, SitePermissionEntity.STATUS_GRANTED)
                            }
                            callback.grant(video?.firstOrNull(), audio?.firstOrNull())
                            viewModel.dismissWebPrompt()
                        },
                        onDeny = { remember ->
                            if (remember) {
                                if (hasVideo) viewModel.saveSitePermission(origin, SitePermissionEntity.PERMISSION_CAMERA, SitePermissionEntity.STATUS_DENIED)
                                if (hasAudio) viewModel.saveSitePermission(origin, SitePermissionEntity.PERMISSION_MICROPHONE, SitePermissionEntity.STATUS_DENIED)
                            }
                            callback.reject()
                            viewModel.dismissWebPrompt()
                        }
                    )
                )
            }

            override fun onContentPermissionRequest(
                s: GeckoSession,
                perm: GeckoSession.PermissionDelegate.ContentPermission
            ): GeckoResult<Int>? {
                if (s.settings.usePrivateMode || perm.privateMode) {
                    // Rechazar geolocalización, notificaciones y persistencia en incógnito
                    return GeckoResult.fromValue(GeckoSession.PermissionDelegate.ContentPermission.VALUE_DENY)
                }

                val origin = try {
                    val parsed = Uri.parse(perm.uri)
                    val host = parsed.host
                    if (!host.isNullOrBlank()) "${parsed.scheme ?: "https"}://$host" else perm.uri
                } catch (_: Throwable) {
                    perm.uri
                }

                // Mapear el tipo de permiso de GeckoView a nuestro modelo de datos
                val permType = when (perm.permission) {
                    GeckoSession.PermissionDelegate.PERMISSION_DESKTOP_NOTIFICATION -> SitePermissionEntity.PERMISSION_NOTIFICATION
                    GeckoSession.PermissionDelegate.PERMISSION_GEOLOCATION -> SitePermissionEntity.PERMISSION_GEOLOCATION
                    GeckoSession.PermissionDelegate.PERMISSION_PERSISTENT_STORAGE -> SitePermissionEntity.PERMISSION_STORAGE
                    else -> "CONTENT_PERMISSION_${perm.permission}"
                }

                // 1. Políticas de bloqueo silencioso ("No Preguntar")
                if (perm.permission == GeckoSession.PermissionDelegate.PERMISSION_DESKTOP_NOTIFICATION &&
                    viewModel.blockNotificationPrompts.value
                ) {
                    return GeckoResult.fromValue(GeckoSession.PermissionDelegate.ContentPermission.VALUE_DENY)
                }

                if (perm.permission == GeckoSession.PermissionDelegate.PERMISSION_GEOLOCATION &&
                    viewModel.blockLocationPrompts.value
                ) {
                    return GeckoResult.fromValue(GeckoSession.PermissionDelegate.ContentPermission.VALUE_DENY)
                }

                // 2. Verificar decisión previa almacenada en Room
                val existing = viewModel.findSitePermissionSync(origin, permType)
                if (existing != null) {
                    return if (existing.status == SitePermissionEntity.STATUS_GRANTED) {
                        GeckoResult.fromValue(GeckoSession.PermissionDelegate.ContentPermission.VALUE_ALLOW)
                    } else {
                        GeckoResult.fromValue(GeckoSession.PermissionDelegate.ContentPermission.VALUE_DENY)
                    }
                }

                // 3. No decidido: Mostrar diálogo de solicitud interactivo
                val result = GeckoResult<Int>()
                val permDescription = when (perm.permission) {
                    GeckoSession.PermissionDelegate.PERMISSION_DESKTOP_NOTIFICATION -> "enviarte notificaciones web"
                    GeckoSession.PermissionDelegate.PERMISSION_GEOLOCATION -> "conocer tu ubicación geográfica"
                    GeckoSession.PermissionDelegate.PERMISSION_PERSISTENT_STORAGE -> "usar almacenamiento persistente en el dispositivo"
                    else -> "acceder a funciones del dispositivo"
                }

                viewModel.postWebPrompt(
                    WebPromptRequest.Permission(
                        origin = origin,
                        permissionType = permType,
                        title = "Solicitud de permiso",
                        message = "El sitio \"$origin\" solicita permiso para $permDescription.",
                        onGrant = { remember ->
                            if (remember) {
                                viewModel.saveSitePermission(origin, permType, SitePermissionEntity.STATUS_GRANTED)
                            }
                            result.complete(GeckoSession.PermissionDelegate.ContentPermission.VALUE_ALLOW)
                            viewModel.dismissWebPrompt()
                        },
                        onDeny = { remember ->
                            if (remember) {
                                viewModel.saveSitePermission(origin, permType, SitePermissionEntity.STATUS_DENIED)
                            }
                            result.complete(GeckoSession.PermissionDelegate.ContentPermission.VALUE_DENY)
                            viewModel.dismissWebPrompt()
                        }
                    )
                )

                return result
            }
        }
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
     * Captura una instantánea visual en píxeles del contenido web actualmente renderizado
     * en GeckoView para generar la miniatura de la pestaña en segundo plano.
     */
    fun captureThumbnail(onCaptured: (Bitmap) -> Unit) {
        try {
            geckoView.capturePixels().then(
                GeckoResult.OnValueListener { bitmap: Bitmap? ->
                    if (bitmap != null) {
                        onCaptured(bitmap)
                    }
                    GeckoResult.fromValue(null)
                }
            )
        } catch (_: Throwable) {
            // Manejo preventivo si la vista aún no está lista para capturar píxeles
        }
    }

    /**
     * Libera la vinculación con la vista cuando el composable sale de pantalla.
     */
    fun release() {
        geckoView.releaseSession()
    }
}

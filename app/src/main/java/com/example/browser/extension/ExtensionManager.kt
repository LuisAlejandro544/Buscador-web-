package com.example.browser.extension

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.browser.engine.GeckoRuntimeProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.WebExtension
import org.mozilla.geckoview.WebExtensionController
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Administrador centralizado de extensiones WebExtension para GeckoView.
 * 
 * Responsabilidades:
 * - Descarga bajo demanda de extensiones oficiales (.xpi) desde Mozilla Add-ons (AMO).
 * - Aislamiento legal y de almacenamiento: ningún archivo .xpi se distribuye dentro del APK.
 * - Instalación, habilitación, deshabilitación y desinstalación mediante el controlador nativo de GeckoView.
 * - Monitoreo del estado y porcentaje de descarga en tiempo real para la interfaz de usuario.
 */
class ExtensionManager(private val context: Context) {

    companion object {
        private const val TAG = "ExtensionManager"

        @Volatile
        private var instance: ExtensionManager? = null

        fun getInstance(context: Context): ExtensionManager {
            return instance ?: synchronized(this) {
                instance ?: ExtensionManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    // Estado observable de extensiones instaladas
    private val _installedExtensions = MutableStateFlow<List<WebExtensionModel>>(emptyList())
    val installedExtensions: StateFlow<List<WebExtensionModel>> = _installedExtensions.asStateFlow()

    // Progreso de descarga en curso (extensionId -> porcentaje 0.0f .. 1.0f)
    private val _downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, Float>> = _downloadProgress.asStateFlow()

    // Indicador de operaciones en progreso
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        setupPromptDelegate()
    }

    /**
     * Configura el delegado de permisos de GeckoView para autorizar instalaciones
     * que el usuario ha solicitado explícitamente desde la aplicación.
     */
    private fun setupPromptDelegate() {
        try {
            val runtime = GeckoRuntimeProvider.get(context)
            runtime.webExtensionController.promptDelegate = object : WebExtensionController.PromptDelegate {
                override fun onInstallPromptRequest(
                    extension: WebExtension,
                    permissions: Array<String>,
                    origins: Array<String>,
                    dataCollectionPermissions: Array<String>
                ): GeckoResult<WebExtension.PermissionPromptResponse>? {
                    // Autorizar instalación con permisos de navegación estándar y compatibilidad con modo incógnito
                    return GeckoResult.fromValue(
                        WebExtension.PermissionPromptResponse(
                            true, // isPermissionsGranted
                            true, // isPrivateModeGranted
                            true  // isTechnicalAndInteractionDataGranted
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al configurar PromptDelegate para extensiones: ${e.message}", e)
        }
    }

    /**
     * Resuelve la URL oficial y original del icono para una extensión según su ID canónico.
     */
    fun resolveExtensionIconUrl(id: String): String? {
        return when (id) {
            "uBlock0@raymondhill.net" -> "https://raw.githubusercontent.com/gorhill/uBlock/master/src/img/icon_128.png"
            "addon@darkreader.org" -> "https://raw.githubusercontent.com/darkreader/darkreader/master/src/icons/dr_128.png"
            "{036a55b4-5e72-4d05-a9c1-bba0175b86f0}" -> "https://raw.githubusercontent.com/FilipePS/Traduzir-Paginas-Web/master/res/icon-128.png"
            "{74145fec-f68b-474a-8703-00e058d04d40}" -> "https://gitlab.com/KevinRoebert/ClearUrls/-/raw/master/res/img/icon_128.png"
            else -> RecommendedExtension.CATALOG.find { it.id == id }?.iconUrl
        }
    }

    /**
     * Actualiza y sincroniza la lista de extensiones instaladas en GeckoView.
     * Se ejecuta en el hilo principal para GeckoResult y actualiza el StateFlow reactivo.
     */
    suspend fun refreshInstalledExtensions(): List<WebExtensionModel> {
        _isLoading.value = true
        return try {
            val rawList = withContext(Dispatchers.Main) {
                val runtime = GeckoRuntimeProvider.get(context)
                runtime.webExtensionController.list().await() ?: emptyList()
            }

            val models = rawList.map { ext ->
                WebExtensionModel(
                    id = ext.id,
                    name = ext.metaData.name.orEmpty().ifBlank { ext.id },
                    description = ext.metaData.description ?: "",
                    version = ext.metaData.version ?: "1.0",
                    isEnabled = ext.metaData.enabled,
                    isBuiltIn = ext.isBuiltIn,
                    optionsPageUrl = ext.metaData.optionsPageUrl,
                    iconUrl = resolveExtensionIconUrl(ext.id),
                    isAllowedInPrivateBrowsing = ext.metaData.allowedInPrivateBrowsing
                )
            }
            _installedExtensions.value = models
            models
        } catch (e: Exception) {
            Log.e(TAG, "Error al consultar lista de extensiones instaladas: ${e.message}", e)
            _installedExtensions.value = emptyList()
            emptyList()
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Descarga e instala una extensión recomendada desde una URL oficial de Mozilla Add-ons (.xpi).
     * La descarga por red se ejecuta en Dispatchers.IO y la instalación nativa en GeckoView en Dispatchers.Main.
     * 
     * @param extensionId Identificador canónico para seguimiento de progreso.
     * @param downloadUrl Enlace de descarga directo HTTPS.
     * @return Resultado de la instalación como WebExtensionModel o null en caso de error.
     */
    suspend fun downloadAndInstall(extensionId: String, downloadUrl: String): Result<WebExtensionModel> {
        val extensionsDir = File(context.cacheDir, "extensions_cache").apply { mkdirs() }
        val sanitizedFileName = "addon_${System.currentTimeMillis()}_${extensionId.hashCode()}.xpi"
        val targetFile = File(extensionsDir, sanitizedFileName)

        _downloadProgress.value = _downloadProgress.value + (extensionId to 0.05f)

        return try {
            // Fase 1: Descarga por red en Dispatchers.IO
            withContext(Dispatchers.IO) {
                val request = Request.Builder().url(downloadUrl).build()
                val response = httpClient.newCall(request).execute()

                if (!response.isSuccessful) {
                    throw IllegalStateException("Servidor de Mozilla respondió con error HTTP ${response.code}")
                }

                val body = response.body ?: throw IllegalStateException("Respuesta vacía al descargar extensión")
                val totalBytes = body.contentLength()

                body.byteStream().use { input ->
                    FileOutputStream(targetFile).use { output ->
                        val buffer = ByteArray(8 * 1024)
                        var bytesRead: Int
                        var accumulated = 0L

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            accumulated += bytesRead
                            if (totalBytes > 0) {
                                val progress = (accumulated.toFloat() / totalBytes.toFloat()).coerceIn(0.1f, 0.95f)
                                _downloadProgress.value = _downloadProgress.value + (extensionId to progress)
                            }
                        }
                        output.flush()
                    }
                }
            }

            _downloadProgress.value = _downloadProgress.value + (extensionId to 0.98f)

            // Fase 2: Instalación en GeckoView ejecutada en el hilo principal (Dispatchers.Main con Looper activo)
            val installedExt = withContext(Dispatchers.Main) {
                val runtime = GeckoRuntimeProvider.get(context)
                val fileUri = Uri.fromFile(targetFile).toString()
                runtime.webExtensionController.install(fileUri).await()
                    ?: throw IllegalStateException("GeckoView no pudo inicializar la extensión instalada")
            }

            // Autorizar uso en navegación privada
            withContext(Dispatchers.Main) {
                try {
                    val runtime = GeckoRuntimeProvider.get(context)
                    runtime.webExtensionController.setAllowedInPrivateBrowsing(installedExt, true).await()
                } catch (_: Exception) {}
            }

            // Refrescar lista local
            refreshInstalledExtensions()

            val model = WebExtensionModel(
                id = installedExt.id,
                name = installedExt.metaData.name.orEmpty().ifBlank { installedExt.id },
                description = installedExt.metaData.description ?: "",
                version = installedExt.metaData.version ?: "1.0",
                isEnabled = installedExt.metaData.enabled,
                isBuiltIn = installedExt.isBuiltIn,
                optionsPageUrl = installedExt.metaData.optionsPageUrl,
                iconUrl = resolveExtensionIconUrl(installedExt.id),
                isAllowedInPrivateBrowsing = installedExt.metaData.allowedInPrivateBrowsing
            )

            Result.success(model)
        } catch (e: Exception) {
            Log.e(TAG, "Fallo durante la descarga o instalación de la extensión $extensionId: ${e.message}", e)
            Result.failure(e)
        } finally {
            // Limpieza del archivo .xpi temporal
            if (targetFile.exists()) {
                try { targetFile.delete() } catch (_: Exception) {}
            }
            _downloadProgress.value = _downloadProgress.value - extensionId
        }
    }

    /**
     * Alterna la activación (encendido/apagado) de una extensión instalada.
     */
    suspend fun setExtensionEnabled(extensionId: String, enabled: Boolean): Result<Unit> {
        return try {
            withContext(Dispatchers.Main) {
                val runtime = GeckoRuntimeProvider.get(context)
                val rawList = runtime.webExtensionController.list().await() ?: emptyList()
                val target = rawList.find { it.id == extensionId }
                    ?: throw IllegalArgumentException("Extensión no encontrada")

                if (enabled) {
                    runtime.webExtensionController.enable(target, WebExtensionController.EnableSource.USER).await()
                } else {
                    runtime.webExtensionController.disable(target, WebExtensionController.EnableSource.USER).await()
                }
            }

            refreshInstalledExtensions()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error al cambiar estado de la extensión $extensionId: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Desinstala completamente una extensión de GeckoView.
     */
    suspend fun uninstallExtension(extensionId: String): Result<Unit> {
        return try {
            withContext(Dispatchers.Main) {
                val runtime = GeckoRuntimeProvider.get(context)
                val rawList = runtime.webExtensionController.list().await() ?: emptyList()
                val target = rawList.find { it.id == extensionId }
                    ?: throw IllegalArgumentException("Extensión no encontrada")

                runtime.webExtensionController.uninstall(target).await()
            }

            refreshInstalledExtensions()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error al desinstalar la extensión $extensionId: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Función utilitaria de suspensión para convertir GeckoResult a Coroutines Kotlin.
     * Garantiza ejecución en Dispatchers.Main para disponer de Looper y evitar el fallo "Must have a Handler".
     */
    private suspend fun <T> GeckoResult<T>.await(): T? = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { cont ->
            this@await.accept(
                { value -> if (cont.isActive) cont.resume(value) },
                { error -> if (cont.isActive) cont.resumeWithException(error ?: Exception("Error en GeckoResult")) }
            )
        }
    }
}

package com.example.viewmodel.delegates

import android.app.Application
import com.example.browser.extension.ExtensionManager
import com.example.browser.extension.RecommendedExtension
import com.example.browser.extension.WebExtensionModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Delegado de gestión de extensiones WebExtension para Mozilla GeckoView.
 * 
 * Responsabilidades:
 * - Gestionar la lista de complementos instalados y su estado de activación.
 * - Coordinar descargas directas desde el catálogo oficial de Mozilla Add-ons (AMO).
 * - Proveer instalación segura de paquetes .xpi mediante URLs directas HTTPS.
 * - Desinstalar complementos y notificar al motor GeckoView.
 */
class ExtensionDelegate(
    application: Application,
    private val scope: CoroutineScope
) {
    val extensionManager: ExtensionManager = ExtensionManager.getInstance(application)

    val installedExtensions: StateFlow<List<WebExtensionModel>> = extensionManager.installedExtensions
    val extensionDownloadProgress: StateFlow<Map<String, Float>> = extensionManager.downloadProgress
    val isExtensionsLoading: StateFlow<Boolean> = extensionManager.isLoading
    val recommendedExtensions: List<RecommendedExtension> = RecommendedExtension.CATALOG

    /**
     * Refresca la lista de extensiones instaladas en GeckoView.
     */
    fun refreshInstalledExtensions() {
        scope.launch {
            extensionManager.refreshInstalledExtensions()
        }
    }

    /**
     * Descarga e instala una extensión del catálogo oficial de Mozilla Add-ons.
     */
    fun downloadAndInstallExtension(
        recommended: RecommendedExtension,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        scope.launch {
            val result = extensionManager.downloadAndInstall(recommended.id, recommended.downloadUrl)
            result.onSuccess {
                onSuccess()
            }.onFailure { error ->
                onError(error.localizedMessage ?: "Error al instalar la extensión")
            }
        }
    }

    /**
     * Descarga e instala una extensión desde un enlace directo seguro HTTPS (.xpi).
     */
    fun installCustomExtension(
        downloadUrl: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        scope.launch {
            val trimmed = downloadUrl.trim()
            if (trimmed.isBlank() || !trimmed.startsWith("https://", ignoreCase = true)) {
                onError("Por seguridad, el enlace debe iniciar con https://")
                return@launch
            }
            val fakeId = "custom_${System.currentTimeMillis()}"
            val result = extensionManager.downloadAndInstall(fakeId, trimmed)
            result.onSuccess {
                onSuccess()
            }.onFailure { error ->
                onError(error.localizedMessage ?: "No se pudo descargar la extensión")
            }
        }
    }

    /**
     * Habilita o deshabilita una extensión instalada en GeckoView.
     */
    fun toggleExtensionEnabled(extensionId: String, enabled: Boolean) {
        scope.launch {
            extensionManager.setExtensionEnabled(extensionId, enabled)
        }
    }

    /**
     * Desinstala una extensión instalada en GeckoView.
     */
    fun uninstallExtension(extensionId: String) {
        scope.launch {
            extensionManager.uninstallExtension(extensionId)
        }
    }
}

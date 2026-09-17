package com.example.viewmodel.delegates

import com.example.data.local.entity.SitePermissionEntity
import com.example.data.repository.BrowserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Delegado de gestión de permisos por sitio web y políticas de bloqueo silencioso.
 * 
 * Responsabilidades:
 * - Consultar y persistir reglas de permisos (Cámara, Micrófono, Ubicación, Notificaciones).
 * - Controlar opciones globales de "No Preguntar" (bloqueo silencioso).
 */
class SitePermissionDelegate(
    private val repository: BrowserRepository,
    private val scope: CoroutineScope
) {
    val allSitePermissions: StateFlow<List<SitePermissionEntity>> = repository.getAllSitePermissions()
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    val blockNotificationPrompts: StateFlow<Boolean> = repository.blockNotificationPrompts
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), false)

    val blockLocationPrompts: StateFlow<Boolean> = repository.blockLocationPrompts
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), false)

    val blockMediaPrompts: StateFlow<Boolean> = repository.blockMediaPrompts
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), false)

    fun saveSitePermission(origin: String, permissionType: String, status: String) {
        scope.launch {
            repository.saveSitePermission(origin, permissionType, status)
        }
    }

    fun updateSitePermissionStatus(id: Long, status: String) {
        scope.launch {
            repository.updateSitePermissionStatus(id, status)
        }
    }

    fun deleteSitePermission(id: Long) {
        scope.launch {
            repository.deleteSitePermission(id)
        }
    }

    fun deletePermissionsForOrigin(origin: String) {
        scope.launch {
            repository.deleteSitePermissionsForOrigin(origin)
        }
    }

    fun clearAllSitePermissions() {
        scope.launch {
            repository.clearAllSitePermissions()
        }
    }

    @androidx.annotation.WorkerThread
    fun findSitePermissionSync(origin: String, permissionType: String): SitePermissionEntity? {
        return repository.findSitePermissionSync(origin, permissionType)
    }

    suspend fun findSitePermission(origin: String, permissionType: String): SitePermissionEntity? {
        return repository.findSitePermission(origin, permissionType)
    }

    fun setBlockNotificationPrompts(enabled: Boolean) {
        scope.launch { repository.setBlockNotificationPrompts(enabled) }
    }

    fun setBlockLocationPrompts(enabled: Boolean) {
        scope.launch { repository.setBlockLocationPrompts(enabled) }
    }

    fun setBlockMediaPrompts(enabled: Boolean) {
        scope.launch { repository.setBlockMediaPrompts(enabled) }
    }
}

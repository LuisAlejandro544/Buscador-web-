package com.example.ui.permissions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.SitePermissionEntity
import com.example.viewmodel.BrowserViewModel

/**
 * Pantalla de Gestión de Permisos por Sitio Web y Políticas de Bloqueo Silencioso.
 * 
 * Permite a los usuarios inspeccionar granularmente los accesos concedidos o bloqueados
 * para cada origen web (cámara, micrófono, ubicación, notificaciones y almacenamiento),
 * revocarlos de forma inmediata, y activar políticas automáticas de "No Preguntar".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SitePermissionsScreen(
    viewModel: BrowserViewModel,
    onNavigateBack: () -> Unit
) {
    val sitePermissions by viewModel.allSitePermissions.collectAsState()
    val blockNotifications by viewModel.blockNotificationPrompts.collectAsState()
    val blockLocation by viewModel.blockLocationPrompts.collectAsState()
    val blockMedia by viewModel.blockMediaPrompts.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showClearAllDialog by remember { mutableStateOf(false) }
    var siteToDelete by remember { mutableStateOf<String?>(null) }

    // Filtrar permisos según la búsqueda
    val filteredPermissions = remember(sitePermissions, searchQuery) {
        if (searchQuery.isBlank()) {
            sitePermissions
        } else {
            sitePermissions.filter { it.origin.contains(searchQuery.trim(), ignoreCase = true) }
        }
    }

    // Agrupar permisos por origen/sitio web
    val groupedBySite = remember(filteredPermissions) {
        filteredPermissions.groupBy { it.origin }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Permisos por Sitio Web",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("site_perm_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Regresar a configuración"
                        )
                    }
                },
                actions = {
                    if (sitePermissions.isNotEmpty()) {
                        IconButton(
                            onClick = { showClearAllDialog = true },
                            modifier = Modifier.testTag("site_perm_clear_all_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Limpiar todos los permisos",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Sección de Políticas de Bloqueo Silencioso ("No Preguntar")
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Políticas de Bloqueo Silencioso",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Configura qué solicitudes de permisos deben rechazarse automáticamente sin mostrar ventanas emergentes de confirmación.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(14.dp))

                        // Switch: Notificaciones
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Silenciar solicitudes de Notificaciones",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Las webs no podrán pedir permiso para enviarte alertas.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Switch(
                                checked = blockNotifications,
                                onCheckedChange = { viewModel.setBlockNotificationPrompts(it) },
                                modifier = Modifier.testTag("switch_block_notification_prompts")
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Switch: Ubicación
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Silenciar solicitudes de Ubicación",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Deniega automáticamente el acceso a tu posición GPS.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Switch(
                                checked = blockLocation,
                                onCheckedChange = { viewModel.setBlockLocationPrompts(it) },
                                modifier = Modifier.testTag("switch_block_location_prompts")
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Switch: Cámara y Micrófono
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Silenciar Cámara y Micrófono",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Bloquea cualquier acceso a dispositivos multimedia sin preguntar.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Switch(
                                checked = blockMedia,
                                onCheckedChange = { viewModel.setBlockMediaPrompts(it) },
                                modifier = Modifier.testTag("switch_block_media_prompts")
                            )
                        }
                    }
                }
            }

            // 2. Buscador de Sitios Web
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sitios con permisos (${groupedBySite.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar por dominio (ej: google.com)...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Borrar búsqueda")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("site_perm_search_field"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // 3. Estado Vacío si no hay sitios configurados
            if (groupedBySite.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (searchQuery.isBlank()) "Sin permisos registrados" else "No se encontraron coincidencias",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (searchQuery.isBlank()) {
                                    "Cuando una página web te pida acceso a la cámara, micrófono o ubicación y respondas al diálogo, podrás visualizarla y revocar sus permisos desde aquí."
                                } else {
                                    "No hay ningún sitio configurado que coincida con \"$searchQuery\"."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                // 4. Lista de Sitios con sus permisos
                items(groupedBySite.keys.toList(), key = { it }) { origin ->
                    val sitePermissionsList = groupedBySite[origin] ?: emptyList()
                    SitePermissionCard(
                        origin = origin,
                        permissions = sitePermissionsList,
                        onToggleStatus = { permId, currentStatus ->
                            val newStatus = if (currentStatus == SitePermissionEntity.STATUS_GRANTED) {
                                SitePermissionEntity.STATUS_DENIED
                            } else {
                                SitePermissionEntity.STATUS_GRANTED
                            }
                            viewModel.updateSitePermissionStatus(permId, newStatus)
                        },
                        onDeletePermission = { permId ->
                            viewModel.deleteSitePermission(permId)
                        },
                        onDeleteSite = {
                            siteToDelete = origin
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Diálogo para limpiar todos los permisos
    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text("¿Restablecer todos los permisos?")
            },
            text = {
                Text("Se eliminarán todas las reglas de permisos otorgadas o bloqueadas para todos los sitios web. Las páginas volverán a solicitar permisos cuando los requieran.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllSitePermissions()
                        showClearAllDialog = false
                    },
                    modifier = Modifier.testTag("dialog_confirm_clear_all_permissions")
                ) {
                    Text("Restablecer", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogo para eliminar todos los permisos de un sitio web específico
    siteToDelete?.let { origin ->
        AlertDialog(
            onDismissRequest = { siteToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text("¿Revocar permisos de este sitio?")
            },
            text = {
                Text("Se revocarán todos los permisos guardados para \"$origin\". La página volverá a preguntar si necesita acceder a tus dispositivos.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deletePermissionsForOrigin(origin)
                        siteToDelete = null
                    },
                    modifier = Modifier.testTag("dialog_confirm_delete_site_permissions")
                ) {
                    Text("Revocar", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { siteToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

/**
 * Tarjeta que representa un sitio web y la lista de permisos que tiene asignados.
 */
@Composable
private fun SitePermissionCard(
    origin: String,
    permissions: List<SitePermissionEntity>,
    onToggleStatus: (permId: Long, currentStatus: String) -> Unit,
    onDeletePermission: (permId: Long) -> Unit,
    onDeleteSite: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("site_card_$origin"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Encabezado del Sitio Web
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = origin,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${permissions.size} permiso(s) configurado(s)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onDeleteSite,
                    modifier = Modifier.testTag("btn_delete_site_$origin")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Revocar todos los permisos de $origin",
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(10.dp))

            // Lista individual de permisos del sitio
            permissions.forEach { perm ->
                PermissionItemRow(
                    permission = perm,
                    onToggleStatus = { onToggleStatus(perm.id, perm.status) },
                    onDelete = { onDeletePermission(perm.id) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

/**
 * Fila individual para un permiso específico dentro de una tarjeta de sitio.
 */
@Composable
private fun PermissionItemRow(
    permission: SitePermissionEntity,
    onToggleStatus: () -> Unit,
    onDelete: () -> Unit
) {
    val (icon, label) = getPermissionInfo(permission.permissionType)
    val isGranted = permission.status == SitePermissionEntity.STATUS_GRANTED

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )

        // Chip interactivo para cambiar entre Permitido y Bloqueado
        FilterChip(
            selected = isGranted,
            onClick = onToggleStatus,
            label = {
                Text(
                    text = if (isGranted) "Permitido" else "Bloqueado",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Block,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                labelColor = MaterialTheme.colorScheme.error
            ),
            modifier = Modifier.testTag("chip_perm_${permission.id}")
        )

        Spacer(modifier = Modifier.width(6.dp))

        // Botón para eliminar y hacer que el sitio vuelva a preguntar
        IconButton(
            onClick = onDelete,
            modifier = Modifier
                .size(36.dp)
                .testTag("btn_remove_perm_${permission.id}")
        ) {
            Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = "Eliminar permiso $label",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}

/**
 * Retorna el icono y el texto en español para cada tipo de permiso.
 */
private fun getPermissionInfo(type: String): Pair<ImageVector, String> {
    return when (type) {
        SitePermissionEntity.PERMISSION_MICROPHONE -> Icons.Default.Mic to "Micrófono"
        SitePermissionEntity.PERMISSION_CAMERA -> Icons.Default.Videocam to "Cámara"
        SitePermissionEntity.PERMISSION_GEOLOCATION -> Icons.Default.LocationOn to "Ubicación"
        SitePermissionEntity.PERMISSION_NOTIFICATION -> Icons.Default.Notifications to "Notificaciones"
        SitePermissionEntity.PERMISSION_STORAGE -> Icons.Default.Storage to "Almacenamiento"
        else -> Icons.Default.Security to type
    }
}

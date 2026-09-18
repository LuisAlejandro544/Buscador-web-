package com.example.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.viewmodel.BrowserViewModel
import kotlinx.coroutines.launch

/**
 * Pantalla principal de Configuración y Ajustes del Navegador.
 * 
 * Modularizada en secciones especializadas:
 * - [SettingsGeneralSection]: Motores de búsqueda, modo de escritorio y página de inicio.
 * - [SettingsShortcutsSection]: Accesos directos a Cookies, Cuentas, Permisos y Extensiones.
 * - [SettingsPrivacySection]: Políticas de JavaScript, cookies, Do Not Track y permisos silenciosos.
 * - [SettingsSoundSection]: Efectos de sonido hápticos y de navegación.
 * - [SettingsArchitectureSection]: Diagnóstico de arquitectura del motor nativo (32/64 bits).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: BrowserViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToCookies: () -> Unit = {},
    onNavigateToAccounts: () -> Unit = {},
    onNavigateToSitePermissions: () -> Unit = {},
    onNavigateToExtensions: () -> Unit = {},
    onNavigateToSecurityThreats: () -> Unit = {},
    onNavigateToCrashInspector: () -> Unit = {},
    onNavigateToPrivacyAuditor: () -> Unit = {}
) {
    val searchEngine by viewModel.searchEngine.collectAsState()
    val homePageUrl by viewModel.homePageUrl.collectAsState()
    val isDesktopModeDefault by viewModel.isDesktopModeDefault.collectAsState()
    val isJavaScriptEnabled by viewModel.isJavaScriptEnabled.collectAsState()
    val isCookiesEnabled by viewModel.isCookiesEnabled.collectAsState()
    val isDoNotTrackEnabled by viewModel.isDoNotTrackEnabled.collectAsState()
    val isSoundEffectsEnabled by viewModel.isSoundEffectsEnabled.collectAsState()

    val blockNotificationPrompts by viewModel.blockNotificationPrompts.collectAsState()
    val blockLocationPrompts by viewModel.blockLocationPrompts.collectAsState()
    val blockMediaPrompts by viewModel.blockMediaPrompts.collectAsState()

    val isFilterEnabled by viewModel.isFilterEnabled.collectAsState()
    val filterRulesCount by viewModel.filterRulesCount.collectAsState()
    val blockedRequestsCount by viewModel.blockedRequestsCount.collectAsState()
    val isUpdatingFilterRules by viewModel.isUpdatingFilterRules.collectAsState()
    val lastFilterUpdateMessage by viewModel.lastFilterUpdateMessage.collectAsState()

    var showClearDataConfirmation by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes del Navegador", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("settings_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sección General
            SettingsGeneralSection(
                currentSearchEngine = searchEngine,
                homePageUrl = homePageUrl,
                isDesktopModeDefault = isDesktopModeDefault,
                onSearchEngineChange = { viewModel.setSearchEngine(it) },
                onHomePageUrlChange = { viewModel.setHomePageUrl(it) },
                onDesktopModeDefaultChange = { viewModel.setDesktopModeDefault(it) }
            )

            // Sección de Accesos Directos a Pantallas Especializadas
            SettingsShortcutsSection(
                onNavigateToCookies = onNavigateToCookies,
                onNavigateToAccounts = onNavigateToAccounts,
                onNavigateToSitePermissions = onNavigateToSitePermissions,
                onNavigateToExtensions = onNavigateToExtensions,
                onNavigateToSecurityThreats = onNavigateToSecurityThreats,
                onNavigateToCrashInspector = onNavigateToCrashInspector,
                onNavigateToPrivacyAuditor = onNavigateToPrivacyAuditor
            )

            // Sección de Escudo de Filtrado Nativo (Rust core-native adblock)
            SettingsFilterEngineSection(
                isFilterEnabled = isFilterEnabled,
                rulesCount = filterRulesCount,
                blockedRequestsCount = blockedRequestsCount,
                isUpdatingRules = isUpdatingFilterRules,
                lastUpdateMessage = lastFilterUpdateMessage,
                onFilterToggle = { viewModel.toggleFilterShield(it) },
                onUpdateRulesClick = { viewModel.updateFilterRulesFromRemote() },
                onAddCustomRule = { viewModel.addCustomFilterRule(it) }
            )

            // Sección de Privacidad y Seguridad
            SettingsPrivacySection(
                isJavaScriptEnabled = isJavaScriptEnabled,
                isCookiesEnabled = isCookiesEnabled,
                isDoNotTrackEnabled = isDoNotTrackEnabled,
                blockNotificationPrompts = blockNotificationPrompts,
                blockLocationPrompts = blockLocationPrompts,
                blockMediaPrompts = blockMediaPrompts,
                onJavaScriptToggle = { viewModel.setJavaScriptEnabled(it) },
                onCookiesToggle = { viewModel.setCookiesEnabled(it) },
                onDoNotTrackToggle = { viewModel.setDoNotTrack(it) },
                onBlockNotificationToggle = { viewModel.setBlockNotificationPrompts(it) },
                onBlockLocationToggle = { viewModel.setBlockLocationPrompts(it) },
                onBlockMediaToggle = { viewModel.setBlockMediaPrompts(it) },
                onClearDataClick = { showClearDataConfirmation = true }
            )

            // Sección de Sonido y Efectos
            SettingsSoundSection(
                isSoundEffectsEnabled = isSoundEffectsEnabled,
                onSoundEffectsToggle = { viewModel.setSoundEffectsEnabled(it) }
            )

            // Sección de Información Técnica de Arquitectura
            SettingsArchitectureSection()
        }

        // Diálogo para limpiar todos los datos de navegación
        if (showClearDataConfirmation) {
            AlertDialog(
                onDismissRequest = { showClearDataConfirmation = false },
                icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                title = { Text("Limpiar datos de navegación") },
                text = { Text("¿Deseas vaciar el historial completo, la caché temporal y todas las cookies del motor de navegación?") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.clearBrowsingData()
                            showClearDataConfirmation = false
                            scope.launch {
                                snackbarHostState.showSnackbar("Datos de navegación eliminados correctamente")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.testTag("confirm_clear_browsing_data_btn")
                    ) {
                        Text("Limpiar ahora")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDataConfirmation = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}

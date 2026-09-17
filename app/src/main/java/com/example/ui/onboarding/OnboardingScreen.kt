package com.example.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.browser.extension.RecommendedExtension
import com.example.data.model.SearchEngine
import com.example.viewmodel.BrowserViewModel

/**
 * Pantalla de bienvenida y configuración inicial ("Onboarding").
 * 
 * Permite al usuario en su primera apertura:
 * 1. Seleccionar su motor de búsqueda preferido (Google, DuckDuckGo, Bing, Brave, Ecosia).
 * 2. Elegir qué extensiones oficiales de Mozilla (uBlock Origin, Dark Reader, TWP, ClearURLs)
 *    desea descargar e instalar bajo demanda desde los servidores oficiales de Mozilla (AMO),
 *    sin almacenar archivos binarios con licencias restrictivas dentro del APK.
 */
@Composable
fun OnboardingScreen(
    viewModel: BrowserViewModel,
    onCompleteOnboarding: () -> Unit
) {
    var currentStep by remember { mutableIntStateOf(1) } // 1: Motor de búsqueda, 2: Extensiones
    val currentSearchEngine by viewModel.searchEngine.collectAsStateWithLifecycle()
    val downloadProgressMap by viewModel.extensionDownloadProgress.collectAsStateWithLifecycle()

    var selectedEngine by remember(currentSearchEngine) { mutableStateOf(currentSearchEngine) }
    val selectedExtensions = remember {
        mutableStateListOf<RecommendedExtension>().apply {
            // Por defecto sugerimos uBlock Origin seleccionado para máxima velocidad y protección
            RecommendedExtension.CATALOG.firstOrNull { it.id.contains("ublock", ignoreCase = true) }?.let {
                add(it)
            }
        }
    }

    var isInstallingBatch by remember { mutableStateOf(false) }
    var installStatusMessage by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .testTag("onboarding_screen"),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Cabecera con barra de progreso de pasos
            OnboardingHeader(
                currentStep = currentStep,
                totalSteps = 2
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Contenido según el paso activo
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (currentStep == 1) {
                    SearchEngineSelectionStep(
                        selectedEngine = selectedEngine,
                        onEngineSelected = { engine ->
                            selectedEngine = engine
                            viewModel.selectSearchEngine(engine)
                        }
                    )
                } else {
                    RecommendedExtensionsStep(
                        recommendedList = viewModel.recommendedExtensions,
                        selectedExtensions = selectedExtensions,
                        downloadProgressMap = downloadProgressMap,
                        isInstalling = isInstallingBatch,
                        installStatusMessage = installStatusMessage,
                        onToggleExtension = { ext ->
                            if (selectedExtensions.any { it.id == ext.id }) {
                                selectedExtensions.removeAll { it.id == ext.id }
                            } else {
                                selectedExtensions.add(ext)
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botones inferiores de navegación
            OnboardingBottomActions(
                currentStep = currentStep,
                isInstalling = isInstallingBatch,
                onNextStep = {
                    viewModel.selectSearchEngine(selectedEngine)
                    currentStep = 2
                },
                onSkip = {
                    viewModel.setOnboardingCompleted(true)
                    onCompleteOnboarding()
                },
                onInstallAndFinish = {
                    if (selectedExtensions.isEmpty()) {
                        viewModel.setOnboardingCompleted(true)
                        onCompleteOnboarding()
                    } else {
                        isInstallingBatch = true
                        installStatusMessage = "Conectando con Mozilla Add-ons..."

                        // Instalar secuencialmente las extensiones elegidas
                        val extensionsToInstall = selectedExtensions.toList()
                        var installedCount = 0

                        fun installNext(index: Int) {
                            if (index >= extensionsToInstall.size) {
                                isInstallingBatch = false
                                viewModel.setOnboardingCompleted(true)
                                onCompleteOnboarding()
                                return
                            }
                            val target = extensionsToInstall[index]
                            installStatusMessage = "Descargando ${target.name} (${index + 1}/${extensionsToInstall.size})..."
                            viewModel.downloadAndInstallExtension(
                                recommended = target,
                                onSuccess = {
                                    installedCount++
                                    installNext(index + 1)
                                },
                                onError = {
                                    // Si falla una, continuar con la siguiente para no bloquear al usuario
                                    installNext(index + 1)
                                }
                            )
                        }

                        installNext(0)
                    }
                }
            )
        }
    }
}

/**
 * Cabecera con indicador gráfico de los pasos del onboarding.
 */
@Composable
private fun OnboardingHeader(
    currentStep: Int,
    totalSteps: Int
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Bienvenido a tu Navegador",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Paso $currentStep de $totalSteps",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LinearProgressIndicator(
            progress = { currentStep.toFloat() / totalSteps.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

/**
 * Paso 1: Selección interactiva del motor de búsqueda preferido.
 */
@Composable
private fun SearchEngineSelectionStep(
    selectedEngine: SearchEngine,
    onEngineSelected: (SearchEngine) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Elige tu Motor de Búsqueda",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Selecciona el proveedor con el que deseas buscar en la web. Podrás cambiarlo cuando desees en los Ajustes.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(SearchEngine.entries) { engine ->
                val isSelected = engine == selectedEngine
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEngineSelected(engine) }
                        .testTag("engine_option_${engine.name.lowercase()}"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    ),
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = engine.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = getEngineDescription(engine),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Icon(
                            imageVector = if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}

/**
 * Paso 2: Catálogo de extensiones recomendadas oficiales para descargar desde Mozilla Add-ons (AMO).
 */
@Composable
private fun RecommendedExtensionsStep(
    recommendedList: List<RecommendedExtension>,
    selectedExtensions: List<RecommendedExtension>,
    downloadProgressMap: Map<String, Float>,
    isInstalling: Boolean,
    installStatusMessage: String,
    onToggleExtension: (RecommendedExtension) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Extensiones Oficiales de Firefox",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Descarga extensiones recomendadas directamente desde Mozilla Add-ons. Se descargan de forma segura bajo demanda sin aumentar el tamaño de la app.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )

        AnimatedVisibility(
            visible = isInstalling,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = installStatusMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(recommendedList) { extension ->
                val isSelected = selectedExtensions.any { it.id == extension.id }
                val progress = downloadProgressMap[extension.id]

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(enabled = !isInstalling) { onToggleExtension(extension) }
                        .testTag("extension_card_${extension.id}"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) {
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    ),
                    border = BorderStroke(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = getExtensionIcon(extension.iconType),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = extension.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = extension.category,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { onToggleExtension(extension) },
                                enabled = !isInstalling,
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = extension.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Barra de progreso si está descargando esta extensión
                        if (progress != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Barra inferior con botones de avance, instalación y omisión.
 */
@Composable
private fun OnboardingBottomActions(
    currentStep: Int,
    isInstalling: Boolean,
    onNextStep: () -> Unit,
    onSkip: () -> Unit,
    onInstallAndFinish: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (currentStep == 1) {
            OutlinedButton(
                onClick = onSkip,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("onboarding_skip_step1"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Omitir y Entrar")
            }

            Button(
                onClick = onNextStep,
                modifier = Modifier
                    .weight(1.5f)
                    .height(50.dp)
                    .testTag("onboarding_next_step"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Extensiones",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }
        } else {
            OutlinedButton(
                onClick = onSkip,
                enabled = !isInstalling,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("onboarding_skip_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Omitir por ahora")
            }

            Button(
                onClick = onInstallAndFinish,
                enabled = !isInstalling,
                modifier = Modifier
                    .weight(1.5f)
                    .height(50.dp)
                    .testTag("onboarding_install_finish_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isInstalling) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Instalando...")
                } else {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Instalar y Entrar")
                }
            }
        }
    }
}

/**
 * Retorna una descripción clara y breve para cada motor de búsqueda.
 */
private fun getEngineDescription(engine: SearchEngine): String {
    return when (engine) {
        SearchEngine.DUCKDUCKGO -> "Búsquedas privadas sin registro de historial ni perfil publicitario."
        SearchEngine.GOOGLE -> "Resultados amplios y universalmente indexados en la web."
        SearchEngine.BING -> "Búsqueda completa con el ecosistema de servicios de Microsoft."
        SearchEngine.BRAVE -> "Índice de búsqueda independiente enfocado en privacidad."
        SearchEngine.ECOSIA -> "Dedicado a la reforestación y sostenibilidad ambiental."
    }
}

/**
 * Mapeo de icono vector según el tipo conceptual de extensión.
 */
private fun getExtensionIcon(iconType: String): ImageVector {
    return when (iconType) {
        "SHIELD" -> Icons.Default.Shield
        "DARK_MODE" -> Icons.Default.DarkMode
        "TRANSLATE" -> Icons.Default.Translate
        "LINK" -> Icons.Default.Link
        else -> Icons.Default.Extension
    }
}

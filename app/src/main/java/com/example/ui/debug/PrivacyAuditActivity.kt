package com.example.ui.debug

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.browser.debug.AuditCategory
import com.example.browser.debug.AuditCheckResult
import com.example.browser.debug.AuditVerdict
import com.example.browser.debug.PrivacyAuditEngine
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

/**
 * Activity independiente para la auditoría forense de privacidad en Modo Incógnito.
 *
 * Funciona como una herramienta de laboratorio desacoplada de la navegación estándar,
 * permitiendo ejecutar pruebas profundas de:
 * 1. Base de datos Room (filtraciones de tablas history, cookies y tabs).
 * 2. Memoria y ciclo de vida de GeckoView.
 * 3. Fugas de red, DoH y WebRTC.
 * 4. Consistencia de huella digital (Resist Fingerprinting).
 * 5. Fugas hacia el Proveedor de Internet (ISP Query Leak Sniffer).
 *
 * Permite examinar los errores y diagnósticos en crudo (RAW) y copiarlos al portapapeles con un toque.
 */
class PrivacyAuditActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                PrivacyAuditScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyAuditScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val auditEngine = remember { PrivacyAuditEngine(context) }

    var testQuery by remember { mutableStateOf("prueba secreta incognito") }
    var isAuditing by remember { mutableStateOf(false) }
    var auditResults by remember { mutableStateOf<List<AuditCheckResult>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf<AuditCategory?>(null) }
    var selectedResultForRaw by remember { mutableStateOf<AuditCheckResult?>(null) }
    var showRawFullReportDialog by remember { mutableStateOf(false) }

    // Ejecutar una auditoría inicial automática al abrir la pantalla
    LaunchedEffect(Unit) {
        isAuditing = true
        auditResults = auditEngine.runFullAudit(testQuery)
        isAuditing = false
    }

    fun copyToClipboard(text: String, label: String = "Informe Forense") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)

        // Respuesta táctil háptica suave
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(35)
            }
        } catch (_: Exception) {}

        Toast.makeText(context, "Copiado al portapapeles", Toast.LENGTH_SHORT).show()
        scope.launch {
            snackbarHostState.showSnackbar("Datos forenses copiados al portapapeles con éxito.")
        }
    }

    val filteredResults = if (selectedCategory == null) {
        auditResults
    } else {
        auditResults.filter { it.category == selectedCategory }
    }

    val passedCount = auditResults.count { it.verdict == AuditVerdict.PASSED }
    val warningCount = auditResults.count { it.verdict == AuditVerdict.WARNING }
    val failedCount = auditResults.count { it.verdict == AuditVerdict.FAILED }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = Color(0xFF00C896),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Privacy Auditor",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Laboratorio Forense de Incógnito",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (auditResults.isNotEmpty()) {
                                showRawFullReportDialog = true
                            }
                        },
                        enabled = auditResults.isNotEmpty()
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Ver y Copiar Reporte Completo en Crudo",
                            tint = if (auditResults.isNotEmpty()) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Panel de Control superior para ingresar término de prueba
            Surface(
                tonalElevation = 2.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Parámetro de Prueba (Búsqueda o Dominio a Auditar):",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = testQuery,
                            onValueChange = { testQuery = it },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("audit_query_input"),
                            placeholder = { Text("Ej: comprar vuelos baratos") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                if (!isAuditing) {
                                    scope.launch {
                                        isAuditing = true
                                        auditResults = auditEngine.runFullAudit(testQuery)
                                        isAuditing = false
                                    }
                                }
                            })
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    isAuditing = true
                                    auditResults = auditEngine.runFullAudit(testQuery)
                                    isAuditing = false
                                }
                            },
                            enabled = !isAuditing,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A896)),
                            modifier = Modifier.testTag("run_audit_button")
                        ) {
                            if (isAuditing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Auditar")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Tarjetas de Métricas de Diagnóstico Rápido
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricBadge(
                            label = "Hermético",
                            count = passedCount,
                            color = Color(0xFF2E7D32),
                            modifier = Modifier.weight(1f)
                        )
                        MetricBadge(
                            label = "Avisos",
                            count = warningCount,
                            color = Color(0xFFED6C02),
                            modifier = Modifier.weight(1f)
                        )
                        MetricBadge(
                            label = "Fugas",
                            count = failedCount,
                            color = Color(0xFFD32F2F),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Barra de Filtros por Categoría con Scroll Horizontal
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { selectedCategory = null },
                    label = { Text("Todas (${auditResults.size})") }
                )
                AuditCategory.values().forEach { category ->
                    val count = auditResults.count { it.category == category }
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text("${category.title.substringBefore(" ")} ($count)") }
                    )
                }
            }

            // Lista de Resultados Forenses
            if (filteredResults.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isAuditing) "Ejecutando suite forense..." else "No hay resultados para mostrar",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredResults, key = { it.id }) { item ->
                        AuditResultCard(
                            result = item,
                            onViewRaw = { selectedResultForRaw = item },
                            onCopy = { copyToClipboard(it, item.testName) }
                        )
                    }
                }
            }
        }
    }

    // Diálogo con detalles forenses crudos individuales (Raw Details)
    selectedResultForRaw?.let { result ->
        AlertDialog(
            onDismissRequest = { selectedResultForRaw = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    VerdictIcon(verdict = result.verdict)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = result.testName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E1E24), shape = RoundedCornerShape(8.dp))
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "DETALLES TÉCNICOS CRUDOS (RAW FORENSIC DATA):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF80DEEA),
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = result.rawDetails,
                        fontSize = 12.sp,
                        color = Color(0xFFECEFF1),
                        fontFamily = FontFamily.Monospace
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val payload = buildString {
                            appendLine("TEST: ${result.testName}")
                            appendLine("ESTADO: ${result.verdict}")
                            appendLine("CATEGORIA: ${result.category.title}")
                            appendLine("RESUMEN: ${result.summary}")
                            appendLine("RAW DETAILS:")
                            appendLine(result.rawDetails)
                        }
                        copyToClipboard(payload, result.testName)
                        selectedResultForRaw = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A896))
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copiar Crudo")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedResultForRaw = null }) {
                    Text("Cerrar")
                }
            }
        )
    }

    // Diálogo con reporte forense general consolidado
    if (showRawFullReportDialog) {
        val fullReport = auditEngine.generateRawReport(auditResults, testQuery)
        AlertDialog(
            onDismissRequest = { showRawFullReportDialog = false },
            title = {
                Text(
                    text = "Informe Forense Consolidado",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF121216), shape = RoundedCornerShape(8.dp))
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = fullReport,
                        fontSize = 11.sp,
                        color = Color(0xFFE0E0E0),
                        fontFamily = FontFamily.Monospace
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        copyToClipboard(fullReport, "Informe Forense Completo")
                        showRawFullReportDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A896))
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copiar Todo")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRawFullReportDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }
}

@Composable
fun MetricBadge(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = color
            )
        }
    }
}

@Composable
fun AuditResultCard(
    result: AuditCheckResult,
    onViewRaw: () -> Unit,
    onCopy: (String) -> Unit
) {
    val borderColor = when (result.verdict) {
        AuditVerdict.PASSED -> Color(0xFF2E7D32)
        AuditVerdict.WARNING -> Color(0xFFED6C02)
        AuditVerdict.FAILED -> Color(0xFFD32F2F)
        AuditVerdict.RUNNING -> Color(0xFF1976D2)
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                VerdictIcon(verdict = result.verdict)
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = result.testName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = result.category.title,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Botón de ver en crudo
                IconButton(onClick = onViewRaw, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "Ver Detalles Crudos",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Botón de copiado directo
                IconButton(
                    onClick = {
                        val textToCopy = "${result.testName} [${result.verdict}]\n${result.summary}\n\nDETALLES:\n${result.rawDetails}"
                        onCopy(textToCopy)
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copiar Resultado",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = result.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Resumen de veredicto
            Surface(
                color = borderColor.copy(alpha = 0.08f),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = result.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = borderColor,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

@Composable
fun VerdictIcon(verdict: AuditVerdict) {
    when (verdict) {
        AuditVerdict.PASSED -> {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Hermético",
                tint = Color(0xFF2E7D32),
                modifier = Modifier.size(22.dp)
            )
        }
        AuditVerdict.WARNING -> {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Aviso",
                tint = Color(0xFFED6C02),
                modifier = Modifier.size(22.dp)
            )
        }
        AuditVerdict.FAILED -> {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = "Fallo",
                tint = Color(0xFFD32F2F),
                modifier = Modifier.size(22.dp)
            )
        }
        AuditVerdict.RUNNING -> {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = Color(0xFF1976D2)
            )
        }
    }
}

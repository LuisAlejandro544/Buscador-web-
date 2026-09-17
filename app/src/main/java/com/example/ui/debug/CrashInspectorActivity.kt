package com.example.ui.debug

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainActivity
import com.example.browser.debug.CrashLogManager
import com.example.browser.debug.CrashReport
import com.example.browser.debug.CrashType
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

/**
 * Actividad independiente de diagnóstico y análisis forense de cierres (Crash Inspector).
 *
 * Registrada como una aplicación adicional en el cajón de aplicaciones del dispositivo móvil
 * para que el desarrollador o usuario pueda inspeccionar exactamente por qué se cerró la app,
 * ver incidentes de GeckoView (OOM), bloqueos ANR y copiar los errores al portapapeles.
 */
class CrashInspectorActivity : ComponentActivity() {

    companion object {
        const val EXTRA_SHOW_LATEST = "extra_show_latest"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                CrashInspectorScreen(
                    onOpenBrowser = {
                        val intent = Intent(this, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                        }
                        startActivity(intent)
                    },
                    onOpenLeakCanary = {
                        openLeakCanary()
                    }
                )
            }
        }
    }

    /**
     * Intenta abrir la pantalla de análisis de fugas de LeakCanary.
     */
    private fun openLeakCanary() {
        try {
            val intent = Intent().apply {
                setClassName(packageName, "leakcanary.internal.activity.LeakActivity")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(
                this,
                "LeakCanary se ejecuta en segundo plano. Monitoreará y notificará automáticamente cuando detecte una fuga.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}

/**
 * Pantalla principal del Crash Inspector en Jetpack Compose.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrashInspectorScreen(
    onOpenBrowser: () -> Unit,
    onOpenLeakCanary: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var reports by remember { mutableStateOf(CrashLogManager.getAllReports(context)) }
    var selectedFilter by remember { mutableStateOf<CrashType?>(null) }
    var reportInDetail by remember { mutableStateOf<CrashReport?>(null) }
    var showClearDialog by remember { mutableStateOf(false) }

    fun refresh() {
        reports = CrashLogManager.getAllReports(context)
    }

    fun copyReport(report: CrashReport) {
        val success = CrashLogManager.copyToClipboard(context, report)
        if (success) {
            vibrateFeedback(context)
            coroutineScope.launch {
                snackbarHostState.showSnackbar("✓ Informe completo copiado al portapapeles")
            }
        }
    }

    fun shareReport(report: CrashReport) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, report.toClipboardText())
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Compartir informe de error")
        context.startActivity(shareIntent)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Crash Inspector", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            "${reports.size} incidente(s) registrado(s)",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onOpenBrowser) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver al Navegador"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenLeakCanary) {
                        Icon(Icons.Default.Memory, contentDescription = "Abrir LeakCanary")
                    }
                    if (reports.isNotEmpty()) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Limpiar Historial")
                        }
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
        ) {
            // Barra de filtros por tipo de incidente
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == null,
                    onClick = { selectedFilter = null },
                    label = { Text("Todos (${reports.size})") }
                )
                FilterChip(
                    selected = selectedFilter == CrashType.UNCAUGHT_EXCEPTION,
                    onClick = { selectedFilter = CrashType.UNCAUGHT_EXCEPTION },
                    label = { Text("Excepciones") }
                )
                FilterChip(
                    selected = selectedFilter == CrashType.GECKO_TAB_CRASH || selectedFilter == CrashType.GECKO_OOM,
                    onClick = {
                        selectedFilter = if (selectedFilter == CrashType.GECKO_TAB_CRASH) null else CrashType.GECKO_TAB_CRASH
                    },
                    label = { Text("GeckoView / OOM") }
                )
                FilterChip(
                    selected = selectedFilter == CrashType.ANR_FREEZE,
                    onClick = { selectedFilter = CrashType.ANR_FREEZE },
                    label = { Text("ANR (Bloqueos)") }
                )
            }

            val filteredReports = remember(reports, selectedFilter) {
                if (selectedFilter == null) {
                    reports
                } else if (selectedFilter == CrashType.GECKO_TAB_CRASH) {
                    reports.filter { it.type == CrashType.GECKO_TAB_CRASH || it.type == CrashType.GECKO_OOM }
                } else {
                    reports.filter { it.type == selectedFilter }
                }
            }

            if (filteredReports.isEmpty()) {
                EmptyReportsView(
                    onSimulateTestCrash = {
                        // Genera un reporte de prueba controlado para que el usuario verifique la herramienta
                        try {
                            throw RuntimeException("Prueba controlada de Crash Inspector (Simulación de fallo en el teléfono)")
                        } catch (t: Throwable) {
                            CrashLogManager.recordException(context, t)
                            refresh()
                        }
                    },
                    onOpenBrowser = onOpenBrowser
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredReports, key = { it.id }) { report ->
                        CrashReportCard(
                            report = report,
                            onCopy = { copyReport(report) },
                            onShare = { shareReport(report) },
                            onShowDetail = { reportInDetail = report }
                        )
                    }
                }
            }
        }
    }

    // Diálogo con detalles forenses y traza completa de la pila
    reportInDetail?.let { report ->
        CrashDetailDialog(
            report = report,
            onDismiss = { reportInDetail = null },
            onCopy = { copyReport(report) },
            onShare = { shareReport(report) }
        )
    }

    // Diálogo de confirmación para vaciar el registro
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("¿Vaciar historial de fallos?") },
            text = { Text("Se eliminarán todos los informes de caídas, ANR y eventos OOM registrados hasta ahora.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        CrashLogManager.clearReports(context)
                        refresh()
                        showClearDialog = false
                    }
                ) {
                    Text("Borrar Todo", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

/**
 * Tarjeta individual que resume un incidente en la lista.
 */
@Composable
fun CrashReportCard(
    report: CrashReport,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onShowDetail: () -> Unit
) {
    val (badgeColor, badgeTextColor, icon) = when (report.type) {
        CrashType.UNCAUGHT_EXCEPTION -> Triple(Color(0xFFFFEBEE), Color(0xFFC62828), Icons.Default.BugReport)
        CrashType.GECKO_TAB_CRASH -> Triple(Color(0xFFFFF3E0), Color(0xFFE65100), Icons.Default.Warning)
        CrashType.GECKO_OOM -> Triple(Color(0xFFF3E5F5), Color(0xFF6A1B9A), Icons.Default.Memory)
        CrashType.ANR_FREEZE -> Triple(Color(0xFFE1F5FE), Color(0xFF0277BD), Icons.Default.HourglassEmpty)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Encabezado de tarjeta: distintivo de tipo y hora
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = badgeColor,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = badgeTextColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            report.type.displayName,
                            color = badgeTextColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    report.formattedDate(),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Título y mensaje del error
            Text(
                report.title,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (report.message.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    report.message,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Metadatos de hardware y URL
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "RAM: ${report.availableRamMb} MB libres / ${report.totalRamMb} MB",
                    fontSize = 11.sp,
                    color = if (report.isLowMemory) Color(0xFFC62828) else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (report.isLowMemory) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    report.deviceModel,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!report.urlAtCrash.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "URL: ${report.urlAtCrash}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Botones de acción rápida
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onCopy,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copiar", fontSize = 13.sp)
                }

                OutlinedButton(
                    onClick = onShowDetail,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Ver Detalle", fontSize = 13.sp)
                }

                IconButton(onClick = onShare) {
                    Icon(Icons.Default.Share, contentDescription = "Compartir")
                }
            }
        }
    }
}

/**
 * Diálogo emergente que muestra el stacktrace completo y metadatos técnicos.
 */
@Composable
fun CrashDetailDialog(
    report: CrashReport,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(report.title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text("• Tipo: ${report.type.displayName}", fontSize = 12.sp)
                Text("• Fecha: ${report.formattedDate()}", fontSize = 12.sp)
                Text("• Dispositivo: ${report.deviceModel}", fontSize = 12.sp)
                Text("• Sistema: ${report.androidVersion}", fontSize = 12.sp)
                Text("• Memoria RAM: ${report.availableRamMb} MB libres / ${report.totalRamMb} MB", fontSize = 12.sp)
                if (report.isLowMemory) {
                    Text("• Estado de Memoria: ¡CRÍTICO (Low RAM activa)!", color = Color(0xFFC62828), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                if (!report.urlAtCrash.isNullOrBlank()) {
                    Text("• URL en curso: ${report.urlAtCrash}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("Traza de Pila (Stack Trace):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        report.stackTrace.ifBlank { "(Sin traza de pila)" },
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onCopy()
                    onDismiss()
                }
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Copiar Todo")
            }
        },
        dismissButton = {
            TextButton(onClick = onShare) {
                Text("Compartir")
            }
        }
    )
}

/**
 * Vista mostrada cuando no hay informes de error registrados.
 */
@Composable
fun EmptyReportsView(
    onSimulateTestCrash: () -> Unit,
    onOpenBrowser: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Check,
            contentDescription = null,
            tint = Color(0xFF2E7D32),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Sin incidentes registrados",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "El navegador funciona con total estabilidad. Si alguna vez ocurre un cierre espontáneo, una caída de GeckoView o un bloqueo ANR, aparecerá aquí con su informe forense completo.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onSimulateTestCrash,
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Simular Error de Prueba")
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = onOpenBrowser,
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Ir al Navegador")
        }
    }
}

/**
 * Genera una vibración táctil corta para retroalimentación en el teléfono móvil.
 */
private fun vibrateFeedback(context: Context) {
    try {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    } catch (_: Exception) {}
}

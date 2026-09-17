package com.example.ui.security

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phishing
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BlockedThreatDetail
import com.example.model.ThreatCategory

/**
 * Pantalla dedicada de bloqueo y advertencia ante amenazas web (Anti-Phishing y Anti-Malware).
 *
 * Se despliega cuando el motor nativo en Rust intercepta una URL clasificada como maliciosa
 * o fraudulenta antes de que cargue ningún contenido en el navegador.
 *
 * Características:
 * - Diseño de alto impacto visual y claro (Rojo carmesí / peligro).
 * - Identificación precisa de la amenaza (URL, dominio, motor reportante, categoría).
 * - Botón de acción principal seguro: "Volver a un lugar seguro" (retroceder o ir a inicio).
 * - Desplegable de detalles técnicos y opción explícita "Ignorar y continuar bajo mi propio riesgo".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreatBlockedScreen(
    threat: BlockedThreatDetail,
    onSafeReturn: () -> Unit,
    onBypassThreat: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showTechnicalDetails by remember { mutableStateOf(false) }

    val dangerRed = Color(0xFFD32F2F)
    val darkCrimsonBg = Color(0xFF1E0A0A)
    val cardSurface = Color(0xFF2C1414)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = darkCrimsonBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Sitio Web Peligroso Bloqueado",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onSafeReturn,
                        modifier = Modifier.testTag("threat_screen_back_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = dangerRed.copy(alpha = 0.9f)
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Escudo de Alerta Circular
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .background(dangerRed.copy(alpha = 0.25f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (threat.category) {
                        ThreatCategory.PHISHING -> Icons.Default.Phishing
                        ThreatCategory.MALWARE -> Icons.Default.Dangerous
                        ThreatCategory.FRAUD -> Icons.Default.ReportProblem
                        ThreatCategory.TRACKER -> Icons.Default.Warning
                    },
                    contentDescription = "Amenaza de Seguridad",
                    tint = dangerRed,
                    modifier = Modifier.size(54.dp)
                )
            }

            // Título de la Amenaza
            Text(
                text = when (threat.category) {
                    ThreatCategory.PHISHING -> "¡Alerta de Suplantación de Identidad!"
                    ThreatCategory.MALWARE -> "¡Alerta de Malware y Troyanos!"
                    ThreatCategory.FRAUD -> "¡Alerta de Fraude Financiero!"
                    ThreatCategory.TRACKER -> "¡Alerta de Rastreo Malicioso!"
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            // Subtítulo con el Dominio
            Text(
                text = "El navegador bloqueó la conexión a \"${threat.domain}\" para proteger tu dispositivo y tus cuentas bancarias.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFFFCDD2),
                textAlign = TextAlign.Center
            )

            // Tarjeta Informativa de la Amenaza
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = cardSurface),
                border = BorderStroke(1.dp, dangerRed.copy(alpha = 0.6f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = dangerRed, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Categoría: ${threat.category.displayName}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Text(
                        text = when (threat.category) {
                            ThreatCategory.PHISHING -> "Este sitio web ha sido reportado por clonar páginas legítimas (bancos, PayPal, redes sociales o tiendas) para robar nombres de usuario, contraseñas y tarjetas de crédito."
                            ThreatCategory.MALWARE -> "Esta URL aloja o distribuye ejecutables maliciosos, troyanos bancarios o exploits automáticos que comprometen tu teléfono."
                            ThreatCategory.FRAUD -> "Dominio clasificado como estafa piramidal, cripto-fraude o servidor de botnet identificado."
                            ThreatCategory.TRACKER -> "Script agresivo de espionaje no autorizado y extracción de huella digital de hardware."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFEF9A9A)
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFFFAB91), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Fuente del reporte: ${threat.threatSource}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFFAB91),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Botón Principal Seguro (Recomendado)
            Button(
                onClick = onSafeReturn,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("safe_return_button")
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Volver a un lugar seguro",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            // Opciones Avanzadas / Ver detalles
            OutlinedButton(
                onClick = { showTechnicalDetails = !showTechnicalDetails },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFEF9A9A)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("toggle_threat_details_button")
            ) {
                Text(
                    text = if (showTechnicalDetails) "Ocultar detalles técnicos" else "Ver detalles técnicos avanzados",
                    color = Color(0xFFFFCDD2)
                )
            }

            AnimatedVisibility(visible = showTechnicalDetails) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF140808)),
                    border = BorderStroke(1.dp, Color(0xFF5A2020))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "URL interceptada:",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.LightGray,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = threat.targetUrl,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFFF8A80),
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Protección ejecutada en el motor nativo de alta velocidad (core-native Rust). Si continúas, tu información personal puede quedar expuesta a los administradores del sitio malicioso.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Botón de escape bajo propio riesgo
                        TextButton(
                            onClick = { onBypassThreat(threat.domain) },
                            modifier = Modifier
                                .align(Alignment.End)
                                .testTag("bypass_threat_button")
                        ) {
                            Text(
                                text = "Ignorar advertencia y visitar el sitio",
                                color = dangerRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

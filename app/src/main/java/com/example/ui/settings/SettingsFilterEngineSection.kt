package com.example.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Sección de configuración e inspección del Motor de Filtrado Nativo en Rust (core-native).
 * 
 * Permite al usuario:
 * - Supervisar el estado del escudo de filtrado en tiempo real.
 * - Inspeccionar la cantidad de reglas de filtrado compiladas en memoria (Rust adblock / ABP).
 * - Monitorear el conteo de elementos publicitarios y rastreadores bloqueados.
 * - Descargar y compilar listas de reglas remotas actualizadas sin reiniciar la app.
 * - Agregar reglas y dominios personalizados al motor nativo.
 */
@Composable
fun SettingsFilterEngineSection(
    isFilterEnabled: Boolean,
    rulesCount: Int,
    blockedRequestsCount: Long,
    isUpdatingRules: Boolean,
    lastUpdateMessage: String?,
    onFilterToggle: (Boolean) -> Unit,
    onUpdateRulesClick: () -> Unit,
    onAddCustomRule: (String) -> Unit
) {
    var customRuleText by remember { mutableStateOf("") }
    var showCustomRuleInput by remember { mutableStateOf(false) }

    val shieldEmerald = Color(0xFF00897B)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("settings_filter_engine_section"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, shieldEmerald.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Cabecera con Icono de Escudo y Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = if (isFilterEnabled) shieldEmerald else Color.Gray,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Escudo de Filtrado Nativo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Motor Rust adblock (core-native)",
                            style = MaterialTheme.typography.bodySmall,
                            color = shieldEmerald,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Switch(
                    checked = isFilterEnabled,
                    onCheckedChange = onFilterToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = shieldEmerald
                    ),
                    modifier = Modifier.testTag("filter_engine_toggle")
                )
            }

            Text(
                text = "Bloquea publicidad invasiva, rastreadores analíticos y scripts de minería directamente en el núcleo nativo de alto rendimiento.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Tarjetas de Métricas en Vivo
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Tarjeta: Reglas Activas
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Rule,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = shieldEmerald
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Reglas activas",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "%,d".format(rulesCount),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = shieldEmerald
                        )
                        Text(
                            text = "Compiladas en memoria",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                // Tarjeta: Peticiones Bloqueadas
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Block,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFFE53935)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Bloqueados",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "%,d".format(blockedRequestsCount),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE53935)
                        )
                        Text(
                            text = "Anuncios y rastreadores",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // Mensaje de estado de última actualización
            lastUpdateMessage?.let { message ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = shieldEmerald
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = shieldEmerald,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Botones de acción: Actualizar listas remotas y Agregar regla
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onUpdateRulesClick,
                    enabled = !isUpdatingRules,
                    colors = ButtonDefaults.buttonColors(containerColor = shieldEmerald),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("update_filter_rules_button")
                ) {
                    if (isUpdatingRules) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Actualizando...", fontSize = 12.sp)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Actualizar listas", fontSize = 12.sp)
                    }
                }

                OutlinedButton(
                    onClick = { showCustomRuleInput = !showCustomRuleInput },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("toggle_custom_rule_input_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Regla manual", fontSize = 12.sp)
                }
            }

            // Campo desplegable para agregar una regla manual
            AnimatedVisibility(visible = showCustomRuleInput) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customRuleText,
                        onValueChange = { customRuleText = it },
                        label = { Text("Regla EasyList o dominio") },
                        placeholder = { Text("Ej: ||doubleclick.net^") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_rule_text_field"),
                        trailingIcon = {
                            if (customRuleText.isNotBlank()) {
                                IconButton(
                                    onClick = {
                                        onAddCustomRule(customRuleText.trim())
                                        customRuleText = ""
                                        showCustomRuleInput = false
                                    },
                                    modifier = Modifier.testTag("submit_custom_rule_button")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Agregar regla", tint = shieldEmerald)
                                }
                            }
                        }
                    )
                    Text(
                        text = "Puedes ingresar patrones en sintaxis Adblock Plus (ABP) o nombres de dominio.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

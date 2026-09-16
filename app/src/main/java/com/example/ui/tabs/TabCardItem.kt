package com.example.ui.tabs

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.TabEntity

/**
 * Componente modular que representa una tarjeta de pestaña en la cuadrícula del navegador.
 * 
 * Incluye:
 * - Renderizado de miniatura real en tiempo de ejecución (captura de dónde dejó el usuario la web).
 * - Indicador visual y atenuación suave cuando la pestaña entra en modo hibernación/reposo tras 5 minutos.
 * - Distintivos de pestañas Protegidas (aislamiento de cookies), Incógnito y Normales.
 * - Acciones táctiles para restaurar/despertar la pestaña o cerrarla liberando memoria.
 */
@Composable
fun TabCardItem(
    tab: TabEntity,
    isActive: Boolean,
    isHibernated: Boolean,
    thumbnail: Bitmap?,
    onClick: () -> Unit,
    onClose: () -> Unit
) {
    val emeraldColor = Color(0xFF00897B)
    val amberSleepColor = Color(0xFFFFA000)

    val borderColor = when {
        isActive && tab.isProtected -> emeraldColor
        isActive -> MaterialTheme.colorScheme.primary
        isHibernated -> amberSleepColor.copy(alpha = 0.7f)
        tab.isProtected -> emeraldColor.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    val containerColor = when {
        isActive && tab.isProtected -> emeraldColor.copy(alpha = 0.15f)
        isActive -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
        tab.isProtected -> emeraldColor.copy(alpha = 0.07f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clickable { onClick() }
            .testTag("tab_card_${tab.id}"),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(if (isActive) 2.dp else 1.dp, borderColor),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
        ) {
            // Barra de título superior de la pestaña
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when {
                        tab.isProtected -> Icons.Default.Shield
                        tab.isIncognito -> Icons.Default.Lock
                        else -> Icons.Default.Language
                    },
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = when {
                        tab.isProtected -> emeraldColor
                        isActive -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (tab.url == "about:home") {
                        when {
                            tab.isProtected -> "Protegida"
                            tab.isIncognito -> "Incógnito"
                            else -> "Inicio"
                        }
                    } else tab.title,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(24.dp)
                        .testTag("close_tab_${tab.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar pestaña",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Área central de Vista Previa (Miniatura de la web donde se dejó)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp)),
                color = MaterialTheme.colorScheme.surface
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (thumbnail != null && !thumbnail.isRecycled) {
                        // Miniatura real capturada de la página web
                        Image(
                            bitmap = thumbnail.asImageBitmap(),
                            contentDescription = "Miniatura de ${tab.title}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // Vista maqueta de respaldo si no hay miniatura o es página interna
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                if (tab.isProtected) {
                                    Text(
                                        text = "🛡️ Contenedor Aislado",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                        color = emeraldColor
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                } else if (tab.isIncognito) {
                                    Text(
                                        text = "🔒 Pestaña Incógnito",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                }
                                Text(
                                    text = if (tab.url == "about:home") "Página de Inicio" else tab.url,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Capa visual si la pestaña está dormida tras 5 minutos de inactividad
                    if (isHibernated) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.45f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                                shadowElevation = 3.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Nightlight,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = amberSleepColor
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "En reposo (5 min)",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Barra inferior con indicadores de estado (Activa / En Reposo)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (isHibernated) {
                    Text(
                        text = "💤 RAM liberada",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = amberSleepColor
                    )
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                if (isActive) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Pestaña activa",
                            tint = if (tab.isProtected) emeraldColor else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Activa",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (tab.isProtected) emeraldColor else MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

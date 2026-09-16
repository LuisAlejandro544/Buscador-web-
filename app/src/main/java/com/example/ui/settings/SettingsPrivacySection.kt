package com.example.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Sección de privacidad, control de scripts y políticas de permisos del navegador.
 */
@Composable
fun SettingsPrivacySection(
    isJavaScriptEnabled: Boolean,
    isCookiesEnabled: Boolean,
    isDoNotTrackEnabled: Boolean,
    blockNotificationPrompts: Boolean,
    blockLocationPrompts: Boolean,
    blockMediaPrompts: Boolean,
    onJavaScriptToggle: (Boolean) -> Unit,
    onCookiesToggle: (Boolean) -> Unit,
    onDoNotTrackToggle: (Boolean) -> Unit,
    onBlockNotificationToggle: (Boolean) -> Unit,
    onBlockLocationToggle: (Boolean) -> Unit,
    onBlockMediaToggle: (Boolean) -> Unit,
    onClearDataClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Privacidad y Seguridad",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(14.dp))

            PrivacyToggleItem(
                icon = Icons.Default.Code,
                title = "Habilitar JavaScript",
                subtitle = "Permite interactividad y contenido dinámico",
                checked = isJavaScriptEnabled,
                onCheckedChange = onJavaScriptToggle,
                testTag = "switch_javascript"
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

            PrivacyToggleItem(
                icon = Icons.Default.Cookie,
                title = "Aceptar cookies web",
                subtitle = "Permite recordar sesiones y preferencias en sitios",
                checked = isCookiesEnabled,
                onCheckedChange = onCookiesToggle,
                testTag = "switch_cookies"
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

            PrivacyToggleItem(
                icon = Icons.Default.Security,
                title = "No Rastrear (Do Not Track)",
                subtitle = "Envía solicitud DNT a todos los servidores web",
                checked = isDoNotTrackEnabled,
                onCheckedChange = onDoNotTrackToggle,
                testTag = "switch_dnt"
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

            Text(
                text = "Políticas Silenciosas (No Preguntar)",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            PrivacyToggleItem(
                icon = Icons.Default.NotificationsOff,
                title = "Bloquear avisos de notificaciones",
                subtitle = "Deniega automáticamente solicitudes de notificaciones emergentes",
                checked = blockNotificationPrompts,
                onCheckedChange = onBlockNotificationToggle,
                testTag = "switch_block_notifications"
            )

            Spacer(modifier = Modifier.height(8.dp))

            PrivacyToggleItem(
                icon = Icons.Default.LocationOff,
                title = "Bloquear avisos de ubicación",
                subtitle = "Deniega acceso automático a GPS y geolocalización",
                checked = blockLocationPrompts,
                onCheckedChange = onBlockLocationToggle,
                testTag = "switch_block_location"
            )

            Spacer(modifier = Modifier.height(8.dp))

            PrivacyToggleItem(
                icon = Icons.Default.MicOff,
                title = "Bloquear avisos de cámara y micrófono",
                subtitle = "Deniega solicitudes automáticas de captura de audio y video",
                checked = blockMediaPrompts,
                onCheckedChange = onBlockMediaToggle,
                testTag = "switch_block_media"
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            Button(
                onClick = onClearDataClick,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_clear_browsing_data"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Limpiar datos de navegación")
            }
        }
    }
}

@Composable
private fun PrivacyToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}

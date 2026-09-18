package com.example.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Sección de accesos directos a pantallas de gestión especializadas del navegador.
 */
@Composable
fun SettingsShortcutsSection(
    onNavigateToCookies: () -> Unit,
    onNavigateToAccounts: () -> Unit,
    onNavigateToSitePermissions: () -> Unit,
    onNavigateToExtensions: () -> Unit,
    onNavigateToSecurityThreats: () -> Unit,
    onNavigateToCrashInspector: () -> Unit = {},
    onNavigateToPrivacyAuditor: () -> Unit = {},
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
                text = "Gestión Avanzada",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(14.dp))

            ShortcutRow(
                icon = Icons.Default.Shield,
                title = "Escudo de Seguridad Web",
                subtitle = "Anti-Phishing y Malware (URLhaus, PhishTank, HaGeZi)",
                onClick = onNavigateToSecurityThreats,
                testTag = "settings_shortcut_security_threats"
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            ShortcutRow(
                icon = Icons.Default.Cookie,
                title = "Cookies y Rastreadores",
                subtitle = "Auditar cookies por dominio y modo protegido",
                onClick = onNavigateToCookies,
                testTag = "settings_shortcut_cookies"
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            ShortcutRow(
                icon = Icons.Default.AccountCircle,
                title = "Cuentas y Acceso Web",
                subtitle = "Credenciales y auto-completado seguro",
                onClick = onNavigateToAccounts,
                testTag = "settings_shortcut_accounts"
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            ShortcutRow(
                icon = Icons.Default.Security,
                title = "Permisos por Sitio",
                subtitle = "Control de geolocalización, cámara y notificaciones",
                onClick = onNavigateToSitePermissions,
                testTag = "settings_shortcut_permissions"
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            ShortcutRow(
                icon = Icons.Default.Extension,
                title = "Extensiones Web (XPI)",
                subtitle = "Gestión e instalación de complementos Mozilla",
                onClick = onNavigateToExtensions,
                testTag = "settings_shortcut_extensions"
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            ShortcutRow(
                icon = Icons.Default.BugReport,
                title = "Crash Inspector & Diagnósticos",
                subtitle = "Historial de cierres, caídas GeckoView, OOM y ANR",
                onClick = onNavigateToCrashInspector,
                testTag = "settings_shortcut_crash_inspector"
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            ShortcutRow(
                icon = Icons.Default.Security,
                title = "Privacy Auditor (Laboratorio Incógnito)",
                subtitle = "Auditoría forense de fugas de BD, memoria, DoH, huella e ISP",
                onClick = onNavigateToPrivacyAuditor,
                testTag = "settings_shortcut_privacy_auditor"
            )
        }
    }
}

@Composable
private fun ShortcutRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

package com.example.browser.security

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

/**
 * Gestor de Notificaciones del Escudo de Seguridad Web.
 *
 * Responsabilidades:
 * - Crear y mantener el canal de notificación exclusivo para actualizaciones de seguridad.
 * - Desplegar notificaciones claras, informativas y no invasivas (IMPORTANCE_LOW) para que
 *   el usuario sepa qué hace la aplicación en segundo plano sin interrumpir su actividad.
 * - Respetar las directivas de privacidad y permisos de Android (POST_NOTIFICATIONS).
 */
object ThreatShieldNotificationHelper {

    const val CHANNEL_ID = "security_threat_updates_channel"
    private const val NOTIFICATION_ID = 2048

    /**
     * Registra el canal de notificaciones en el sistema operativo Android (API 26+).
     * Se configura con baja prioridad para ser informativo y nunca invasivo ni abrumador.
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.threat_shield_notification_channel_name)
            val descriptionText = context.getString(R.string.threat_shield_notification_channel_desc)
            val importance = NotificationManager.IMPORTANCE_LOW

            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Muestra una notificación resumida y agradable sobre la actualización del escudo.
     *
     * @param context Contexto de la aplicación.
     * @param activeFeedsCount Cantidad de motores de ciberseguridad sincronizados.
     * @param totalRulesAdded Cantidad de nuevas reglas o firmas procesadas por el motor nativo Rust.
     */
    fun showUpdateSuccessNotification(
        context: Context,
        activeFeedsCount: Int,
        totalRulesAdded: Int
    ) {
        // Verificar permiso en Android 13+ (Tiramisu)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionGranted = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!permissionGranted) return
        }

        createNotificationChannel(context)

        // Intento para abrir el navegador al pulsar la notificación
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = context.getString(R.string.threat_shield_notif_title)
        val contentText = if (totalRulesAdded > 0) {
            "Se sincronizaron $activeFeedsCount motores de seguridad ($totalRulesAdded nuevas firmas activas)."
        } else {
            "Filtros de estafas y malware al día con $activeFeedsCount fuentes activas."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_shield_check)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                "El escudo web analizó las listas globales contra malware, phishing y fraude financiero. " +
                "Tus sesiones de navegación continúan protegidas en tiempo real sin impacto en tu rendimiento."
            ))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setColor(0xFF00897B.toInt()) // Tono esmeralda de seguridad
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}

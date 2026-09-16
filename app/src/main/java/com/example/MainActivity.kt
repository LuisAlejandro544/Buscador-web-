package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.browser.download.DownloadNotificationHelper
import com.example.browser.lifecycle.AppHibernationManager
import com.example.browser.sound.SoundEffectManager
import com.example.ui.navigation.BrowserNavGraph
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.BrowserViewModel

/**
 * Actividad principal del Navegador Web.
 * Configura la experiencia visual de borde a borde (Edge-to-Edge),
 * maneja los Intents entrantes de enlaces web externos y aloja el grafo de navegación.
 * 
 * Integra:
 * - Solicitud segura de permisos de notificación (Android 13+ / API 33).
 * - Sincronización con [AppHibernationManager] para el ciclo de 20 segundos de ahorro de RAM.
 * - Protección visual FLAG_SECURE en tiempo real para modo incógnito.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: BrowserViewModel by viewModels()

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Si el usuario concede o deniega, el flujo continúa de forma no bloqueante
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Inicializar canales de notificaciones de descargas
        DownloadNotificationHelper.createNotificationChannels(this)

        // Inicializar SoundPool para efectos de sonido nativos (descargas completadas, etc.)
        SoundEffectManager.initialize(this)

        // Solicitar permiso de notificaciones en Android 13+ para avisos nativos
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Procesar enlace si la app fue abierta desde otra aplicación
        handleIncomingIntent(intent)

        setContent {
            val isIncognito by viewModel.isIncognitoMode.collectAsStateWithLifecycle()

            // Protección de pantalla del sistema contra grabadores, capturas y multitarea
            DisposableEffect(isIncognito) {
                if (isIncognito) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
                onDispose {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BrowserNavGraph(viewModel = viewModel)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // El usuario está en la app: cancelar cuenta regresiva de hibernación o despertar el motor
        AppHibernationManager.onAppForegrounded(this)
    }

    override fun onStop() {
        super.onStop()
        // El usuario salió de la app: iniciar cuenta regresiva secreta de 20s para hibernación de RAM
        AppHibernationManager.onAppBackgrounded(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        SoundEffectManager.release()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            intent.dataString?.let { incomingUrl ->
                val trimmedUrl = incomingUrl.trim()
                if (trimmedUrl.isNotBlank()) {
                    val uri = try { android.net.Uri.parse(trimmedUrl) } catch (_: Exception) { null }
                    val scheme = uri?.scheme?.lowercase()
                    // Blindaje crítico: solo permitir protocolos web externos legítimos (http/https)
                    // Bloquea explícitamente file://, content://, javascript:, about:, data:
                    if (scheme == "http" || scheme == "https") {
                        viewModel.createNewTab(url = trimmedUrl, isIncognito = false)
                    }
                }
            }
        }
    }
}

package com.example

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.navigation.BrowserNavGraph
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.BrowserViewModel

/**
 * Actividad principal del Navegador Web.
 * Configura la experiencia visual de borde a borde (Edge-to-Edge),
 * maneja los Intents entrantes de enlaces web externos y aloja el grafo de navegación.
 * 
 * Integra protección visual FLAG_SECURE en tiempo real para modo incógnito:
 * bloquea capturas de pantalla y oculta el contenido sensible en la multitarea de Android.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: BrowserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            intent.dataString?.let { incomingUrl ->
                if (incomingUrl.isNotBlank()) {
                    viewModel.createNewTab(url = incomingUrl, isIncognito = false)
                }
            }
        }
    }
}

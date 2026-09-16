package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.ui.navigation.BrowserNavGraph
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.BrowserViewModel

/**
 * Actividad principal del Navegador Web.
 * Configura la experiencia visual de borde a borde (Edge-to-Edge),
 * maneja los Intents entrantes de enlaces web externos y aloja el grafo de navegación.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: BrowserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Procesar enlace si la app fue abierta desde otra aplicación
        handleIncomingIntent(intent)

        setContent {
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

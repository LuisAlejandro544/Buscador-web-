package com.example.viewmodel.delegates

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.browser.reader.ReaderArticle
import com.example.browser.reader.ReaderExtractor
import com.example.browser.reader.ReaderFont
import com.example.browser.reader.ReaderSettings
import com.example.browser.reader.ReaderTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Delegado responsable del Modo Lectura Nativo y del motor de síntesis de voz (Text-To-Speech).
 * 
 * Capacidades:
 * - Extracción y aislamiento heurístico del artículo web sin distracciones.
 * - Ajuste de tipografía, tamaño de fuente y paleta cromática (Sepia, Blanco, Noche OLED, Carbón).
 * - Lectura en voz alta (Text-To-Speech) mediante la API nativa de Android con controles de reproducción.
 */
class ReaderDelegate(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val _currentArticle = MutableStateFlow<ReaderArticle?>(null)
    val currentArticle: StateFlow<ReaderArticle?> = _currentArticle.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _settings = MutableStateFlow(ReaderSettings())
    val settings: StateFlow<ReaderSettings> = _settings.asStateFlow()

    // Cliente HTTP para descarga de contenido limpio de artículos
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    // Motor de síntesis de voz nativo (TTS)
    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false

    init {
        initTextToSpeech()
    }

    private fun initTextToSpeech() {
        try {
            textToSpeech = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val result = textToSpeech?.setLanguage(Locale("es", "ES"))
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        textToSpeech?.setLanguage(Locale.getDefault())
                    }
                    isTtsInitialized = true
                    setupTtsListener()
                }
            }
        } catch (_: Throwable) {
            isTtsInitialized = false
        }
    }

    private fun setupTtsListener() {
        textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _settings.value = _settings.value.copy(isSpeechPlaying = true)
            }

            override fun onDone(utteranceId: String?) {
                _settings.value = _settings.value.copy(isSpeechPlaying = false)
            }

            override fun onError(utteranceId: String?) {
                _settings.value = _settings.value.copy(isSpeechPlaying = false)
            }
        })
    }

    /**
     * Carga y procesa un artículo a partir de su URL.
     * Si se proporciona el código HTML actual, lo analiza de inmediato sin requerir nueva descarga.
     */
    fun loadArticle(url: String, currentTitle: String = "", pageHtml: String? = null) {
        if (url.isBlank() || url.startsWith("about:")) {
            _errorMessage.value = "No se puede activar el modo lectura en esta página."
            return
        }

        _isLoading.value = true
        _errorMessage.value = null
        stopSpeech()

        scope.launch {
            try {
                val article = withContext(Dispatchers.IO) {
                    if (!pageHtml.isNullOrBlank() && pageHtml.length > 200) {
                        ReaderExtractor.extractFromHtml(pageHtml, url)
                    } else {
                        // Descargar el documento web completo para análisis del DOM
                        val request = Request.Builder()
                            .url(url)
                            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36")
                            .build()

                        httpClient.newCall(request).execute().use { response ->
                            if (response.isSuccessful) {
                                val body = response.body?.string().orEmpty()
                                ReaderExtractor.extractFromHtml(body, url)
                            } else {
                                ReaderExtractor.extractFromHtml("", url)
                            }
                        }
                    }
                }

                _currentArticle.value = if (article.title.isBlank() && currentTitle.isNotBlank()) {
                    article.copy(title = currentTitle)
                } else {
                    article
                }
            } catch (e: Throwable) {
                _errorMessage.value = "Error al procesar el modo lectura: ${e.localizedMessage ?: "Conexión no disponible"}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Alterna la reproducción de lectura en voz alta del artículo.
     */
    fun toggleSpeech() {
        if (!isTtsInitialized || textToSpeech == null) {
            initTextToSpeech()
            return
        }

        val article = _currentArticle.value ?: return

        if (_settings.value.isSpeechPlaying) {
            stopSpeech()
        } else {
            val textToRead = article.plainText.ifBlank { article.title }
            if (textToRead.isNotBlank()) {
                val chunks = textToRead.chunked(3000)
                textToSpeech?.stop()
                chunks.forEachIndexed { index, chunk ->
                    val queueMode = if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
                    textToSpeech?.speak(chunk, queueMode, null, "reader_chunk_$index")
                }
                _settings.value = _settings.value.copy(isSpeechPlaying = true)
            }
        }
    }

    /**
     * Detiene la síntesis de voz activa.
     */
    fun stopSpeech() {
        try {
            textToSpeech?.stop()
        } catch (_: Throwable) {}
        _settings.value = _settings.value.copy(isSpeechPlaying = false)
    }

    /**
     * Modifica el tema visual de lectura.
     */
    fun setTheme(theme: ReaderTheme) {
        _settings.value = _settings.value.copy(theme = theme)
    }

    /**
     * Modifica la fuente tipográfica seleccionada.
     */
    fun setFont(font: ReaderFont) {
        _settings.value = _settings.value.copy(font = font)
    }

    /**
     * Incrementa o reduce el tamaño de fuente (rango: 14sp a 28sp).
     */
    fun adjustFontSize(deltaSp: Float) {
        val newSize = (_settings.value.fontSizeSp + deltaSp).coerceIn(14f, 28f)
        _settings.value = _settings.value.copy(fontSizeSp = newSize)
    }

    /**
     * Libera los recursos del sintetizador de voz.
     */
    fun release() {
        try {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        } catch (_: Throwable) {}
        textToSpeech = null
        isTtsInitialized = false
    }
}

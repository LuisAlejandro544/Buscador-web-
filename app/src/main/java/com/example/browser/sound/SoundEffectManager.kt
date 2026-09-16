package com.example.browser.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.example.R
import com.example.data.preferences.BrowserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Gestor central de efectos de sonido y retroalimentación háptica para el navegador.
 *
 * Utiliza Android SoundPool para reproducción con cero latencia (ideal para efectos cortos
 * en formato PCM WAV), administrando un banco de sonidos aleatorios para eventos de éxito
 * en descargas y acciones de la interfaz.
 */
object SoundEffectManager {

    private const val TAG = "SoundEffectManager"

    // Identificadores de los recursos de sonido en res/raw
    private val DOWNLOAD_SUCCESS_RAW_RES = intArrayOf(
        R.raw.download_success_1,
        R.raw.download_success_2,
        R.raw.download_success_3,
        R.raw.download_success_4,
        R.raw.download_success_5,
        R.raw.download_success_6
    )

    private var soundPool: SoundPool? = null
    private val loadedSoundIds = mutableListOf<Int>()
    private val readySoundIds = mutableSetOf<Int>()
    private var isInitialized = false

    /**
     * Inicializa SoundPool y precarga los audios de la aplicación en memoria.
     */
    @Synchronized
    fun initialize(context: Context) {
        if (isInitialized && soundPool != null) return

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(audioAttributes)
            .build().apply {
                setOnLoadCompleteListener { _, sampleId, status ->
                    if (status == 0) {
                        readySoundIds.add(sampleId)
                    }
                }
            }

        loadedSoundIds.clear()
        readySoundIds.clear()

        for (resId in DOWNLOAD_SUCCESS_RAW_RES) {
            try {
                val soundId = soundPool?.load(context.applicationContext, resId, 1) ?: -1
                if (soundId > 0) {
                    loadedSoundIds.add(soundId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error precargando efecto de sonido resId=$resId", e)
            }
        }

        isInitialized = true
    }

    /**
     * Reproduce de forma aleatoria uno de los tonos de logro cuando una descarga finaliza exitosamente.
     * También activa una vibración háptica sutil de acompañamiento.
     */
    fun playRandomDownloadSuccess(context: Context) {
        val appContext = context.applicationContext

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Verificar si los efectos de sonido están habilitados en los ajustes del usuario
                val preferences = BrowserPreferences(appContext)
                val isEnabled = preferences.isSoundEffectsEnabled.first()
                if (!isEnabled) {
                    return@launch
                }

                // Asegurar inicialización previa
                if (!isInitialized || soundPool == null) {
                    initialize(appContext)
                }

                // Seleccionar un sonido aleatorio de la lista precargada
                val availablePool = if (readySoundIds.isNotEmpty()) readySoundIds.toList() else loadedSoundIds
                if (availablePool.isNotEmpty()) {
                    val randomSoundId = availablePool[Random.nextInt(availablePool.size)]
                    soundPool?.play(randomSoundId, 1.0f, 1.0f, 1, 0, 1.0f)
                }

                // Retroalimentación háptica satisfactoria
                vibrateSuccess(appContext)
            } catch (e: Exception) {
                Log.e(TAG, "No se pudo reproducir el sonido de descarga completada", e)
            }
        }
    }

    /**
     * Ejecuta una pulsación háptica corta de confirmación.
     */
    private fun vibrateSuccess(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vibratorManager?.defaultVibrator
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(35)
                }
            }
        } catch (_: Throwable) {
            // Ignorar excepciones de vibración si el dispositivo no tiene hardware vibrador
        }
    }

    /**
     * Libera los recursos de SoundPool al destruir la aplicación o cuando no se utilicen.
     */
    @Synchronized
    fun release() {
        soundPool?.release()
        soundPool = null
        loadedSoundIds.clear()
        readySoundIds.clear()
        isInitialized = false
    }
}

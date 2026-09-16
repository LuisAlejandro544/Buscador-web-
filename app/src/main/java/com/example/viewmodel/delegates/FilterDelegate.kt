package com.example.viewmodel.delegates

import android.content.Context
import com.example.browser.engine.NativeBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Delegado del Motor de Filtrado de Red Nativo (Rust adblock / core-native).
 * 
 * Responsabilidades:
 * - Supervisar y exponer el estado del escudo de filtrado (activo/inactivo, reglas compiladas, peticiones bloqueadas).
 * - Evaluar solicitudes de red interceptadas para bloquear anuncios, rastreadores y minería de datos.
 * - Actualizar listas de filtros en caliente desde fuentes remotas (EasyList, AdGuard, listas Hosts)
 *   sin requerir reiniciar la aplicación ni recompilar binarios.
 */
class FilterDelegate(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val _isFilterEnabled = MutableStateFlow(true)
    val isFilterEnabled: StateFlow<Boolean> = _isFilterEnabled.asStateFlow()

    private val _rulesCount = MutableStateFlow(14)
    val rulesCount: StateFlow<Int> = _rulesCount.asStateFlow()

    private val _blockedRequestsCount = MutableStateFlow(0L)
    val blockedRequestsCount: StateFlow<Long> = _blockedRequestsCount.asStateFlow()

    private val _isUpdatingRules = MutableStateFlow(false)
    val isUpdatingRules: StateFlow<Boolean> = _isUpdatingRules.asStateFlow()

    private val _lastUpdateMessage = MutableStateFlow<String?>(null)
    val lastUpdateMessage: StateFlow<String?> = _lastUpdateMessage.asStateFlow()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    init {
        refreshStats()
    }

    /**
     * Consulta las estadísticas actuales del motor nativo Rust / JNI.
     */
    fun refreshStats() {
        try {
            val statsJson = NativeBridge.getFilterStats()
            val json = JSONObject(statsJson)
            _rulesCount.value = json.optInt("rules_count", _rulesCount.value)
            _blockedRequestsCount.value = json.optLong("blocked_count", _blockedRequestsCount.value)
        } catch (_: Throwable) {}
    }

    /**
     * Habilita o deshabilita el escudo de filtrado global de la aplicación.
     */
    fun toggleFilter(enabled: Boolean) {
        _isFilterEnabled.value = enabled
    }

    /**
     * Evalúa si una URL de recurso solicitada debe ser bloqueada.
     */
    fun shouldBlockUrl(url: String, sourceUrl: String = "", requestType: String = "other"): Boolean {
        if (!_isFilterEnabled.value) return false

        val blocked = NativeBridge.shouldBlockUrl(url, sourceUrl, requestType)
        if (blocked) {
            _blockedRequestsCount.value += 1
        }
        return blocked
    }

    /**
     * Añade reglas de filtrado manuales en formato EasyList / ABP o dominios Hosts.
     */
    fun addRules(rules: String): Int {
        val total = NativeBridge.addFilterRules(rules)
        _rulesCount.value = total
        refreshStats()
        return total
    }

    /**
     * Descarga y compila en caliente una lista actualizada de reglas de filtrado (por ejemplo, EasyList o lista Hosts).
     */
    fun updateRulesFromRemote(
        listUrl: String = "https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts",
        onComplete: ((Boolean, Int) -> Unit)? = null
    ) {
        if (_isUpdatingRules.value) return

        _isUpdatingRules.value = true
        _lastUpdateMessage.value = "Descargando lista de reglas actualizada..."

        scope.launch {
            try {
                val rulesText = withContext(Dispatchers.IO) {
                    val request = Request.Builder()
                        .url(listUrl)
                        .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) NativeFilter/1.0")
                        .build()

                    httpClient.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            response.body?.string().orEmpty()
                        } else {
                            ""
                        }
                    }
                }

                if (rulesText.isNotBlank()) {
                    val newCount = NativeBridge.addFilterRules(rulesText)
                    _rulesCount.value = newCount
                    refreshStats()
                    _lastUpdateMessage.value = "¡Actualización exitosa! $newCount reglas activas en el motor nativo."
                    onComplete?.invoke(true, newCount)
                } else {
                    _lastUpdateMessage.value = "No se pudieron obtener datos de la fuente remota."
                    onComplete?.invoke(false, _rulesCount.value)
                }
            } catch (e: Throwable) {
                _lastUpdateMessage.value = "Error al actualizar reglas: ${e.localizedMessage ?: "Error de red"}"
                onComplete?.invoke(false, _rulesCount.value)
            } finally {
                _isUpdatingRules.value = false
            }
        }
    }
}

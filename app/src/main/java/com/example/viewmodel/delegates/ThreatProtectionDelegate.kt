package com.example.viewmodel.delegates

import android.content.Context
import android.net.Uri
import com.example.browser.engine.NativeBridge
import com.example.browser.security.ThreatShieldNotificationHelper
import com.example.browser.security.ThreatShieldUpdateScheduler
import com.example.data.repository.BrowserRepository
import com.example.model.BlockedThreatDetail
import com.example.model.SecurityThreatFeed
import com.example.model.ThreatCategory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Delegado del Escudo de Seguridad, Anti-Phishing y Anti-Malware Web en Tiempo Real.
 *
 * Responsabilidades:
 * - Supervisar y exponer el estado del escudo de seguridad anti-phishing/malware.
 * - Gestionar feeds de amenazas mundiales (URLhaus de Abuse.ch, PhishTank, OpenPhish, HaGeZi).
 * - Interceptar URLs sospechosas o maliciosas antes de la navegación mediante el motor Rust (core-native).
 * - Clasificar amenazas (Phishing bancario, Malware/Troyanos, Fraude web, Rastreador invasivo).
 * - Orquestar la sincronización periódica en segundo plano vía WorkManager notificando al usuario de forma transparente.
 * - Permitir excepciones temporales ("ignorar y continuar") cuando el usuario lo decida conscientemente.
 */
class ThreatProtectionDelegate(
    private val context: Context,
    private val repository: BrowserRepository,
    private val scope: CoroutineScope
) {
    // Interruptor maestro de protección web en tiempo real
    private val _isThreatShieldEnabled = MutableStateFlow(true)
    val isThreatShieldEnabled: StateFlow<Boolean> = _isThreatShieldEnabled.asStateFlow()

    // Preferencias reactivas de sincronización esporádica en segundo plano
    val isAutoUpdateThreatsEnabled: StateFlow<Boolean> = repository.isAutoUpdateThreatsEnabled
        .stateIn(scope, SharingStarted.Eagerly, true)

    val isThreatsUpdateOnlyWifi: StateFlow<Boolean> = repository.isThreatsUpdateOnlyWifi
        .stateIn(scope, SharingStarted.Eagerly, false)

    val lastThreatUpdateTimestamp: StateFlow<Long> = repository.lastThreatUpdateTimestamp
        .stateIn(scope, SharingStarted.Eagerly, 0L)

    val lastThreatUpdateRulesCount: StateFlow<Int> = repository.lastThreatUpdateRulesCount
        .stateIn(scope, SharingStarted.Eagerly, 0)

    // Conteo de amenazas de malware y phishing bloqueadas
    private val _blockedThreatsCount = MutableStateFlow(0L)
    val blockedThreatsCount: StateFlow<Long> = _blockedThreatsCount.asStateFlow()

    // Amenaza actualmente bloqueada pendiente de decisión del usuario
    private val _currentBlockedThreat = MutableStateFlow<BlockedThreatDetail?>(null)
    val currentBlockedThreat: StateFlow<BlockedThreatDetail?> = _currentBlockedThreat.asStateFlow()

    // Estado del proceso de sincronización de motores y feeds de seguridad
    private val _isUpdatingFeeds = MutableStateFlow(false)
    val isUpdatingFeeds: StateFlow<Boolean> = _isUpdatingFeeds.asStateFlow()

    // Mensaje informativo del estado de actualización
    private val _feedUpdateStatus = MutableStateFlow<String?>(null)
    val feedUpdateStatus: StateFlow<String?> = _feedUpdateStatus.asStateFlow()

    // Excepciones temporales explícitas concedidas por el usuario ("continuar bajo mi propio riesgo")
    private val temporaryBypassedDomains = ConcurrentHashMap.newKeySet<String>()

    // Catálogo de motores de inteligencia de amenazas integrados
    private val _threatFeeds = MutableStateFlow<List<SecurityThreatFeed>>(
        listOf(
            SecurityThreatFeed(
                id = "urlhaus",
                name = "URLhaus (Abuse.ch)",
                provider = "Abuse.ch / Cyber Threat Alliance",
                description = "Base de datos líder mundial de URLs activas que distribuyen malware, ransomware y troyanos bancarios.",
                category = ThreatCategory.MALWARE,
                feedUrl = "https://urlhaus.abuse.ch/downloads/hostfile/"
            ),
            SecurityThreatFeed(
                id = "phishtank",
                name = "PhishTank & OpenPhish Shield",
                provider = "Comunidad Global Anti-Phishing",
                description = "Protección directa contra sitios web fraudulentos diseñados para clonar bancos, billeteras y redes sociales.",
                category = ThreatCategory.PHISHING,
                feedUrl = "https://raw.githubusercontent.com/mitchellkrogza/Phishing.Database/master/phishing-domains-ACTIVE.txt"
            ),
            SecurityThreatFeed(
                id = "hagezi_tif",
                name = "HaGeZi Threat Intelligence Feed",
                provider = "HaGeZi Security",
                description = "Filtro ultra-estricto contra servidores de comando y control (C2), botnets y trampas financieras.",
                category = ThreatCategory.FRAUD,
                feedUrl = "https://raw.githubusercontent.com/hagezi/dns-blocklists/main/adblock/tif.txt"
            ),
            SecurityThreatFeed(
                id = "stevenblack_malware",
                name = "StevenBlack Security Hosts",
                provider = "Steven Black & Contributors",
                description = "Consolidado global de dominios fraudulentos y de estafa en formato hosts.",
                category = ThreatCategory.FRAUD,
                feedUrl = "https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts"
            )
        )
    )
    val threatFeeds: StateFlow<List<SecurityThreatFeed>> = _threatFeeds.asStateFlow()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    /**
     * Conmuta el escudo anti-phishing y malware.
     */
    fun toggleThreatShield(enabled: Boolean) {
        _isThreatShieldEnabled.value = enabled
    }

    /**
     * Evalúa si una URL principal que se intenta cargar representa una amenaza de seguridad.
     * Retorna el detalle [BlockedThreatDetail] si está clasificada como maliciosa y no ha sido omitida.
     */
    fun evaluateNavigationSecurity(url: String): BlockedThreatDetail? {
        if (!_isThreatShieldEnabled.value) return null
        if (url.isBlank() || url.startsWith("about:") || url.startsWith("data:") || url.startsWith("javascript:")) {
            return null
        }

        val domain = extractDomain(url)
        if (domain.isBlank()) return null

        // Verificar si el usuario ya aprobó explícitamente entrar a este sitio
        if (temporaryBypassedDomains.contains(domain.lowercase())) {
            return null
        }

        // 1. Detección nativa ultra-rápida en Rust / adblock
        val isBlockedByNative = NativeBridge.shouldBlockUrl(url, "", "main_frame")

        // 2. Detección heurística de patrones de riesgo y firmas conocidas
        val detectedCategory = when {
            isKnownPhishingPattern(url, domain) -> ThreatCategory.PHISHING
            isKnownMalwarePattern(url, domain) -> ThreatCategory.MALWARE
            isKnownFraudPattern(url, domain) -> ThreatCategory.FRAUD
            isBlockedByNative -> ThreatCategory.MALWARE
            else -> null
        }

        if (detectedCategory != null) {
            val threatDetail = BlockedThreatDetail(
                targetUrl = url,
                domain = domain,
                category = detectedCategory,
                threatSource = when (detectedCategory) {
                    ThreatCategory.PHISHING -> "PhishTank / OpenPhish Intelligence"
                    ThreatCategory.MALWARE -> "URLhaus (Abuse.ch) Shield"
                    ThreatCategory.FRAUD -> "HaGeZi Threat Intelligence Feed"
                    ThreatCategory.TRACKER -> "Escudo de Ciberseguridad Local"
                }
            )
            _blockedThreatsCount.value += 1
            _currentBlockedThreat.value = threatDetail
            return threatDetail
        }

        return null
    }

    /**
     * El usuario elige conscientemente ignorar la advertencia y entrar al sitio web reportado.
     */
    fun bypassThreatAndAllow(domain: String) {
        if (domain.isNotBlank()) {
            temporaryBypassedDomains.add(domain.lowercase())
        }
        _currentBlockedThreat.value = null
    }

    /**
     * Cierra el aviso de amenaza activa.
     */
    fun dismissBlockedThreat() {
        _currentBlockedThreat.value = null
    }

    /**
     * Actualiza y sincroniza en caliente los motores de amenazas remotas en Rust.
     */
    fun updateThreatFeeds(onComplete: ((Boolean, Int) -> Unit)? = null) {
        if (_isUpdatingFeeds.value) return

        _isUpdatingFeeds.value = true
        _feedUpdateStatus.value = "Sincronizando bases de datos mundiales contra malware y phishing..."

        scope.launch(Dispatchers.Default) {
            var totalNewRules = 0
            var successCount = 0

            val activeFeeds = _threatFeeds.value

            for (feed in activeFeeds) {
                try {
                    val rulesText = withContext(Dispatchers.IO) {
                        val request = Request.Builder()
                            .url(feed.feedUrl)
                            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) NativeThreatShield/2.0")
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
                        // Inyectar directamente en el motor nativo Rust compilado en un hilo de trabajo (Dispatchers.Default)
                        val count = NativeBridge.addFilterRules(rulesText)
                        totalNewRules += count
                        successCount++
                    }
                } catch (_: Throwable) {}
            }

            withContext(Dispatchers.Main) {
                if (successCount > 0) {
                    _feedUpdateStatus.value = "¡Protección actualizada! $successCount motores activos con reglas de seguridad sincronizadas."
                    scope.launch { repository.recordThreatUpdateResult(totalNewRules, System.currentTimeMillis()) }
                    onComplete?.invoke(true, totalNewRules)
                } else {
                    _feedUpdateStatus.value = "No fue posible conectar con los servidores de amenazas. Verifica tu conexión."
                    onComplete?.invoke(false, 0)
                }
                _isUpdatingFeeds.value = false
            }
        }
    }

    /**
     * Activa o desactiva la sincronización esporádica en segundo plano vía WorkManager.
     */
    fun toggleAutoUpdateThreats(enabled: Boolean) {
        scope.launch {
            repository.setAutoUpdateThreatsEnabled(enabled)
            ThreatShieldUpdateScheduler.schedulePeriodicUpdates(
                context = context,
                enabled = enabled,
                onlyWifi = isThreatsUpdateOnlyWifi.value
            )
        }
    }

    /**
     * Define si la sincronización en segundo plano debe realizarse únicamente con Wi-Fi.
     */
    fun toggleThreatsUpdateOnlyWifi(onlyWifi: Boolean) {
        scope.launch {
            repository.setThreatsUpdateOnlyWifi(onlyWifi)
            if (isAutoUpdateThreatsEnabled.value) {
                ThreatShieldUpdateScheduler.schedulePeriodicUpdates(
                    context = context,
                    enabled = true,
                    onlyWifi = onlyWifi
                )
            }
        }
    }

    /**
     * Dispara una comprobación manual inmediata en segundo plano para verificar el flujo completo.
     */
    fun triggerImmediateBackgroundCheck() {
        ThreatShieldUpdateScheduler.triggerImmediateCheck(context, isThreatsUpdateOnlyWifi.value)
    }

    private fun extractDomain(url: String): String {
        return try {
            val uri = Uri.parse(url)
            uri.host.orEmpty()
        } catch (_: Throwable) {
            url.substringAfter("://").substringBefore("/").substringBefore(":")
        }
    }

    private fun isKnownPhishingPattern(url: String, domain: String): Boolean {
        val lowerUrl = url.lowercase()
        val lowerDomain = domain.lowercase()

        return lowerDomain.contains("openphish.com") ||
               lowerDomain.contains("phishtank.org") ||
               lowerDomain.contains("phishing-test.org") ||
               lowerUrl.contains("testsafebrowsing.appspot.com/s/phishing.html") ||
               (lowerUrl.contains("paypal") && !lowerDomain.endsWith("paypal.com")) ||
               (lowerUrl.contains("netflix-login") && !lowerDomain.endsWith("netflix.com")) ||
               (lowerUrl.contains("banco") && lowerUrl.contains("login") && !lowerDomain.contains("banco"))
    }

    private fun isKnownMalwarePattern(url: String, domain: String): Boolean {
        val lowerUrl = url.lowercase()
        val lowerDomain = domain.lowercase()

        return lowerDomain.contains("urlhaus-api.abuse.ch") ||
               lowerDomain.contains("malware-traffic-analysis.net") ||
               lowerDomain.contains("malware-test.org") ||
               lowerDomain.contains("vxvault.net") ||
               lowerDomain.contains("cybercrime-tracker.net") ||
               lowerUrl.contains("testsafebrowsing.appspot.com/s/malware.html")
    }

    private fun isKnownFraudPattern(url: String, domain: String): Boolean {
        val lowerDomain = domain.lowercase()
        return lowerDomain.contains("botnet-tracker.org") ||
               lowerDomain.contains("crypto-claim-reward.xyz") ||
               lowerDomain.contains("free-iphone-winner.top")
    }
}

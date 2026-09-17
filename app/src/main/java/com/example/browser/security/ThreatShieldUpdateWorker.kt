package com.example.browser.security

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.browser.engine.NativeBridge
import com.example.data.preferences.BrowserPreferences
import com.example.model.SecurityThreatFeed
import com.example.model.ThreatCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Worker en segundo plano que actualiza periódicamente las listas de amenazas web
 * (estafas, phishing bancario, malware y sitios fraudulentos).
 *
 * Utiliza WorkManager de Android Jetpack para garantizar ejecución eficiente, respetando
 * restricciones de ahorro de batería y conectividad. Al finalizar con éxito, publica una
 * notificación informativa y transparente al usuario.
 */
class ThreatShieldUpdateWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "ThreatShieldWorker"
        const val WORK_NAME = "threat_shield_periodic_update"

        // Lista de feeds de inteligencia preconfigurados para actualización en segundo plano
        val DEFAULT_SECURITY_FEEDS = listOf(
            SecurityThreatFeed(
                id = "urlhaus_malware",
                name = "URLhaus Malware Blocker",
                provider = "Abuse.ch",
                description = "Base de datos en tiempo real de dominios que distribuyen malware.",
                category = ThreatCategory.MALWARE,
                feedUrl = "https://urlhaus.abuse.ch/downloads/hostfile/"
            ),
            SecurityThreatFeed(
                id = "openphish_community",
                name = "OpenPhish Threat Intelligence",
                provider = "OpenPhish",
                description = "Fuentes comunitarias de webs de suplantación de identidad bancaria y corporativa.",
                category = ThreatCategory.PHISHING,
                feedUrl = "https://raw.githubusercontent.com/mitchellkrogza/Phishing.Database/master/phishing-domains-ACTIVE.txt"
            ),
            SecurityThreatFeed(
                id = "hagezi_tif",
                name = "HaGeZi Threat Intelligence Feed",
                provider = "HaGeZi Security",
                description = "Filtro ultra-estricto contra servidores de comando y control (C2) y trampas financieras.",
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
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val appContext = applicationContext
        val preferences = BrowserPreferences(appContext)

        // 1. Verificar si el usuario mantiene activada la actualización en segundo plano
        if (!preferences.isAutoUpdateThreatsEnabledSync()) {
            Log.i(TAG, "Actualización en segundo plano desactivada por el usuario en preferencias.")
            return@withContext Result.success()
        }

        var successCount = 0
        var totalNewRules = 0

        try {
            for (feed in DEFAULT_SECURITY_FEEDS) {
                try {
                    val request = Request.Builder()
                        .url(feed.feedUrl)
                        .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) NativeThreatShieldWorker/2.0")
                        .build()

                    val rulesText = httpClient.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            response.body?.string().orEmpty()
                        } else {
                            ""
                        }
                    }

                    if (rulesText.isNotBlank()) {
                        // Inyectar en el motor nativo de Rust compilado
                        val added = NativeBridge.addFilterRules(rulesText)
                        totalNewRules += added
                        successCount++
                        Log.d(TAG, "Feed '${feed.name}' sincronizado con éxito. Nuevas reglas: $added")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error descargando feed ${feed.name}: ${e.message}")
                }
            }

            // 2. Registrar métricas de la actualización
            if (successCount > 0) {
                preferences.recordThreatUpdateResultSync(totalNewRules, System.currentTimeMillis())

                // 3. Notificar de forma amigable y no invasiva al usuario
                ThreatShieldNotificationHelper.showUpdateSuccessNotification(
                    context = appContext,
                    activeFeedsCount = successCount,
                    totalRulesAdded = totalNewRules
                )
                Log.i(TAG, "Actualización completada: $successCount motores actualizados, $totalNewRules reglas procesadas.")
                Result.success()
            } else {
                Log.w(TAG, "No se pudo actualizar ningún motor en este intento.")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fallo general en ThreatShieldUpdateWorker", e)
            Result.retry()
        }
    }
}

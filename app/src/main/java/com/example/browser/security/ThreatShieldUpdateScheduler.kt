package com.example.browser.security

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Planificador y orquestador del ciclo de vida de las actualizaciones de seguridad.
 *
 * Configura tareas de WorkManager con directivas de conservación de recursos y batería:
 * - Intervalo periódico de 24 horas para mantener los filtros al día sin fatigar el hardware.
 * - Restricciones de conectividad inteligente (cualquier red o solo redes no medidas / Wi-Fi).
 * - Exige estado de batería no bajo para no afectar la autonomía del dispositivo móvil.
 */
object ThreatShieldUpdateScheduler {

    private const val TAG = "ThreatShieldScheduler"
    private const val PERIODIC_WORK_NAME = ThreatShieldUpdateWorker.WORK_NAME
    private const val IMMEDIATE_WORK_NAME = "threat_shield_one_time_update"

    /**
     * Programa o actualiza la periodicidad de sincronización de amenazas web.
     *
     * @param context Contexto de la aplicación.
     * @param enabled Si está activo el planificador.
     * @param onlyWifi Si las descargas deben limitarse a Wi-Fi (redes sin costo por megabyte).
     * @param intervalHours Intervalo de ejecución en horas (por defecto 24h).
     */
    fun schedulePeriodicUpdates(
        context: Context,
        enabled: Boolean,
        onlyWifi: Boolean = false,
        intervalHours: Long = 24
    ) {
        val workManager = WorkManager.getInstance(context)

        if (!enabled) {
            Log.i(TAG, "Cancelando tareas periódicas de actualización de amenazas web.")
            workManager.cancelUniqueWork(PERIODIC_WORK_NAME)
            return
        }

        val requiredNetwork = if (onlyWifi) {
            NetworkType.UNMETERED
        } else {
            NetworkType.CONNECTED
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(requiredNetwork)
            .setRequiresBatteryNotLow(true)
            .build()

        val periodicWorkRequest = PeriodicWorkRequestBuilder<ThreatShieldUpdateWorker>(
            intervalHours, TimeUnit.HOURS,
            3, TimeUnit.HOURS // Flex period para alinearse con ventanas eficientes del SO
        )
            .setConstraints(constraints)
            .build()

        Log.i(TAG, "Programando actualización periódica de amenazas: cada $intervalHours h (Solo Wi-Fi: $onlyWifi)")

        workManager.enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicWorkRequest
        )
    }

    /**
     * Encola una actualización puntual e inmediata en segundo plano.
     */
    fun triggerImmediateCheck(context: Context, onlyWifi: Boolean = false) {
        val workManager = WorkManager.getInstance(context)

        val requiredNetwork = if (onlyWifi) {
            NetworkType.UNMETERED
        } else {
            NetworkType.CONNECTED
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(requiredNetwork)
            .build()

        val oneTimeRequest = OneTimeWorkRequestBuilder<ThreatShieldUpdateWorker>()
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniqueWork(
            IMMEDIATE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            oneTimeRequest
        )
    }
}

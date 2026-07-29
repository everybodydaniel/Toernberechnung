package com.example.trnberechnung.navigation

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.annotation.MainThread
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.trnberechnung.MainActivity
import com.example.trnberechnung.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

data class VoyageServiceDependencies(
    val locationProvider: LocationProvider,
    val activeVoyageManager: ActiveVoyageManager,
)

/**
 * Implement this on the Application-level dependency container. The service
 * deliberately does not construct Room or navigation dependencies itself.
 */
interface VoyageServiceHost {
    val voyageServiceDependencies: VoyageServiceDependencies
}

class VoyageForegroundService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var collectionJob: Job? = null
    private var dependencies: VoyageServiceDependencies? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        dependencies = (application as? VoyageServiceHost)?.voyageServiceDependencies
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        when (intent?.action ?: ACTION_START_TRACKING) {
            ACTION_STOP_TRACKING -> {
                stopTracking()
                return START_NOT_STICKY
            }

            ACTION_START_TRACKING -> {
                startAsForeground()
                startLocationCollection()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        collectionJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startLocationCollection() {
        if (collectionJob?.isActive == true) return
        val serviceDependencies =
            dependencies ?: run {
                stopTracking()
                return
            }
        collectionJob =
            serviceScope.launch {
                val session =
                    runCatching {
                        serviceDependencies.activeVoyageManager.restoreActiveVoyage()
                    }.getOrNull()
                if (session == null || serviceDependencies.locationProvider.access() != LocationAccess.PRECISE) {
                    stopTracking()
                    return@launch
                }

                serviceDependencies.locationProvider
                    .locationUpdates()
                    .catch { error ->
                        if (error is CancellationException) throw error
                        stopTracking()
                    }.collect { fix ->
                        runCatching {
                            serviceDependencies.activeVoyageManager.processLocation(fix)
                        }
                    }
            }
    }

    private fun startAsForeground() {
        try {
            val foregroundServiceType =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                } else {
                    0
                }
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                navigationNotification(),
                foregroundServiceType,
            )
        } catch (e: Exception) {
            android.util.Log.e("VoyageForegroundService", "Could not start foreground service", e)
        }
    }

    private fun stopTracking() {
        collectionJob?.cancel()
        collectionJob = null
        try {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        } catch (e: Exception) {
            android.util.Log.e("VoyageForegroundService", "Could not stop foreground service", e)
        }
        stopSelf()
    }

    private fun navigationNotification(): Notification {
        val activityIntent =
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        val contentIntent =
            PendingIntent.getActivity(
                this,
                0,
                activityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        return NotificationCompat
            .Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_navigation_notification)
            .setContentTitle("TideNode Navigation")
            .setContentText("GPS-Aufzeichnung der aktiven Fahrt läuft")
            .setContentIntent(contentIntent)
            .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel =
            NotificationChannel(
                CHANNEL_ID,
                "Aktive Navigation",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Sichtbare GPS-Aufzeichnung während einer aktiven Fahrt"
                setShowBadge(false)
            }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "active_voyage_navigation"
        const val NOTIFICATION_ID = 4_201
        const val ACTION_START_TRACKING =
            "com.example.trnberechnung.navigation.action.START_TRACKING"
        const val ACTION_STOP_TRACKING =
            "com.example.trnberechnung.navigation.action.STOP_TRACKING"
    }
}

object VoyageServiceController {
    /**
     * Must only be called from a currently visible Activity after the skipper
     * explicitly starts or resumes navigation.
     */
    @MainThread
    fun startFromVisibleActivity(activity: Activity) {
        try {
            val intent =
                Intent(activity, VoyageForegroundService::class.java).setAction(
                    VoyageForegroundService.ACTION_START_TRACKING,
                )
            ContextCompat.startForegroundService(activity, intent)
        } catch (e: Exception) {
            android.util.Log.e("VoyageServiceController", "Could not start foreground service", e)
        }
    }

    fun stop(context: Context) {
        try {
            context.stopService(Intent(context, VoyageForegroundService::class.java))
        } catch (e: Exception) {
            android.util.Log.e("VoyageServiceController", "Could not stop foreground service", e)
        }
    }
}

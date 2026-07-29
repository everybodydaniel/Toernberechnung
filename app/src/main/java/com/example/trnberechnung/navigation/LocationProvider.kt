package com.example.trnberechnung.navigation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow

enum class LocationAccess {
    DENIED,
    APPROXIMATE,
    PRECISE,
}

data class LocationUpdateOptions(
    val intervalMillis: Long = 1_000L,
    val minimumIntervalMillis: Long = 500L,
    val minimumDistanceMeters: Float = 5f,
) {
    init {
        require(intervalMillis > 0L) { "Location interval must be positive." }
        require(minimumIntervalMillis > 0L) {
            "Minimum location interval must be positive."
        }
        require(minimumDistanceMeters >= 0f) {
            "Minimum location distance must not be negative."
        }
    }
}

interface LocationProvider {
    val latestFix: StateFlow<LocationFix?>

    fun access(): LocationAccess

    fun locationUpdates(
        options: LocationUpdateOptions = LocationUpdateOptions(),
    ): Flow<LocationFix>
}

class FusedLocationProvider(
    context: Context,
    private val client: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context.applicationContext),
) : LocationProvider {
    private val applicationContext = context.applicationContext
    private val mutableLatestFix = MutableStateFlow<LocationFix?>(null)

    override val latestFix: StateFlow<LocationFix?> = mutableLatestFix.asStateFlow()

    override fun access(): LocationAccess =
        when {
            hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) -> LocationAccess.PRECISE
            hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION) -> LocationAccess.APPROXIMATE
            else -> LocationAccess.DENIED
        }

    override fun locationUpdates(options: LocationUpdateOptions): Flow<LocationFix> =
        callbackFlow {
            if (access() == LocationAccess.DENIED) {
                close(SecurityException("Location permission has not been granted."))
                return@callbackFlow
            }

            val request =
                LocationRequest
                    .Builder(Priority.PRIORITY_HIGH_ACCURACY, options.intervalMillis)
                    .setMinUpdateIntervalMillis(options.minimumIntervalMillis)
                    .setMinUpdateDistanceMeters(options.minimumDistanceMeters)
                    .setWaitForAccurateLocation(true)
                    .build()
            val callback =
                object : LocationCallback() {
                    override fun onLocationResult(result: LocationResult) {
                        result.locations.forEach { location ->
                            val fix =
                                LocationFix(
                                    coordinate =
                                        GeoPoint(
                                            latitude = location.latitude,
                                            longitude = location.longitude,
                                        ),
                                    horizontalAccuracyMeters =
                                        if (location.hasAccuracy()) {
                                            location.accuracy.toDouble()
                                        } else {
                                            -1.0
                                        },
                                    timestampEpochMillis = location.time,
                                    speedMetersPerSecond =
                                        if (location.hasSpeed()) {
                                            location.speed.toDouble()
                                        } else {
                                            0.0
                                        },
                                    courseDegrees =
                                        if (location.hasBearing()) {
                                            GeoMath.normalizeDegrees(location.bearing.toDouble())
                                        } else {
                                            null
                                        },
                                )
                            mutableLatestFix.value = fix
                            trySend(fix)
                        }
                    }
                }

            try {
                client
                    .requestLocationUpdates(request, callback, Looper.getMainLooper())
                    .addOnFailureListener { error -> close(error) }
            } catch (error: SecurityException) {
                close(error)
                return@callbackFlow
            }
            awaitClose { client.removeLocationUpdates(callback) }
        }.buffer(Channel.CONFLATED)

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(applicationContext, permission) ==
            PackageManager.PERMISSION_GRANTED
}

package com.example.trnberechnung.navigation

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow

data class HeadingSample(
    val magneticHeadingDegrees: Double,
    val trueHeadingDegrees: Double,
    val timestampNanos: Long,
)

class HeadingUnavailableException :
    IllegalStateException("This device has no rotation-vector sensor.")

interface HeadingProvider {
    val headings: Flow<HeadingSample>
}

class SensorHeadingProvider(
    context: Context,
    private val latestLocation: () -> LocationFix? = { null },
    private val sensorManager: SensorManager? =
        context.applicationContext.getSystemService(SensorManager::class.java),
) : HeadingProvider {
    override val headings: Flow<HeadingSample> =
        callbackFlow {
            val rotationSensor =
                sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
                    ?: run {
                        close(HeadingUnavailableException())
                        return@callbackFlow
                    }
            val rotationMatrix = FloatArray(9)
            val orientation = FloatArray(3)
            val listener =
                object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent) {
                        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                        SensorManager.getOrientation(rotationMatrix, orientation)
                        val magneticHeading =
                            GeoMath.normalizeDegrees(Math.toDegrees(orientation[0].toDouble()))
                        val fix = latestLocation()
                        val declination =
                            fix?.let {
                                GeomagneticField(
                                    it.coordinate.latitude.toFloat(),
                                    it.coordinate.longitude.toFloat(),
                                    0f,
                                    it.timestampEpochMillis,
                                ).declination.toDouble()
                            } ?: 0.0
                        trySend(
                            HeadingSample(
                                magneticHeadingDegrees = magneticHeading,
                                trueHeadingDegrees =
                                    GeoMath.normalizeDegrees(magneticHeading + declination),
                                timestampNanos = event.timestamp,
                            ),
                        )
                    }

                    override fun onAccuracyChanged(
                        sensor: Sensor?,
                        accuracy: Int,
                    ) = Unit
                }
            val registered =
                sensorManager?.registerListener(
                    listener,
                    rotationSensor,
                    SensorManager.SENSOR_DELAY_UI,
                ) == true
            if (!registered) {
                close(HeadingUnavailableException())
                return@callbackFlow
            }
            awaitClose { sensorManager?.unregisterListener(listener) }
        }.buffer(Channel.CONFLATED)
}

package com.skanderjabouzi.newcompass.handlers

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import com.skanderjabouzi.newcompass.Azimuth
import com.skanderjabouzi.newcompass.DisplayRotation
import com.skanderjabouzi.newcompass.LocationStatus
import com.skanderjabouzi.newcompass.RotationVector
import com.skanderjabouzi.newcompass.SensorAccuracy
import com.skanderjabouzi.newcompass.util.MathUtils

class SensorHandler(
    private val appContext: Context,
    private val onAzimuthChanged: (Azimuth) -> Unit,
    private val onSensorAccuracyChanged: (SensorAccuracy) -> Unit,
    private val isTrueNorthEnabled: () -> Boolean,
    private val getCurrentLocation: () -> Location?,
    private val getCurrentLocationStatus: () -> LocationStatus,
    private val getCurrentDisplayRotation: () -> DisplayRotation
) {
    private var sensorManager: SensorManager? = appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensorEventListener: CompassSensorEventListener = CompassSensorEventListener()

    fun start() {
        sensorManager?.let { manager ->
            manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)?.let { sensor ->
                manager.registerListener(
                    sensorEventListener,
                    sensor,
                    SensorManager.SENSOR_DELAY_FASTEST
                )
            }
            manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.let { sensor ->
                manager.registerListener(
                    sensorEventListener,
                    sensor,
                    SensorManager.SENSOR_DELAY_NORMAL
                )
            }
        }
    }

    fun stop() {
        sensorManager?.unregisterListener(sensorEventListener)
    }

    private inner class CompassSensorEventListener : SensorEventListener {
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            when (sensor?.type) {
                Sensor.TYPE_MAGNETIC_FIELD -> {
                    val newAccuracy = when (accuracy) {
                        SensorManager.SENSOR_STATUS_NO_CONTACT -> SensorAccuracy.NO_CONTACT
                        SensorManager.SENSOR_STATUS_UNRELIABLE -> SensorAccuracy.UNRELIABLE
                        SensorManager.SENSOR_STATUS_ACCURACY_LOW -> SensorAccuracy.LOW
                        SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> SensorAccuracy.MEDIUM
                        SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> SensorAccuracy.HIGH
                        else -> SensorAccuracy.NO_CONTACT
                    }
                    onSensorAccuracyChanged(newAccuracy)
                }
            }
        }

        override fun onSensorChanged(event: SensorEvent?) {
            event?.let {
                when (it.sensor.type) {
                    Sensor.TYPE_ROTATION_VECTOR -> {
                        val rotationVector = RotationVector(it.values[0], it.values[1], it.values[2])
                        val displayRotation = getCurrentDisplayRotation()
                        val magneticAzimuth = MathUtils.calculateAzimuth(rotationVector, displayRotation)

                        val finalAzimuth = if (isTrueNorthEnabled()) {
                            val currentLoc = getCurrentLocation()
                            if (currentLoc != null && getCurrentLocationStatus() == LocationStatus.PRESENT) {
                                val declination = MathUtils.getMagneticDeclination(currentLoc)
                                magneticAzimuth.plus(declination)
                            } else {
                                magneticAzimuth
                            }
                        } else {
                            magneticAzimuth
                        }
                        onAzimuthChanged(finalAzimuth)
                    }
                }
            }
        }
    }
}
// CompassViewModel.kt
package com.skanderjabouzi.newcompass

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skanderjabouzi.newcompass.util.MathUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CompassViewModel : ViewModel() {
    private val _azimuth = MutableStateFlow<Azimuth?>(null)
    val azimuth: StateFlow<Azimuth?> = _azimuth.asStateFlow()

    private val _sensorAccuracy = MutableStateFlow(SensorAccuracy.NO_CONTACT)
    val sensorAccuracy: StateFlow<SensorAccuracy> = _sensorAccuracy.asStateFlow()

    private val _trueNorth = MutableStateFlow(false)
    val trueNorth: StateFlow<Boolean> = _trueNorth.asStateFlow()

    private val _location = MutableStateFlow<Location?>(null)
    val location: StateFlow<Location?> = _location.asStateFlow()

    private val _locationStatus = MutableStateFlow(LocationStatus.NOT_PRESENT)
    val locationStatus: StateFlow<LocationStatus> = _locationStatus.asStateFlow()

    private var sensorManager: SensorManager? = null
    private var locationManager: LocationManager? = null
    private val sensorEventListener = CompassSensorEventListener()
    private var locationRequest: CancellationSignal? = null

    fun startSensors(context: Context) {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        sensorManager?.let { manager ->
            // Register rotation vector sensor
            manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)?.let { sensor ->
                manager.registerListener(
                    sensorEventListener,
                    sensor,
                    SensorManager.SENSOR_DELAY_FASTEST
                )
            }

            // Register magnetic field sensor
            manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.let { sensor ->
                manager.registerListener(
                    sensorEventListener,
                    sensor,
                    SensorManager.SENSOR_DELAY_NORMAL
                )
            }
        }
    }

    fun stopSensors() {
        sensorManager?.unregisterListener(sensorEventListener)
        locationRequest?.cancel()
    }

    fun setTrueNorth(enabled: Boolean) {
        _trueNorth.value = enabled
    }

    fun requestLocation() {
        // Implementation similar to original CompassFragment
        viewModelScope.launch {
            try {
                _locationStatus.value = LocationStatus.LOADING
                // Location request logic here
                // This would need permission handling and actual location request
            } catch (e: Exception) {
                _locationStatus.value = LocationStatus.NOT_PRESENT
            }
        }
    }

    private inner class CompassSensorEventListener : SensorEventListener {
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            when (sensor?.type) {
                Sensor.TYPE_MAGNETIC_FIELD -> {
                    _sensorAccuracy.value = when (accuracy) {
                        SensorManager.SENSOR_STATUS_NO_CONTACT -> SensorAccuracy.NO_CONTACT
                        SensorManager.SENSOR_STATUS_UNRELIABLE -> SensorAccuracy.UNRELIABLE
                        SensorManager.SENSOR_STATUS_ACCURACY_LOW -> SensorAccuracy.LOW
                        SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> SensorAccuracy.MEDIUM
                        SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> SensorAccuracy.HIGH
                        else -> SensorAccuracy.NO_CONTACT
                    }
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

                        val finalAzimuth = if (_trueNorth.value) {
                            val declination = _location.value?.let { loc ->
                                MathUtils.getMagneticDeclination(loc)
                            } ?: 0f
                            magneticAzimuth.plus(declination)
                        } else {
                            magneticAzimuth
                        }

                        _azimuth.value = finalAzimuth
                    }
                }
            }
        }

        private fun getCurrentDisplayRotation(): DisplayRotation {
            // Implementation to get current display rotation
            return DisplayRotation.ROTATION_0
        }
    }
}
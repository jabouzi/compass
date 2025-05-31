// CompassViewModel.kt
package com.skanderjabouzi.newcompass

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.CancellationSignal
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skanderjabouzi.newcompass.util.MathUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CompassViewModel(private val context: Context) : ViewModel() {

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
    private var locationListener: LocationListener? = null

    var currentLocation by mutableStateOf<Location?>(null)
        private set

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
                checkLocationPermission()
            } catch (e: Exception) {
                _locationStatus.value = LocationStatus.NOT_PRESENT
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        try {
            locationListener = object : LocationListener {
                override fun onLocationChanged(newLocation: Location) {
                    currentLocation = newLocation
                }

                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }

            locationManager?.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                5000, // 5 seconds
                10f,   // 10 meters
                locationListener!!
            )

            // Get last known location
            locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let { lastLocation ->
                currentLocation = lastLocation
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            _locationStatus.value = LocationStatus.PERMISSION_DENIED
        }

        _locationStatus.value = LocationStatus.PRESENT
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
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
import android.view.Surface
import android.view.WindowManager
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModel
import com.skanderjabouzi.newcompass.model.Azimuth
import com.skanderjabouzi.newcompass.model.DisplayRotation
import com.skanderjabouzi.newcompass.model.LocationStatus
import com.skanderjabouzi.newcompass.model.RotationVector
import com.skanderjabouzi.newcompass.model.SensorAccuracy
import com.skanderjabouzi.newcompass.util.MathUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow


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
    // private var locationRequest: CancellationSignal? = null // For getCurrentLocation
    private var locationListener: LocationListener? = null
    private val appContext: Context = context.applicationContext


    fun startSensors() { // Renamed from startSensors(context: Context) as context is now a class property
        sensorManager = appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        locationManager = appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager

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
        // If True North was enabled before sensors were stopped, re-start location updates
        if (_trueNorth.value) {
            startLocationUpdates()
        }
    }

    fun stopSensors() {
        sensorManager?.unregisterListener(sensorEventListener)
        stopLocationUpdates() // Ensure location updates are also stopped
        // locationRequest?.cancel() // If using getCurrentLocation elsewhere
    }

    fun setTrueNorth(enabled: Boolean) {
        _trueNorth.value = enabled
        if (enabled) {
            startLocationUpdates()
        } else {
            stopLocationUpdates()
        }
    }

    // This requestLocation is for a one-time high-accuracy fix.
    // If you only need location for True North, this might be optional.
    // The implementation from the previous AI response can be adapted here if needed.
    // For now, let's assume True North uses startLocationUpdates/stopLocationUpdates.
    /*
    fun requestLocation() {
        viewModelScope.launch {
            // ... implementation for single location update using getCurrentLocation ...
            // (Refer to previous AI response for a complete example)
            // This would involve checking appContext, locationManager, permissions,
            // and then calling locationManager.getCurrentLocation(...)
        }
    }
    */

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        if (locationManager == null) {
            locationManager = appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        }

        if (ActivityCompat.checkSelfPermission(
                appContext,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                appContext,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            _locationStatus.value = LocationStatus.PERMISSION_DENIED
            return
        }

        _locationStatus.value = LocationStatus.LOADING

        if (locationListener == null) {
            locationListener = object : LocationListener {
                override fun onLocationChanged(newLocation: Location) {
                    _location.value = newLocation
                    _locationStatus.value = LocationStatus.PRESENT
                }

                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

                override fun onProviderEnabled(provider: String) {
                    _locationStatus.value = LocationStatus.LOADING // Or attempt to get location again
                }

                override fun onProviderDisabled(provider: String) {
                    _location.value = null
                    _locationStatus.value = LocationStatus.NOT_PRESENT
                }
            }
        }

        try {
            // Attempt to get last known location first
            val lastKnownLocation = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if (lastKnownLocation != null) {
                _location.value = lastKnownLocation
                _locationStatus.value = LocationStatus.PRESENT
            }
            // else status remains LOADING until first update from requestLocationUpdates

            locationManager?.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                5000L, // 5 seconds
                10f,   // 10 meters
                locationListener!!
            )
        } catch (e: SecurityException) {
            _locationStatus.value = LocationStatus.PERMISSION_DENIED
        } catch (e: Exception) {
            _location.value = null
            _locationStatus.value = LocationStatus.NOT_PRESENT
        }
    }

    fun stopLocationUpdates() {
        locationListener?.let {
            locationManager?.removeUpdates(it)
            // locationListener = null // Consider nullifying if you want a fresh listener object each time
        }
        // Only set to NOT_PRESENT if we are explicitly stopping.
        // If it's due to provider disabled, onProviderDisabled handles it.
        if (_trueNorth.value == false) { // Or a more general condition for stopping
            _location.value = null
            _locationStatus.value = LocationStatus.NOT_PRESENT
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
                        val displayRotation = getCurrentDisplayRotation() // Ensure this is correctly implemented
                        val magneticAzimuth = MathUtils.calculateAzimuth(rotationVector, displayRotation)

                        val finalAzimuth = if (_trueNorth.value) {
                            val currentLoc = _location.value // Use the StateFlow value
                            if (currentLoc != null && _locationStatus.value == LocationStatus.PRESENT) {
                                val declination = MathUtils.getMagneticDeclination(currentLoc)
                                magneticAzimuth.plus(declination)
                            } else {
                                // If location not available for true north, fall back to magnetic
                                // Or indicate that true north cannot be calculated
                                magneticAzimuth
                            }
                        } else {
                            magneticAzimuth
                        }
                        _azimuth.value = finalAzimuth
                    }
                }
            }
        }

        private fun getCurrentDisplayRotation(): DisplayRotation {
            // This method is used to get the actual display rotation from the context
            // by using context.display.rotation
             val display = (appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
             return when (display.rotation) {
                 Surface.ROTATION_0 -> DisplayRotation.ROTATION_0
                 Surface.ROTATION_90 -> DisplayRotation.ROTATION_90
                 Surface.ROTATION_180 -> DisplayRotation.ROTATION_180
                 Surface.ROTATION_270 -> DisplayRotation.ROTATION_270
                 else -> DisplayRotation.ROTATION_0
             }
        }
    }
}
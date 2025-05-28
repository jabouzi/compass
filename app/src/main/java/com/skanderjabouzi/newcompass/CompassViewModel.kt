// CompassViewModel.kt
package com.skanderjabouzi.newcompass

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
import androidx.compose.runtime.*
import androidx.core.app.ActivityCompat
import kotlin.math.*

class CompassViewModel(private val context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    // Advanced smooth filtering
    private var filteredAzimuth = 0f
    private val filterAlpha = 0.15f

    // Stability detection
    private val azimuthHistory = mutableListOf<Float>()
    private val historySize = 10
    private var stableCount = 0
    private val stabilityThreshold = 1.0f
    private val requiredStableReadings = 5

    // Adaptive filtering
    private var currentAlpha = filterAlpha

    // Location listener for declination calculation
    private var locationListener: LocationListener? = null

    // State variables
    var azimuth by mutableStateOf(0f)
        private set

    var declination by mutableStateOf(0f)
        private set

    var useTrueNorth by mutableStateOf(false)

    var currentLocation by mutableStateOf<Location?>(null)
        private set

    var hasLocationPermission by mutableStateOf(false)
        private set

    var isStable by mutableStateOf(false)
        private set

    fun startSensorUpdates() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        magnetometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stopSensorUpdates() {
        sensorManager.unregisterListener(this)
    }

    fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            hasLocationPermission = false
            return
        }

        hasLocationPermission = true

        try {
            locationListener = object : LocationListener {
                override fun onLocationChanged(newLocation: Location) {
                    currentLocation = newLocation
                    calculateDeclination(newLocation)
                }

                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }

            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                5000, // 5 seconds
                10f,   // 10 meters
                locationListener!!
            )

            // Get last known location
            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let { lastLocation ->
                currentLocation = lastLocation
                calculateDeclination(lastLocation)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun calculateDeclination(location: Location) {
        val geoField = android.hardware.GeomagneticField(
            location.latitude.toFloat(),
            location.longitude.toFloat(),
            location.altitude.toFloat(),
            System.currentTimeMillis()
        )
        declination = geoField.declination
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.size)
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.size)
            }
        }

        updateCompassReading()
    }

    private fun updateCompassReading() {
        val success = SensorManager.getRotationMatrix(
            rotationMatrix,
            null,
            accelerometerReading,
            magnetometerReading
        )

        if (success) {
            SensorManager.getOrientation(rotationMatrix, orientationAngles)

            var newAzimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()

            // Normalize to 0-360
            if (newAzimuth < 0) {
                newAzimuth += 360f
            }

            // Update azimuth history for stability detection
            updateAzimuthHistory(newAzimuth)

            // Check if compass is stable (not moving much)
            checkStability()

            // Adaptive filtering based on stability
            currentAlpha = if (isStable) {
                filterAlpha * 0.3f // Much slower filtering when stable
            } else {
                filterAlpha // Normal filtering when moving
            }

            // Apply smooth filtering to prevent flickering
            val angleDiff = calculateAngleDifference(newAzimuth, filteredAzimuth)

            // If stable and difference is very small, ignore micro-movements
            val filteredDiff = if (isStable && abs(angleDiff) < 0.5f) {
                0f // Completely ignore tiny movements when stable
            } else {
                angleDiff * currentAlpha
            }

            filteredAzimuth += filteredDiff

            // Normalize filtered azimuth
            filteredAzimuth = normalizeAngle(filteredAzimuth)

            // Apply magnetic declination for true north
            val finalAzimuth = if (useTrueNorth) {
                normalizeAngle(filteredAzimuth + declination)
            } else {
                filteredAzimuth
            }

            azimuth = finalAzimuth
        }
    }

    private fun updateAzimuthHistory(newAzimuth: Float) {
        azimuthHistory.add(newAzimuth)
        if (azimuthHistory.size > historySize) {
            azimuthHistory.removeAt(0)
        }
    }

    private fun checkStability() {
        if (azimuthHistory.size < historySize) {
            isStable = false
            stableCount = 0
            return
        }

        // Calculate variance in recent readings
        val variance = calculateVariance(azimuthHistory)

        if (variance < stabilityThreshold) {
            stableCount++
            if (stableCount >= requiredStableReadings) {
                isStable = true
            }
        } else {
            stableCount = 0
            isStable = false
        }
    }

    private fun calculateVariance(angles: List<Float>): Float {
        if (angles.size < 2) return Float.MAX_VALUE

        // Convert to unit vectors to handle angle wraparound
        var sumX = 0.0
        var sumY = 0.0

        for (angle in angles) {
            val radians = Math.toRadians(angle.toDouble())
            sumX += cos(radians)
            sumY += sin(radians)
        }

        val avgX = sumX / angles.size
        val avgY = sumY / angles.size
        val avgAngle = atan2(avgY, avgX)

        // Calculate circular variance
        var variance = 0.0
        for (angle in angles) {
            val radians = Math.toRadians(angle.toDouble())
            val diff = atan2(sin(radians - avgAngle), cos(radians - avgAngle))
            variance += diff * diff
        }

        return Math.toDegrees(sqrt(variance / angles.size)).toFloat()
    }

    private fun calculateAngleDifference(angle1: Float, angle2: Float): Float {
        var diff = angle1 - angle2
        while (diff > 180) diff -= 360
        while (diff < -180) diff += 360
        return diff
    }

    private fun normalizeAngle(angle: Float): Float {
        var normalized = angle
        while (normalized < 0) normalized += 360f
        while (normalized >= 360) normalized -= 360f
        return normalized
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Handle accuracy changes if needed
    }

    fun toggleNorthType() {
        useTrueNorth = !useTrueNorth
    }

    fun cleanup() {
        stopSensorUpdates()
        locationListener?.let {
            locationManager.removeUpdates(it)
        }
    }
}
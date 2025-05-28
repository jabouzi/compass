package com.skanderjabouzi.newcompass

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class CompassManager(private val context: Context) : SensorEventListener {

    /** constants **/
    private val LOCATION_UPDATE_MIN_TIME = 60000L
    private val LOCATION_UPDATE_MIN_DISTANCE = 10000f

    enum class CompassStatus {
        GOOD,
        INTERFERENCE,
        INACTIVE,
        GYROSCOPE_UNAVAILABLE,
        LOCATION_UNAVAILABLE
    }

    private val MAGNETIC_INTERFERENCE_THRESHOLD_MODIFIER = 1.05f

    /** variables **/
    private val locationManager: LocationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val magSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    private val accelSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    private var sensorsRegistered: Boolean = false
    @Volatile private var accelMagDataReady: Boolean = false
    @Volatile private var gyroDataReady: Boolean = false

    private var magValues: FloatArray? = null
    private var accelValues: FloatArray? = null
    private var gyroValues: FloatArray? = null

    private var lastGyroTimestamp: Long = 0L
    // No longer directly using rotationMatrixFromGyro or deltaRotationVector as their intended use
    // with SensorManager.getRotationVectorFromGyro was incorrect for this manual fusion.
    // We are directly integrating gyroValues (angular velocity) into Euler angles.

    private var fusedOrientation = FloatArray(3) // Holds azimuth, pitch, roll in radians

    private val COMPLEMENTARY_FILTER_ALPHA = 0.98f

    private var locationCache: Location? = null
    private var geoField: GeomagneticField? = null

    @Volatile private var useManualDeclination: Boolean = false
    @Volatile private var manualDeclination: Float = 0.0f

    val _bearing = MutableStateFlow(0f)
    val bearing: StateFlow<Float> = _bearing.asStateFlow()

    private val _declination = MutableStateFlow(0f)
    val declination: StateFlow<Float> = _declination.asStateFlow()

    private val _isUsingManualDeclination = MutableStateFlow(false)
    val isUsingManualDeclinationFlow: StateFlow<Boolean> = _isUsingManualDeclination.asStateFlow()

    private val _compassStatus = MutableStateFlow(CompassStatus.INACTIVE)
    val compassStatus: StateFlow<CompassStatus> = _compassStatus.asStateFlow()

    private val locationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            locationCache = location
            updateGeoField()
            if (!useManualDeclination) {
                _declination.value = getDeclinationInternal()
            }
            if (_compassStatus.value == CompassStatus.LOCATION_UNAVAILABLE) {
                _compassStatus.value = CompassStatus.GOOD
            }
        }

        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {
            _compassStatus.value = CompassStatus.LOCATION_UNAVAILABLE
        }
    }

    private fun interferenceTest(values: FloatArray) {
        val threshold = getExpectedFieldStrength() * MAGNETIC_INTERFERENCE_THRESHOLD_MODIFIER
        var magnitude = 0f
        for (value in values) {
            magnitude += value * value
        }
        magnitude = sqrt(magnitude)

        if (magnitude > threshold) {
            _compassStatus.value = CompassStatus.INTERFERENCE
        } else {
            if (_compassStatus.value == CompassStatus.INTERFERENCE) {
                _compassStatus.value = CompassStatus.GOOD
            }
        }
    }

    private fun getExpectedFieldStrength(): Float {
        return geoField?.fieldStrength ?: 50f
    }

    private fun updateGeoField() {
        locationCache?.let { location ->
            geoField = GeomagneticField(
                location.latitude.toFloat(),
                location.longitude.toFloat(),
                location.altitude.toFloat(),
                System.currentTimeMillis()
            )
        }
    }

    private fun convertToTrueNorth(bearing: Float): Float {
        return bearing + getDeclinationInternal()
    }

    private fun fuseSensorData() {
        // Ensure we have accelerometer and magnetic field data
        if (accelValues == null || magValues == null) {
            return
        }

        // 1. Get orientation from Accelerometer and Magnetometer (stable but noisy/jumpy)
        val R_accelMag = FloatArray(16)
        val I_accelMag = FloatArray(16)
        SensorManager.getRotationMatrix(R_accelMag, I_accelMag, accelValues, magValues)
        val orientation_accelMag = FloatArray(3)
        SensorManager.getOrientation(R_accelMag, orientation_accelMag) // Azimuth, Pitch, Roll in radians

        // 2. Gyroscope integration (smooth but drifts over time)
        if (gyroValues != null && gyroDataReady && lastGyroTimestamp != 0L) {
            val dt = (System.nanoTime() - lastGyroTimestamp) * 1.0f / 1_000_000_000.0f

            // Complementary Filter Fusion for Euler Angles
            // This is a simplified integration. For robustness, use quaternions.
            fusedOrientation[0] = COMPLEMENTARY_FILTER_ALPHA * (fusedOrientation[0] + gyroValues!![2] * dt) + (1 - COMPLEMENTARY_FILTER_ALPHA) * orientation_accelMag[0] // Azimuth (rotation around Z-axis)
            fusedOrientation[1] = COMPLEMENTARY_FILTER_ALPHA * (fusedOrientation[1] + gyroValues!![1] * dt) + (1 - COMPLEMENTARY_FILTER_ALPHA) * orientation_accelMag[1] // Pitch (rotation around X-axis)
            fusedOrientation[2] = COMPLEMENTARY_FILTER_ALPHA * (fusedOrientation[2] + gyroValues!![0] * dt) + (1 - COMPLEMENTARY_FILTER_ALPHA) * orientation_accelMag[2] // Roll (rotation around Y-axis)

            // Normalize azimuth to be within -PI to PI
            fusedOrientation[0] = (fusedOrientation[0] + Math.PI.toFloat() * 2) % (Math.PI.toFloat() * 2)
            if (fusedOrientation[0] > Math.PI.toFloat()) fusedOrientation[0] -= Math.PI.toFloat() * 2
            if (fusedOrientation[0] < -Math.PI.toFloat()) fusedOrientation[0] += Math.PI.toFloat() * 2

        } else {
            // If gyro data isn't available or ready, rely solely on accel/mag
            fusedOrientation = orientation_accelMag.clone()
        }

        // Update lastGyroTimestamp here, regardless of whether gyroDataReady was true
        // This ensures dt calculation for the next cycle is accurate even if gyro was delayed
        if (gyroSensor != null) {
            lastGyroTimestamp = System.nanoTime()
        }


        // Flags reset after fusion attempt
        accelMagDataReady = false
        gyroDataReady = false

        // Update the public bearing flow
        _bearing.value = getPositiveBearing(true)
    }

    private fun getDeclinationInternal(): Float {
        if (useManualDeclination) {
            return manualDeclination
        }
        return geoField?.declination ?: 0f
    }

    fun setManualDeclination(declination: Float) {
        useManualDeclination = true
        manualDeclination = declination
        _isUsingManualDeclination.value = true
        _declination.value = declination
    }

    fun useAutoDeclination() {
        useManualDeclination = false
        _isUsingManualDeclination.value = false
        _declination.value = getDeclinationInternal()
    }

    fun getBearing(trueNorth: Boolean): Float {
        // We now directly use the `fusedOrientation` stored by `fuseSensorData()`
        var azimuth = fusedOrientation[0]
        var bearing = azimuth * (360f / (2 * Math.PI)).toFloat() // convert from radians into degrees

        if (trueNorth) {
            bearing = convertToTrueNorth(bearing)
        }
        return bearing
    }

    fun getPositiveBearing(trueNorth: Boolean): Float {
        var bearing = getBearing(trueNorth)
        if (bearing < 0) {
            bearing += 360f
        }
        return bearing
    }

    @SuppressLint("MissingPermission")
    fun registerSensors() {
        if (!sensorsRegistered) {
            try {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    LOCATION_UPDATE_MIN_TIME,
                    LOCATION_UPDATE_MIN_DISTANCE,
                    locationListener
                )
            } catch (e: IllegalArgumentException) {
                _compassStatus.value = CompassStatus.LOCATION_UNAVAILABLE
            }

            magSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
            accelSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
            gyroSensor?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
                lastGyroTimestamp = System.nanoTime() // Initialize timestamp for gyro
            } ?: run {
                _compassStatus.value = CompassStatus.GYROSCOPE_UNAVAILABLE
            }

            // Reset states for new registration
            accelMagDataReady = false
            gyroDataReady = false
            fusedOrientation = FloatArray(3) // Reset fused orientation to 0s
            // Initialize fused orientation from the first stable accel/mag reading if possible
            // This is better done in fuseSensorData after the first valid accel/mag input

            if (_compassStatus.value != CompassStatus.GYROSCOPE_UNAVAILABLE && _compassStatus.value != CompassStatus.LOCATION_UNAVAILABLE) {
                _compassStatus.value = CompassStatus.GOOD
            }
            sensorsRegistered = true
        }
    }

    fun unregisterSensors() {
        if (sensorsRegistered) {
            locationManager.removeUpdates(locationListener)
            sensorManager.unregisterListener(this, magSensor)
            sensorManager.unregisterListener(this, accelSensor)
            sensorManager.unregisterListener(this, gyroSensor)
            accelMagDataReady = false
            gyroDataReady = false
            lastGyroTimestamp = 0L
            _compassStatus.value = CompassStatus.INACTIVE
            sensorsRegistered = false
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            when (it.sensor.type) {
                Sensor.TYPE_MAGNETIC_FIELD -> {
                    magValues = it.values.clone()
                    interferenceTest(magValues!!)
                    if (accelValues != null) accelMagDataReady = true
                }
                Sensor.TYPE_ACCELEROMETER -> {
                    accelValues = it.values.clone()
                    if (magValues != null) accelMagDataReady = true
                }
                Sensor.TYPE_GYROSCOPE -> {
                    gyroValues = it.values.clone()
                    gyroDataReady = true
                }
            }

            // Trigger fusion if conditions are met
            // Only fuse if we have accel/mag data (required for correction)
            if (accelMagDataReady) {
                // Also require gyro data if the sensor is present
                if (gyroSensor != null && !gyroDataReady) {
                    // Waiting for gyro data, don't fuse yet if gyro is expected
                    // This scenario means we are waiting for a complete set of data
                    return
                }
                fuseSensorData()
            }
            // If accelMagDataReady is false, we can't do the primary fusion.
            // In a more robust system, you might have a gyro-only fallback that drifts.
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Can be used to report sensor accuracy to the user
    }
}
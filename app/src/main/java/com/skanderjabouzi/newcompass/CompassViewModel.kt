package com.skanderjabouzi.newcompass

import android.location.Location
import androidx.lifecycle.ViewModel
import com.skanderjabouzi.newcompass.model.Azimuth
import com.skanderjabouzi.newcompass.model.LocationStatus
import com.skanderjabouzi.newcompass.model.SensorAccuracy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CompassViewModel : ViewModel() { // No longer takes Context

    private val _azimuth = MutableStateFlow<Azimuth?>(null)
    val azimuth: StateFlow<Azimuth?> = _azimuth.asStateFlow()

    private val _sensorAccuracy = MutableStateFlow(SensorAccuracy.NO_CONTACT)
    val sensorAccuracy: StateFlow<SensorAccuracy> = _sensorAccuracy.asStateFlow()

    private val _trueNorth = MutableStateFlow(false)
    val trueNorth: StateFlow<Boolean> = _trueNorth.asStateFlow()

    private val _location = MutableStateFlow<Location?>(null) // Ensure 'Location' is a common type
    val location: StateFlow<Location?> = _location.asStateFlow()

    private val _locationStatus = MutableStateFlow(LocationStatus.NOT_PRESENT)
    val locationStatus: StateFlow<LocationStatus> = _locationStatus.asStateFlow()

    // Public methods for platform-specific code to update the ViewModel's state

    /**
     * Updates the azimuth value.
     * This should be called by platform-specific code after calculating the azimuth.
     */
    fun updateAzimuth(newAzimuth: Azimuth) {
        _azimuth.value = newAzimuth
    }

    /**
     * Updates the sensor accuracy status.
     * This should be called by platform-specific code when sensor accuracy changes.
     */
    fun updateSensorAccuracy(newAccuracy: SensorAccuracy) {
        _sensorAccuracy.value = newAccuracy
    }

    /**
     * Updates the location data and status.
     * This should be called by platform-specific code when location information is updated.
     * @param newLocation The new location data (ensure 'Location' is a common type).
     * @param newStatus The new status of the location service.
     */
    fun updateLocation(newLocation: Location?, newStatus: LocationStatus) {
        _location.value = newLocation
        _locationStatus.value = newStatus
    }

    /**
     * Sets the True North preference.
     * The actual starting/stopping of location services based on this value
     * should be handled by platform-specific code observing the `trueNorth` StateFlow.
     */
    fun setTrueNorth(enabled: Boolean) {
        _trueNorth.value = enabled
    }

    // Android-specific lifecycle methods like onCleared() from androidx.lifecycle.ViewModel
    // can be kept if the ViewModel base class is still androidx.lifecycle.ViewModel.
    // Any platform-specific cleanup previously done here (like stopping sensors)
    // must be moved to the platform-specific handler.
    // override fun onCleared() {
    //     super.onCleared()
    // }
}
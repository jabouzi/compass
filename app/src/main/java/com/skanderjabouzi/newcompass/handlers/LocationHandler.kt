package com.skanderjabouzi.newcompass.handlers

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.core.app.ActivityCompat
import com.skanderjabouzi.newcompass.model.LocationStatus

class LocationHandler(
    private val appContext: Context,
    private val onLocationHasChanged: (Location?) -> Unit,
    private val onLocationStatusChanged: (LocationStatus) -> Unit
) {
    private var locationManager: LocationManager? = appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private var internalLocationListener: LocationListener? = null

    @SuppressLint("MissingPermission")
    fun startUpdates() {
        if (locationManager == null) { // Ensure locationManager is initialized
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
            onLocationStatusChanged(LocationStatus.PERMISSION_DENIED)
            return
        }

        onLocationStatusChanged(LocationStatus.LOADING)

        if (internalLocationListener == null) {
            internalLocationListener = object : LocationListener {
                override fun onLocationChanged(newLocation: Location) {
                    onLocationHasChanged(newLocation)
                    onLocationStatusChanged(LocationStatus.PRESENT)
                }

                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

                override fun onProviderEnabled(provider: String) {
                    onLocationStatusChanged(LocationStatus.LOADING) // Or attempt to get location again
                }

                override fun onProviderDisabled(provider: String) {
                    onLocationHasChanged(null)
                    onLocationStatusChanged(LocationStatus.NOT_PRESENT)
                }
            }
        }

        try {
            val lastKnownLocation = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if (lastKnownLocation != null) {
                onLocationHasChanged(lastKnownLocation)
                onLocationStatusChanged(LocationStatus.PRESENT)
            }
            // else status remains LOADING until first update from requestLocationUpdates

            locationManager?.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                5000L, // 5 seconds
                10f,   // 10 meters
                internalLocationListener!!
            )
        } catch (e: SecurityException) {
            onLocationStatusChanged(LocationStatus.PERMISSION_DENIED)
        } catch (e: Exception) {
            onLocationHasChanged(null)
            onLocationStatusChanged(LocationStatus.NOT_PRESENT)
        }
    }

    fun stopUpdates() {
        internalLocationListener?.let {
            locationManager?.removeUpdates(it)
        }
        // The ViewModel will manage resetting location and status based on its logic (e.g., trueNorth)
    }
}
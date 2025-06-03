package com.skanderjabouzi.newcompass

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.Surface
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skanderjabouzi.newcompass.handlers.LocationHandler
import com.skanderjabouzi.newcompass.handlers.SensorHandler
import com.skanderjabouzi.newcompass.handlers.rememberLocationPermissionController
import com.skanderjabouzi.newcompass.model.DisplayRotation
import com.skanderjabouzi.newcompass.model.LocationStatus
import com.skanderjabouzi.newcompass.screens.CompassScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // NewCompassTheme { // Apply your app's theme
            MainScreen()
            // }
        }
    }
}

@SuppressLint("ContextCastToActivity", "ServiceCast")
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val compassViewModel: CompassViewModel = viewModel()
    val locationPermissionController = rememberLocationPermissionController()

    val sensorHandler = remember {
        SensorHandler(
            appContext = context.applicationContext,
            onAzimuthChanged = { newAzimuth -> compassViewModel.updateAzimuth(newAzimuth) },
            onSensorAccuracyChanged = { newAccuracy -> compassViewModel.updateSensorAccuracy(newAccuracy) },
            isTrueNorthEnabled = { compassViewModel.trueNorth.value },
            getCurrentLocation = { compassViewModel.location.value },
            getCurrentLocationStatus = { compassViewModel.locationStatus.value },
            getCurrentDisplayRotation = {
                val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                val display = windowManager.defaultDisplay
                when (display.rotation) {
                    Surface.ROTATION_0 -> DisplayRotation.ROTATION_0
                    Surface.ROTATION_90 -> DisplayRotation.ROTATION_90
                    Surface.ROTATION_180 -> DisplayRotation.ROTATION_180
                    Surface.ROTATION_270 -> DisplayRotation.ROTATION_270
                    else -> DisplayRotation.ROTATION_0
                }
            }
        )
    }

    val locationHandler = remember {
        LocationHandler(
            appContext = context.applicationContext,
            onLocationHasChanged = { newLocation ->
                compassViewModel.updateLocation(newLocation, compassViewModel.locationStatus.value)
            },
            onLocationStatusChanged = { newStatus ->
                val currentLocation = if (newStatus == LocationStatus.PRESENT) compassViewModel.location.value else null
                compassViewModel.updateLocation(currentLocation, newStatus)
            }
        )
    }

    val trueNorthEnabled by compassViewModel.trueNorth.collectAsState()
    val locationStatus by compassViewModel.locationStatus.collectAsState()
    var hapticFeedbackUserSetting by remember { mutableStateOf(true) }
    var screenOrientationLocked by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        sensorHandler.start()
        onDispose {
            sensorHandler.stop()
            locationHandler.stopUpdates()
        }
    }

    LaunchedEffect(trueNorthEnabled, locationPermissionController.isPermissionGranted(), locationStatus) {
        if (trueNorthEnabled) {
            if (locationPermissionController.isPermissionGranted()) {
                if (locationStatus != LocationStatus.PRESENT && locationStatus != LocationStatus.LOADING) {
                    locationHandler.startUpdates()
                }
            } else {
                if (locationStatus != LocationStatus.PERMISSION_DENIED) {
                    locationHandler.startUpdates()
                }
            }
        } else {
            locationHandler.stopUpdates()
            compassViewModel.updateLocation(null, LocationStatus.NOT_PRESENT)
        }
    }

    if (trueNorthEnabled && locationStatus == LocationStatus.PERMISSION_DENIED && !locationPermissionController.isPermissionGranted()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("True North functionality requires location permission.")
            Button(onClick = {
                locationPermissionController.requestPermission { isGranted: Boolean -> // Explicitly typed
                    if (isGranted) {
                        locationHandler.startUpdates()
                    }
                }
            }) {
                Text("Grant Location Permission")
            }
        }
    } else {
        CompassScreen(
            compassViewModel = compassViewModel,
            trueNorth = trueNorthEnabled,
            hapticFeedback = hapticFeedbackUserSetting,
            screenOrientationLocked = screenOrientationLocked,
            onTrueNorthChanged = { enabled -> compassViewModel.setTrueNorth(enabled) },
            onHapticFeedbackChanged = { hapticFeedbackUserSetting = it },
            onScreenOrientationChanged = { screenOrientationLocked = it }
        )
    }
}
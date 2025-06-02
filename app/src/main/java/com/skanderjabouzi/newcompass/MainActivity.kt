// MainActivity.kt
package com.skanderjabouzi.newcompass

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.Surface
import android.view.WindowManager // Added import
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skanderjabouzi.newcompass.handlers.LocationHandler
import com.skanderjabouzi.newcompass.handlers.SensorHandler
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
            onLocationChanged = { newLocation ->
                compassViewModel.updateLocation(newLocation, compassViewModel.locationStatus.value)
            },
            onLocationStatusChanged = { newStatus ->
                compassViewModel.updateLocation(compassViewModel.location.value, newStatus)
            }
        )
    }

    val trueNorthEnabled by compassViewModel.trueNorth.collectAsState()
    var hapticFeedbackUserSetting by remember { mutableStateOf(true) } // Added state
    var screenOrientationLocked by remember { mutableStateOf(false) } // Added state

    DisposableEffect(Unit) {
        sensorHandler.start()
        onDispose {
            sensorHandler.stop()
            locationHandler.stopUpdates()
        }
    }

    LaunchedEffect(trueNorthEnabled) {
        if (trueNorthEnabled) {
            locationHandler.startUpdates()
        } else {
            locationHandler.stopUpdates()
            compassViewModel.updateLocation(null, LocationStatus.NOT_PRESENT)
        }
    }

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
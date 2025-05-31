// MainActivity.kt
package com.skanderjabouzi.newcompass

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.skanderjabouzi.newcompass.screens.CompassScreen

class MainActivity : ComponentActivity() {
    private lateinit var compassViewModel: CompassViewModel

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                //compassViewModel.startLocationUpdates()
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                //compassViewModel.startLocationUpdates()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //compassViewModel = CompassViewModel()

        setContent {
            CompassActivity()
        }

        // Request location permissions
        locationPermissionRequest.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ))
    }

    override fun onResume() {
        super.onResume()
        //compassViewModel.startSensorUpdates()
    }

    override fun onPause() {
        super.onPause()
        //compassViewModel.stopSensorUpdates()
    }
}

@SuppressLint("ContextCastToActivity")
@Composable
fun CompassActivity() {
    var screenOrientationLocked by remember { mutableStateOf(false) }
    var trueNorth by remember { mutableStateOf(false) }
    var hapticFeedback by remember { mutableStateOf(true) }

    val activity = LocalContext.current as Activity

    // Handle screen orientation
    LaunchedEffect(screenOrientationLocked) {
        activity.requestedOrientation = if (screenOrientationLocked) {
            ActivityInfo.SCREEN_ORIENTATION_LOCKED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    CompassScreen(
        trueNorth = trueNorth,
        hapticFeedback = hapticFeedback,
        screenOrientationLocked = screenOrientationLocked,
        onTrueNorthChanged = { trueNorth = it },
        onHapticFeedbackChanged = { hapticFeedback = it },
        onScreenOrientationChanged = { screenOrientationLocked = it }
    )
}
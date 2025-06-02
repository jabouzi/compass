package com.skanderjabouzi.newcompass.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.skanderjabouzi.newcompass.model.Azimuth
import com.skanderjabouzi.newcompass.CompassViewModel
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import com.skanderjabouzi.newcompass.util.handleHapticFeedback

@Composable
fun CompassView(
    compassViewModel: CompassViewModel,
    trueNorth: Boolean,
    hapticFeedback: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val azimuth by compassViewModel.azimuth.collectAsState()
    val locationStatus by compassViewModel.locationStatus.collectAsState()

    var lastHapticFeedbackPoint by remember { mutableStateOf<Azimuth?>(null) }

    // Handle sensors and location
    LaunchedEffect(trueNorth) {
        compassViewModel.setTrueNorth(trueNorth)
        // The following check might be redundant if requestLocation is handled by locationStatus changes
        // if (trueNorth && location == null && locationStatus != LocationStatus.Loading) {
        //     compassViewModel.requestLocation()
        // }
    }

    DisposableEffect(Unit) { // Changed context to Unit if sensors don't depend on context for start/stop
        compassViewModel.startSensors()
        onDispose {
            compassViewModel.stopSensors()
        }
    }

    BoxWithConstraints(modifier = modifier) {
        val size = minOf(maxWidth, maxHeight)
        // val center = size / 2 // Not used

        Box(
            modifier = Modifier
                .size(size)
                .align(Alignment.Center),
            contentAlignment = Alignment.Center
        ) {
            // Compass Rose
            azimuth?.let { currentAzimuth ->
                CompassRose(
                    azimuth = currentAzimuth,
                    size = size,
                    modifier = Modifier.fillMaxSize()
                )

                // Handle haptic feedback
                if (hapticFeedback) {
                    LaunchedEffect(currentAzimuth) {
                        handleHapticFeedback(
                            context = context,
                            azimuth = currentAzimuth,
                            lastHapticFeedbackPoint = lastHapticFeedbackPoint,
                            onUpdateLastPoint = { lastHapticFeedbackPoint = it }
                        )
                    }
                }
            }

            // Status overlay
            CompassStatus(
                azimuth = azimuth,
                locationStatus = locationStatus,
                onLocationReloadClick = { compassViewModel.startLocationUpdates() },
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}
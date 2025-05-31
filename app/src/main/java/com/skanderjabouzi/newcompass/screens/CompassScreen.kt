package com.skanderjabouzi.newcompass.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.skanderjabouzi.newcompass.CompassViewModel

@SuppressLint("ViewModelConstructorInComposable")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompassScreen(
    compassViewModel: CompassViewModel,
    trueNorth: Boolean,
    hapticFeedback: Boolean,
    screenOrientationLocked: Boolean,
    onTrueNorthChanged: (Boolean) -> Unit,
    onHapticFeedbackChanged: (Boolean) -> Unit,
    onScreenOrientationChanged: (Boolean) -> Unit
) {
    var showSettings by remember { mutableStateOf(false) }
    var showSensorStatus by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Compass") },
                actions = {
                    CompassTopBarActions(
                        sensorAccuracy = compassViewModel.sensorAccuracy.collectAsState().value,
                        screenOrientationLocked = screenOrientationLocked,
                        onSensorStatusClick = { showSensorStatus = true },
                        onScreenRotationClick = { onScreenOrientationChanged(!screenOrientationLocked) },
                        onSettingsClick = { showSettings = true }
                    )
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            CompassView(
                compassViewModel = compassViewModel,
                trueNorth = trueNorth,
                hapticFeedback = hapticFeedback,
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    if (showSettings) {
        SettingsDialog(
            trueNorth = trueNorth,
            hapticFeedback = hapticFeedback,
            onTrueNorthChanged = onTrueNorthChanged,
            onHapticFeedbackChanged = onHapticFeedbackChanged,
            onDismiss = { showSettings = false }
        )
    }

    if (showSensorStatus) {
        SensorStatusDialog(
            compassViewModel = compassViewModel,
            onDismiss = { showSensorStatus = false }
        )
    }
}
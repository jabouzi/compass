package com.skanderjabouzi.newcompass.screens

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.skanderjabouzi.newcompass.R
import com.skanderjabouzi.newcompass.model.SensorAccuracy

@Composable
fun CompassTopBarActions(
    sensorAccuracy: SensorAccuracy,
    screenOrientationLocked: Boolean,
    onSensorStatusClick: () -> Unit,
    onScreenRotationClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    IconButton(onClick = onSensorStatusClick) {
        Icon(
            painter = painterResource(sensorAccuracy.iconResourceId),
            contentDescription = "Sensor Status",
            modifier = Modifier.size(24.dp), // Added size modifier
            tint = Color.Unspecified //
        )
    }

    IconButton(onClick = onScreenRotationClick) {
        Icon(
            painter = painterResource(
                if (screenOrientationLocked) R.drawable.ic_screen_rotation_lock
                else R.drawable.ic_screen_rotation
            ),
            contentDescription = "Screen Rotation"
        )
    }

    IconButton(onClick = onSettingsClick) {
        Icon(
            Icons.Default.Settings,
            contentDescription = "Settings"
        )
    }
}
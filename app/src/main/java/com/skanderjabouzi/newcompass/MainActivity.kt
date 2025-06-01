// MainActivity.kt
package com.skanderjabouzi.newcompass

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
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

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val compassViewModel: CompassViewModel = viewModel(
        factory = CompassViewModelFactory(context.applicationContext)
    )

    var trueNorthUserSetting by rememberSaveable { mutableStateOf(false) }
    var hapticFeedbackUserSetting by rememberSaveable { mutableStateOf(true) }
    var screenOrientationLocked by rememberSaveable { mutableStateOf(false) }

    // Effect to lock/unlock screen orientation
    val activity = LocalContext.current as? ComponentActivity
    LaunchedEffect(screenOrientationLocked) {
        activity?.requestedOrientation = if (screenOrientationLocked) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT // Or your preferred locked orientation
        } else {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    CompassScreen(
        compassViewModel = compassViewModel,
        trueNorth = trueNorthUserSetting,
        hapticFeedback = hapticFeedbackUserSetting,
        screenOrientationLocked = screenOrientationLocked,
        onTrueNorthChanged = { trueNorthUserSetting = it },
        onHapticFeedbackChanged = { hapticFeedbackUserSetting = it },
        onScreenOrientationChanged = { screenOrientationLocked = it }
    )
}
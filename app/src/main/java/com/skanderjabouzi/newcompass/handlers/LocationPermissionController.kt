package com.skanderjabouzi.newcompass.handlers

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

interface LocationPermissionController {
    fun requestPermission(onResult: (isGranted: Boolean) -> Unit)
    fun isPermissionGranted(): Boolean
}

@Composable
fun rememberLocationPermissionController(): LocationPermissionController {
    val context = LocalContext.current
    var currentOnResultCallback by remember { mutableStateOf<((Boolean) -> Unit)?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted: Boolean -> // Explicitly typed
            currentOnResultCallback?.invoke(isGranted)
            currentOnResultCallback = null
        }
    )

    return remember {
        object : LocationPermissionController {
            override fun requestPermission(onResult: (isGranted: Boolean) -> Unit) {
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    onResult(true)
                } else {
                    currentOnResultCallback = onResult
                    launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }

            override fun isPermissionGranted(): Boolean {
                return ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
    }
}
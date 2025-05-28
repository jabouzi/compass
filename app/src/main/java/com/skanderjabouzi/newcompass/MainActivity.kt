// MainActivity.kt
package com.skanderjabouzi.newcompass

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

class MainActivity : ComponentActivity() {
    private lateinit var compassViewModel: CompassViewModel

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                compassViewModel.startLocationUpdates()
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                compassViewModel.startLocationUpdates()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        compassViewModel = CompassViewModel(this)

        setContent {
            CompassTheme {
                CompassScreen(compassViewModel)
            }
        }

        // Request location permissions
        locationPermissionRequest.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ))
    }

    override fun onResume() {
        super.onResume()
        compassViewModel.startSensorUpdates()
    }

    override fun onPause() {
        super.onPause()
        compassViewModel.stopSensorUpdates()
    }
}

@Composable
fun CompassScreen(viewModel: CompassViewModel) {
    // Animate rotation smoothly with adaptive speed
    val animatedRotation by animateFloatAsState(
        targetValue = -viewModel.azimuth,
        animationSpec = tween(
            durationMillis = if (viewModel.isStable) 800 else 200,
            easing = LinearOutSlowInEasing
        ),
        label = "compass_rotation"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Compass Display
        Box(
            modifier = Modifier
                .size(300.dp)
                .clip(CircleShape)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            // Compass Rose
            CompassRose(
                rotation = animatedRotation,
                modifier = Modifier.size(280.dp)
            )

            // Center dot
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(Color.Red)
            )

            // North indicator (fixed)
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                drawLine(
                    color = Color.Red,
                    start = androidx.compose.ui.geometry.Offset(size.width / 2, 20f),
                    end = androidx.compose.ui.geometry.Offset(size.width / 2, 60f),
                    strokeWidth = 6f
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Degree Display with stability indicator
        Card(
            modifier = Modifier.padding(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (viewModel.isStable) Color(0xFF2E7D32) else Color.DarkGray
            )
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "${viewModel.azimuth.roundToInt()}°",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (viewModel.isStable) {
                    Text(
                        text = "STABLE",
                        fontSize = 12.sp,
                        color = Color.Green,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Direction Text
        Text(
            text = getDirectionText(viewModel.azimuth),
            fontSize = 20.sp,
            color = Color.White,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Controls
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = { viewModel.toggleNorthType() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (viewModel.useTrueNorth) Color.Green else Color.Gray
                )
            ) {
                Text(if (viewModel.useTrueNorth) "True North" else "Magnetic North")
            }
        }

        // Location info
        if (viewModel.hasLocationPermission && viewModel.currentLocation != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Declination: ${viewModel.declination.roundToInt()}°",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
                Text(
                    text = "Lat: ${String.format("%.4f", viewModel.currentLocation?.latitude)}",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
                Text(
                    text = "Lon: ${String.format("%.4f", viewModel.currentLocation?.longitude)}",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun CompassRose(rotation: Float, modifier: Modifier = Modifier) {
    // Use your custom vector drawable
    val roseVector = ImageVector.vectorResource(id = R.drawable.ic_rose)

    Icon(
        imageVector = roseVector,
        contentDescription = "Compass Rose",
        modifier = modifier.rotate(rotation),
        tint = Color.White
    )
}

fun getDirectionText(azimuth: Float): String {
    return when (azimuth.roundToInt()) {
        in 0..11, in 349..360 -> "N"
        in 12..33 -> "NNE"
        in 34..56 -> "NE"
        in 57..78 -> "ENE"
        in 79..101 -> "E"
        in 102..123 -> "ESE"
        in 124..146 -> "SE"
        in 147..168 -> "SSE"
        in 169..191 -> "S"
        in 192..213 -> "SSW"
        in 214..236 -> "SW"
        in 237..258 -> "WSW"
        in 259..281 -> "W"
        in 282..303 -> "WNW"
        in 304..326 -> "NW"
        in 327..348 -> "NNW"
        else -> "N"
    }
}

@Composable
fun CompassTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(),
        content = content
    )
}
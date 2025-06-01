package com.skanderjabouzi.newcompass.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.skanderjabouzi.newcompass.Azimuth
import com.skanderjabouzi.newcompass.R

@Composable
fun CompassRose(
    azimuth: Azimuth,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val rotation = -azimuth.degrees
    val animatedRotation by animateFloatAsState(
        targetValue = rotation,
        animationSpec = tween(
            durationMillis = 100,
            easing = LinearEasing
        ),
        label = "compass_rotation"
    )

    Box(modifier = modifier) {
        // Compass Rose Image
        Image(
            painter = painterResource(R.drawable.ic_rose),
            contentDescription = "Compass Rose",
            modifier = Modifier
                .fillMaxSize()
                .rotate(animatedRotation)
        )

        // You can keep this or remove it if it's redundant.
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(Color.Red)
                .align(Alignment.Center)
        )

        Canvas(
            modifier = Modifier.fillMaxSize() // Canvas overlays CompassView and the dot
        ) {
            val centerWidth = this.size.width / 2
            drawLine(
                color = Color.Red,
                start = Offset(centerWidth, 0f), // Start at the top edge of the Canvas
                end = Offset(centerWidth, 60f),   // Length of the line
                strokeWidth = 6f
            )
        }

        // Cardinal Direction Labels
//        CompassLabels(
//            rotation = animatedRotation,
//            size = size,
//            modifier = Modifier.fillMaxSize()
//        )
    }
}
package com.skanderjabouzi.newcompass.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
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

        // Cardinal Direction Labels
        CompassLabels(
            rotation = animatedRotation,
            size = size,
            modifier = Modifier.fillMaxSize()
        )
    }
}
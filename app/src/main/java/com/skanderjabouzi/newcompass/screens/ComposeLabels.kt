package com.skanderjabouzi.newcompass.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CompassLabels(
    rotation: Float,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val cardinalTextSize = with(density) { (size * 0.08f).toSp() }
    val degreeTextSize = with(density) { (size * 0.04f).toSp() }

    Box(modifier = modifier) {
        // Cardinal directions
        val cardinalRadius = size * 0.35f
        listOf(
            "N" to 0f,
            "E" to 90f,
            "S" to 180f,
            "W" to 270f
        ).forEach { (label, angle) ->
            val finalAngle = rotation + angle
            val radians = Math.toRadians(finalAngle.toDouble())
            val x = (cardinalRadius.value * sin(radians))
            val y = (-cardinalRadius.value * cos(radians))

            Text(
                text = label,
                fontSize = cardinalTextSize,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .offset(
                        x = x.dp + size / 2 - cardinalTextSize.value.dp / 2,
                        y = y.dp + size / 2 - cardinalTextSize.value.dp / 2
                    )
            )
        }

        // Degree markers
        val degreeRadius = size * 0.4f
        (0..330 step 30).forEach { degree ->
            if (degree % 90 != 0) { // Skip cardinal directions
                val finalAngle = rotation + degree
                val radians = Math.toRadians(finalAngle.toDouble())
                val x = (degreeRadius.value * sin(radians))
                val y = (-degreeRadius.value * cos(radians))

                Text(
                    text = degree.toString(),
                    fontSize = degreeTextSize,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .offset(
                            x = x.dp + size / 2 - degreeTextSize.value.dp / 2,
                            y = y.dp + size / 2 - degreeTextSize.value.dp / 2
                        )
                )
            }
        }
    }
}
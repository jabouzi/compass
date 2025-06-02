package com.skanderjabouzi.newcompass.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.skanderjabouzi.newcompass.model.Azimuth
import com.skanderjabouzi.newcompass.model.LocationStatus
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator

@Composable
fun CompassStatus(
    azimuth: Azimuth?,
    locationStatus: LocationStatus,
    onLocationReloadClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        azimuth?.let {
            Text(
                modifier = Modifier.padding(bottom = 2.dp),
                text = "${it.roundedDegrees}°",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                modifier = Modifier.padding(top = 2.dp),
                text = stringResource(it.cardinalDirection.labelResourceId),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Location status indicator
        when (locationStatus) {
            LocationStatus.LOADING -> {
                Spacer(modifier = Modifier.height(8.dp))
                CircularProgressIndicator(modifier = Modifier.size(16.dp))
            }
            LocationStatus.NOT_PRESENT, LocationStatus.PERMISSION_DENIED -> {
                Spacer(modifier = Modifier.height(8.dp))
                IconButton(onClick = onLocationReloadClick) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Reload Location", // Consider using stringResource
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            LocationStatus.PRESENT -> {
                // Location is available, no indicator needed
            }
        }
    }
}
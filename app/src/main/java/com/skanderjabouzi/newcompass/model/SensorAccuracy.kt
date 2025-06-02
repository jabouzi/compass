package com.skanderjabouzi.newcompass.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.skanderjabouzi.newcompass.R

enum class SensorAccuracy(
    @StringRes val textResourceId: Int,
    @DrawableRes val iconResourceId: Int
) {
    NO_CONTACT(
        R.string.sensor_accuracy_no_contact,
        R.drawable.ic_sensor_no_signal
    ),
    UNRELIABLE(
        R.string.sensor_accuracy_unreliable,
        R.drawable.ic_sensor_unreliable_signal
    ),
    LOW(
        R.string.sensor_accuracy_low,
        R.drawable.ic_sensor_low_signal
    ),
    MEDIUM(
        R.string.sensor_accuracy_medium,
        R.drawable.ic_sensor_medium_signal
    ),
    HIGH(
        R.string.sensor_accuracy_high,
        R.drawable.ic_sensor_high_signal
    )
}

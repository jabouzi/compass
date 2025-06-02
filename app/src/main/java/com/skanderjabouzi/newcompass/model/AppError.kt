package com.skanderjabouzi.newcompass.model

import androidx.annotation.StringRes
import com.skanderjabouzi.newcompass.R

enum class AppError(@StringRes val messageId: Int) {
    SENSOR_MANAGER_NOT_PRESENT(R.string.sensor_error_message),
    ROTATION_VECTOR_SENSOR_NOT_AVAILABLE(R.string.sensor_error_message),
    ROTATION_VECTOR_SENSOR_FAILED(R.string.sensor_error_message),
    MAGNETIC_FIELD_SENSOR_NOT_AVAILABLE(R.string.sensor_error_message),
    MAGNETIC_FIELD_SENSOR_FAILED(R.string.sensor_error_message),
    LOCATION_MANAGER_NOT_PRESENT(R.string.location_error_message),
    LOCATION_DISABLED(R.string.location_error_message),
    NO_LOCATION_PROVIDER_AVAILABLE(R.string.location_error_message)
}

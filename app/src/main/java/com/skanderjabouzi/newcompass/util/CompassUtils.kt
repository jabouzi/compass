package com.skanderjabouzi.newcompass.util

import android.app.Activity
import android.content.Context
import android.view.HapticFeedbackConstants
import android.view.View
import com.skanderjabouzi.newcompass.model.Azimuth

fun handleHapticFeedback(
    context: Context,
    azimuth: Azimuth,
    lastHapticFeedbackPoint: Azimuth?,
    onUpdateLastPoint: (Azimuth) -> Unit
) {
    val hapticFeedbackInterval = 2.0f

    lastHapticFeedbackPoint?.let { lastPoint ->
        val boundaryStart = lastPoint - hapticFeedbackInterval
        val boundaryEnd = lastPoint + hapticFeedbackInterval

        if (!MathUtils.isAzimuthBetweenTwoPoints(azimuth, boundaryStart, boundaryEnd)) {
            val closestIntervalPoint = MathUtils.getClosestNumberFromInterval(azimuth.degrees, hapticFeedbackInterval)
            onUpdateLastPoint(Azimuth(closestIntervalPoint))

            // Trigger haptic feedback
            (context as? Activity)?.let { activity ->
                activity.findViewById<View>(android.R.id.content)
                    ?.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            }
        }
    } ?: run {
        val closestIntervalPoint = MathUtils.getClosestNumberFromInterval(azimuth.degrees, hapticFeedbackInterval)
        onUpdateLastPoint(Azimuth(closestIntervalPoint))
    }
}
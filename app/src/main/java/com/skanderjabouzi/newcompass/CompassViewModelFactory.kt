package com.skanderjabouzi.newcompass

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class CompassViewModelFactory(private val applicationContext: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CompassViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CompassViewModel(applicationContext) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
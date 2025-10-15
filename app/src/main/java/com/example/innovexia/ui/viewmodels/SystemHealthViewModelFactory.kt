package com.example.innovexia.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.innovexia.core.health.RealHealthApi
import com.example.innovexia.data.local.AppDatabase

/**
 * Factory for SystemHealthViewModel
 */
class SystemHealthViewModelFactory(
    private val healthApi: RealHealthApi,
    private val database: AppDatabase
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SystemHealthViewModel::class.java)) {
            return SystemHealthViewModel(healthApi, database) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

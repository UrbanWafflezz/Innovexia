package com.example.innovexia.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.innovexia.data.repository.SubscriptionRepository
import com.example.innovexia.data.repository.UsageRepository

/**
 * Factory for creating SubscriptionViewModel with dependencies
 */
class SubscriptionViewModelFactory(
    private val subscriptionRepository: SubscriptionRepository,
    private val usageRepository: UsageRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SubscriptionViewModel::class.java)) {
            return SubscriptionViewModel(subscriptionRepository, usageRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

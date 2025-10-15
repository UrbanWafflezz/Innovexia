package com.example.innovexia.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.innovexia.data.ai.GeminiService
import com.example.innovexia.data.preferences.UserPreferences
import com.example.innovexia.data.repository.ChatRepository
import com.example.innovexia.core.persona.PersonaRepository

/**
 * Factory for creating HomeViewModel with dependencies.
 */
class HomeViewModelFactory(
    private val chatRepository: ChatRepository,
    private val userPreferences: UserPreferences,
    private val geminiService: GeminiService,
    private val personaRepository: PersonaRepository,
    private val subscriptionViewModel: SubscriptionViewModel,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(
                chatRepository,
                userPreferences,
                geminiService,
                personaRepository,
                subscriptionViewModel,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

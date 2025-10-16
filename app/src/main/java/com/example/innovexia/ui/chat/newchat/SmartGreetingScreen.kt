package com.example.innovexia.ui.chat.newchat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.innovexia.R
import com.example.innovexia.ui.chat.newchat.components.GreetingBackground
import com.example.innovexia.ui.chat.newchat.suggestions.SuggestionCardUi
import com.example.innovexia.ui.chat.newchat.suggestions.SuggestionCards
import com.example.innovexia.ui.chat.newchat.suggestions.SuggestionsVM

/**
 * Smart Greeting Screen - Memory-Aware + Gemini Integration
 *
 * Features:
 * - Personalized greeting based on time of day, persona, and memory
 * - Smart suggestion cards from memories, sources, and defaults
 * - Beautiful fade + slide animations
 * - Dark glass aesthetic matching Innovexia design
 * - Fully offline-capable with graceful fallbacks
 */
@Composable
fun SmartGreetingScreen(
    persona: com.example.innovexia.core.persona.Persona?,
    onSuggestionClicked: (SuggestionCardUi) -> Unit,
    modifier: Modifier = Modifier,
    greetingVM: SmartGreetingVM = hiltViewModel(),
    suggestionsVM: SuggestionsVM = hiltViewModel()
) {
    val greetingState by greetingVM.uiState.collectAsState()
    val suggestionCards by suggestionsVM.ui.collectAsState()
    val isLoading by suggestionsVM.isLoading.collectAsState()

    // Monitor auth state changes in real-time (don't cache with remember)
    val auth = remember { com.google.firebase.auth.FirebaseAuth.getInstance() }
    var isGuest by remember { mutableStateOf(auth.currentUser == null) }

    // Listen to auth state changes
    DisposableEffect(Unit) {
        val authStateListener = com.google.firebase.auth.FirebaseAuth.AuthStateListener { firebaseAuth ->
            val wasGuest = isGuest
            val nowGuest = firebaseAuth.currentUser == null

            if (wasGuest != nowGuest) {
                android.util.Log.d("SmartGreetingScreen", "🔄 Auth state changed - wasGuest: $wasGuest, nowGuest: $nowGuest")
                isGuest = nowGuest
            }
        }
        auth.addAuthStateListener(authStateListener)

        onDispose {
            auth.removeAuthStateListener(authStateListener)
        }
    }

    // Load greeting on first composition
    LaunchedEffect(Unit) {
        android.util.Log.d("SmartGreetingScreen", "🎯 Initial load - persona: ${persona?.name}, isGuest: $isGuest")
        greetingVM.loadGreetingWithPersona(persona)
    }

    // Load/reload suggestions when auth state or persona changes
    LaunchedEffect(isGuest, persona) {
        android.util.Log.d("SmartGreetingScreen", "🔄 Reloading suggestions - isGuest: $isGuest, persona: ${persona?.name}")

        if (isGuest) {
            // Guest mode - show default suggestions only (no memory)
            android.util.Log.d("SmartGreetingScreen", "🚫 Guest mode - showing default suggestions")
            suggestionsVM.clearSuggestions()
        } else {
            // Logged in user - load memory-based suggestions
            android.util.Log.d("SmartGreetingScreen", "✅ Authenticated - loading memory-based suggestions")
            suggestionsVM.loadWithPersona(persona)
        }
    }

    // Debug: Log when composable recomposes
    android.util.Log.d("SmartGreetingScreen", "🔄 Recomposing SmartGreetingScreen - persona: ${persona?.name}")

    GreetingBackground(modifier = modifier) {
        SmartGreetingContent(
            greeting = greetingState.greeting,
            suggestionCards = suggestionCards,
            isLoading = isLoading,
            onSuggestionClicked = onSuggestionClicked,
            onRefresh = { suggestionsVM.refreshWithPersona() }
        )
    }
}

/**
 * Main content of the greeting screen with card-based suggestions
 */
@Composable
private fun SmartGreetingContent(
    greeting: String,
    suggestionCards: List<SuggestionCardUi>,
    isLoading: Boolean,
    onSuggestionClicked: (SuggestionCardUi) -> Unit,
    onRefresh: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(800)) + slideInVertically(
            animationSpec = tween(800),
            initialOffsetY = { it / 2 }
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Compact personalized greeting - no icon
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 18.sp,
                        lineHeight = 24.sp
                    ),
                    color = Color(0xFFE9EEF6),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                )

                Spacer(Modifier.height(8.dp))

                // Smart suggestion cards (new card-based UI)
                SuggestionCards(
                    items = suggestionCards,
                    isLoading = isLoading,
                    onClick = onSuggestionClicked,
                    onRefresh = onRefresh,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

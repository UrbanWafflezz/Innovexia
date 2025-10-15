package com.example.innovexia.ui.chat.newchat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Background gradient for the smart greeting screen.
 * Deep gray to navy vertical gradient matching the Innovexia dark aesthetic.
 */
@Composable
fun GreetingBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0E0E11), // Deep gray
                        Color(0xFF1A1A1F)  // Navy
                    )
                )
            )
    ) {
        content()
    }
}

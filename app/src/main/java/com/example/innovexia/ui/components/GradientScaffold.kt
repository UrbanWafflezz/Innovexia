package com.example.innovexia.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.innovexia.ui.theme.getBackgroundGradient

@Composable
fun GradientScaffold(
    modifier: Modifier = Modifier,
    darkTheme: Boolean = isSystemInDarkTheme(),
    applySystemBarsPadding: Boolean = true,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(brush = getBackgroundGradient(darkTheme))
            .then(
                if (applySystemBarsPadding) {
                    Modifier.windowInsetsPadding(WindowInsets.systemBars)
                } else {
                    Modifier
                }
            )
    ) {
        content()
    }
}

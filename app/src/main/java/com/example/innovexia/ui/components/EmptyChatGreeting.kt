package com.example.innovexia.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.innovexia.ui.theme.InnovexiaTheme
import java.util.Calendar

/**
 * EmptyChatGreeting - Shows a dynamic greeting with quick action chips
 * when the chat is empty.
 *
 * Features:
 * - Dynamic greeting based on time of day (morning/afternoon/evening)
 * - Burst/asterisk decoration above title
 * - Quick action chips that prefill the composer
 * - Respects window insets for small screens
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EmptyChatGreeting(
    onQuickActionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    userName: String? = null,
    darkTheme: Boolean = isSystemInDarkTheme()
) {
    val haptic = LocalHapticFeedback.current

    // Determine greeting based on time of day
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when {
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    val greetingText = if (userName != null) {
        "How can Innovexia help you this $greeting, $userName?"
    } else {
        "How can Innovexia help you this $greeting?"
    }

    // Quick action prompts
    val quickActions = listOf(
        "Research" to "Help me research a topic",
        "Brainstorm ideas" to "Help me brainstorm ideas for my project",
        "Draft an email" to "Help me draft a professional email",
        "Summarize a PDF" to "Summarize the key points from a document",
        "Create a plan" to "Help me create a step-by-step plan"
    )

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            // Burst/asterisk decoration
            Text(
                text = "✨",
                fontSize = 48.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Dynamic greeting title
            Text(
                text = greetingText,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 36.sp
                ),
                color = if (darkTheme) Color(0xFFE5E7EB) else Color(0xFF111827),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            // Quick action chips
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                quickActions.forEach { (label, prompt) ->
                    QuickActionChip(
                        label = label,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onQuickActionClick(prompt)
                        },
                        darkTheme = darkTheme
                    )
                }
            }
        }
    }
}

/**
 * A glass/elevated pill chip for quick actions
 */
@Composable
private fun QuickActionChip(
    label: String,
    onClick: () -> Unit,
    darkTheme: Boolean
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (darkTheme) {
                    Color(0xFF1F2937).copy(alpha = 0.8f)
                } else {
                    Color(0xFFFFFFFF).copy(alpha = 0.9f)
                }
            )
            .clickable(
                onClick = onClick,
                role = Role.Button
            )
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            ),
            color = if (darkTheme) Color(0xFFE5E7EB) else Color(0xFF374151)
        )
    }
}

@Preview(name = "EmptyChatGreeting Light", showBackground = true)
@Composable
fun EmptyChatGreetingPreview_Light() {
    InnovexiaTheme(darkTheme = false) {
        EmptyChatGreeting(
            onQuickActionClick = {},
            darkTheme = false
        )
    }
}

@Preview(name = "EmptyChatGreeting Light with Name", showBackground = true)
@Composable
fun EmptyChatGreetingPreview_LightWithName() {
    InnovexiaTheme(darkTheme = false) {
        EmptyChatGreeting(
            onQuickActionClick = {},
            userName = "Alex",
            darkTheme = false
        )
    }
}

@Preview(name = "EmptyChatGreeting Dark", showBackground = true, backgroundColor = 0xFF111827)
@Composable
fun EmptyChatGreetingPreview_Dark() {
    InnovexiaTheme(darkTheme = true) {
        EmptyChatGreeting(
            onQuickActionClick = {},
            darkTheme = true
        )
    }
}

@Preview(name = "EmptyChatGreeting Dark with Name", showBackground = true, backgroundColor = 0xFF111827)
@Composable
fun EmptyChatGreetingPreview_DarkWithName() {
    InnovexiaTheme(darkTheme = true) {
        EmptyChatGreeting(
            onQuickActionClick = {},
            userName = "Alex",
            darkTheme = true
        )
    }
}

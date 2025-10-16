package com.example.innovexia.ui.persona.memory

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.example.innovexia.ui.theme.InnovexiaColors

/**
 * Floating search bar for filtering memories
 * Appears at the top when scrolled
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MemorySearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    darkTheme: Boolean = isSystemInDarkTheme()
) {
    BasicTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(if (darkTheme) Color(0xFF1E2530) else Color(0xFFF5F5F5)),
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            color = if (darkTheme) InnovexiaColors.DarkTextPrimary else InnovexiaColors.LightTextPrimary
        ),
        cursorBrush = SolidColor(if (darkTheme) InnovexiaColors.GoldDim else InnovexiaColors.Gold),
        singleLine = true,
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = if (darkTheme) InnovexiaColors.DarkTextSecondary else InnovexiaColors.LightTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
                Box(modifier = Modifier.weight(1f)) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "Search memories…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (darkTheme) InnovexiaColors.DarkTextSecondary else InnovexiaColors.LightTextSecondary
                        )
                    }
                    innerTextField()
                }
                AnimatedVisibility(
                    visible = searchQuery.isNotEmpty(),
                    enter = fadeIn(animationSpec = tween(180)) + scaleIn(animationSpec = tween(180)),
                    exit = fadeOut(animationSpec = tween(180)) + scaleOut(animationSpec = tween(180))
                ) {
                    IconButton(
                        onClick = { onSearchQueryChange("") },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search",
                            tint = if (darkTheme) InnovexiaColors.DarkTextSecondary else InnovexiaColors.LightTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    )
}

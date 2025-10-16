package com.example.innovexia.ui.persona.memory

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.innovexia.ui.theme.InnovexiaColors

/**
 * Category filter tabs for memory items
 * Shows pill-style selector with smooth transitions
 */
@Composable
fun MemoryCategoryTabs(
    selectedCategory: MemoryCategory,
    onCategorySelected: (MemoryCategory) -> Unit,
    modifier: Modifier = Modifier,
    darkTheme: Boolean = isSystemInDarkTheme()
) {
    val categories = MemoryCategory.values().toList()

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(categories) { category ->
            CategoryChip(
                category = category,
                isSelected = category == selectedCategory,
                onClick = { onCategorySelected(category) },
                darkTheme = darkTheme
            )
        }
    }
}

/**
 * Individual category chip with selection state
 */
@Composable
private fun CategoryChip(
    category: MemoryCategory,
    isSelected: Boolean,
    onClick: () -> Unit,
    darkTheme: Boolean
) {
    val backgroundColor by animateDpAsState(
        targetValue = if (isSelected) 4.dp else 0.dp,
        animationSpec = tween(durationMillis = 180),
        label = "chip_background"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) {
            if (darkTheme) InnovexiaColors.GoldDim.copy(alpha = 0.2f) else InnovexiaColors.Gold.copy(alpha = 0.15f)
        } else {
            if (darkTheme) Color(0xFF1E2530) else Color(0xFFF5F5F5)
        },
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(
                1.5.dp,
                if (darkTheme) InnovexiaColors.GoldDim else InnovexiaColors.Gold
            )
        } else {
            null
        },
        modifier = Modifier.height(36.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = category.emoji,
                fontSize = 14.sp
            )
            Text(
                text = category.displayName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (isSelected) {
                    if (darkTheme) InnovexiaColors.GoldDim else InnovexiaColors.Gold
                } else {
                    if (darkTheme) InnovexiaColors.DarkTextPrimary else InnovexiaColors.LightTextPrimary
                },
                fontSize = 13.sp
            )
        }
    }
}

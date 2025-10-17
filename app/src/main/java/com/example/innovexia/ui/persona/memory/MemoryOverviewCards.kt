package com.example.innovexia.ui.persona.memory

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.innovexia.ui.theme.InnovexiaColors

/**
 * Overview cards showing memory category summaries
 * Glass-morphism style cards with category counts
 */
@Composable
fun MemoryOverviewCards(
    categorySummaries: List<CategorySummary>,
    onCategoryClick: (MemoryCategory) -> Unit,
    modifier: Modifier = Modifier,
    darkTheme: Boolean = isSystemInDarkTheme()
) {
    if (categorySummaries.isEmpty()) {
        return
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Memory Overview",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (darkTheme) InnovexiaColors.DarkTextPrimary else InnovexiaColors.LightTextPrimary,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(categorySummaries) { summary ->
                CategorySummaryCard(
                    summary = summary,
                    onClick = { onCategoryClick(summary.category) },
                    darkTheme = darkTheme
                )
            }
        }
    }
}

/**
 * Individual category summary card with improved Material 3 design
 */
@Composable
private fun CategorySummaryCard(
    summary: CategorySummary,
    onClick: () -> Unit,
    darkTheme: Boolean
) {
    val gradientColors = if (darkTheme) {
        listOf(
            Color(0xFF1E2530).copy(alpha = 0.9f),
            Color(0xFF141A22).copy(alpha = 0.7f)
        )
    } else {
        listOf(
            Color.White.copy(alpha = 0.95f),
            Color(0xFFF8FAFC).copy(alpha = 0.85f)
        )
    }

    val accentColor = when (summary.category) {
        MemoryCategory.Facts -> if (darkTheme) Color(0xFF60A5FA) else Color(0xFF3B82F6)
        MemoryCategory.Events -> if (darkTheme) Color(0xFF34D399) else Color(0xFF10B981)
        MemoryCategory.Preferences -> if (darkTheme) Color(0xFFA78BFA) else Color(0xFF8B5CF6)
        MemoryCategory.Emotions -> if (darkTheme) Color(0xFFF472B6) else Color(0xFFEC4899)
        MemoryCategory.Projects -> if (darkTheme) Color(0xFFFBBF24) else Color(0xFFF59E0B)
        MemoryCategory.Knowledge -> if (darkTheme) Color(0xFF6366F1) else Color(0xFF4F46E5)
        else -> if (darkTheme) InnovexiaColors.GoldDim else InnovexiaColors.Gold
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent,
        border = BorderStroke(
            1.5.dp,
            Brush.linearGradient(
                colors = listOf(
                    accentColor.copy(alpha = 0.4f),
                    accentColor.copy(alpha = 0.15f)
                )
            )
        ),
        modifier = Modifier
            .width(150.dp)
            .height(120.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(gradientColors)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Category icon and name
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = summary.category.emoji,
                        fontSize = 28.sp
                    )
                    Text(
                        text = summary.category.displayName,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = if (darkTheme) InnovexiaColors.DarkTextPrimary else InnovexiaColors.LightTextPrimary,
                        fontSize = 13.sp,
                        maxLines = 1
                    )
                }

                // Count section
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "${summary.count}",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = accentColor,
                        fontSize = 32.sp
                    )
                    Text(
                        text = if (summary.count == 1) "memory" else "memories",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (darkTheme) InnovexiaColors.DarkTextSecondary else InnovexiaColors.LightTextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

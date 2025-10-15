package com.example.innovexia.ui.chat.newchat.suggestions

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.innovexia.R

/**
 * Card-based smart suggestions UI with refresh button
 */
@Composable
fun SuggestionCards(
    items: List<SuggestionCardUi>,
    isLoading: Boolean,
    onClick: (SuggestionCardUi) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Compact header with refresh button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp, start = 2.dp, end = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // More informative label
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Quick suggestions",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFFFFFFF),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                Text(
                    text = if (isLoading) "Generating..." else "Based on your memories",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0x99FFFFFF),
                    fontSize = 11.sp
                )
            }

            // Compact refresh button with loading animation
            val infiniteTransition = rememberInfiniteTransition(label = "refresh")
            val rotation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "refresh_rotation"
            )

            IconButton(
                onClick = onRefresh,
                enabled = !isLoading,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_refresh),
                    contentDescription = "Refresh suggestions",
                    tint = if (isLoading) Color(0xFFE7C879) else Color(0xCCFFFFFF),
                    modifier = Modifier
                        .size(18.dp)
                        .then(
                            if (isLoading) Modifier.graphicsLayer { rotationZ = rotation }
                            else Modifier
                        )
                )
            }
        }

        // Suggestion cards grid - show only 4 cards maximum
        LazyVerticalGrid(
            columns = GridCells.Fixed(2), // 2 columns = 2x2 grid
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp) // Optimized height for 2 rows
        ) {
            items(items.take(4), key = { it.id }) { suggestion -> // Limit to 4 cards
                SuggestionCard(
                    suggestion = suggestion,
                    onClick = { onClick(suggestion) },
                    modifier = Modifier
                        .animateItem(
                            fadeInSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
                            fadeOutSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
                            placementSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        )
                        .height(130.dp) // Slightly taller for centered content
                )
            }
        }
    }
}

/**
 * Individual suggestion card with clean, modern design
 */
@Composable
private fun SuggestionCard(
    suggestion: SuggestionCardUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Smooth scale and elevation animation on press
    val interactionSource = MutableInteractionSource()
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "card_scale"
    )

    val elevation by animateDpAsState(
        targetValue = if (isPressed) 0.dp else 2.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "card_elevation"
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF2D3139), // Single solid shade - dark gray
        border = BorderStroke(1.dp, Color(0x1AFFFFFF)),
        tonalElevation = elevation,
        shadowElevation = elevation,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                onClick = onClick,
                interactionSource = interactionSource,
                indication = null
            )
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        // Card with header icon and content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header - Icon with background circle
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF3A3F4A)), // Slightly lighter shade for icon background
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(suggestion.icon),
                    contentDescription = null,
                    tint = Color(0xFFFFFFFF),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Content section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                // Title
                Text(
                    text = suggestion.title,
                    color = Color(0xFFFFFFFF),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        lineHeight = 19.sp
                    )
                )

                // Subtitle
                if (!suggestion.subtitle.isNullOrBlank()) {
                    Text(
                        text = suggestion.subtitle,
                        color = Color(0x99FFFFFF), // 60% opacity white
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            lineHeight = 14.sp
                        )
                    )
                }
            }
        }
    }
}


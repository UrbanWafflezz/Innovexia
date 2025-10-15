package com.example.innovexia.ui.persona.sources

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.innovexia.ui.theme.DarkColors
import com.example.innovexia.ui.theme.InnovexiaColors
import com.example.innovexia.ui.theme.LightColors

/**
 * Compact header for Sources tab with collapsible info banner
 */
@Composable
fun SourcesHeader(
    personaName: String,
    personaColor: Color,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    darkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    var bannerExpanded by remember { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (bannerExpanded) 180f else 0f,
        label = "expand_rotation"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Compact header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Compact avatar + name
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(personaColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = personaName.take(1).uppercase(),
                        color = personaColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Text(
                    text = personaName,
                    color = if (darkTheme) DarkColors.PrimaryText else LightColors.PrimaryText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Compact switch
            Switch(
                checked = enabled,
                onCheckedChange = onEnabledChange,
                modifier = Modifier.height(24.dp),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = if (darkTheme) InnovexiaColors.GoldDim else InnovexiaColors.Gold,
                    checkedTrackColor = if (darkTheme) InnovexiaColors.GoldDim.copy(alpha = 0.5f) else InnovexiaColors.Gold.copy(alpha = 0.5f),
                    uncheckedThumbColor = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText,
                    uncheckedTrackColor = if (darkTheme) DarkColors.SecondaryText.copy(alpha = 0.3f) else LightColors.SecondaryText.copy(alpha = 0.3f)
                )
            )
        }

        // Collapsible info banner (only when enabled)
        if (enabled) {
            Column {
                // Banner header - clickable to expand/collapse
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (darkTheme) {
                                InnovexiaColors.GoldDim.copy(alpha = 0.1f)
                            } else {
                                InnovexiaColors.Gold.copy(alpha = 0.1f)
                            }
                        )
                        .clickable { bannerExpanded = !bannerExpanded }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = null,
                            tint = if (darkTheme) InnovexiaColors.GoldDim else InnovexiaColors.Gold,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Local sources",
                            color = if (darkTheme) DarkColors.PrimaryText else LightColors.PrimaryText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Icon(
                        imageVector = Icons.Rounded.ExpandMore,
                        contentDescription = if (bannerExpanded) "Collapse" else "Expand",
                        tint = if (darkTheme) InnovexiaColors.GoldDim else InnovexiaColors.Gold,
                        modifier = Modifier
                            .size(18.dp)
                            .rotate(rotationAngle)
                    )
                }

                // Expandable content
                AnimatedVisibility(
                    visible = bannerExpanded,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Files, images, and links added here are stored and processed on this device. You can change this later in Settings.",
                            color = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

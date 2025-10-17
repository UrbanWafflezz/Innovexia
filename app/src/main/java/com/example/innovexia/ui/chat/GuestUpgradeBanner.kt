package com.example.innovexia.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.People
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.innovexia.ui.theme.DarkColors
import com.example.innovexia.ui.theme.InnovexiaColors
import com.example.innovexia.ui.theme.LightColors

/**
 * Compact Material 3 banner encouraging guest users to register or log in.
 * Features press animations and matches auth screen design.
 */
@Composable
fun GuestUpgradeBanner(
    onSignUp: () -> Unit,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(true) }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut(),
        modifier = modifier
    ) {
        val isDark = isSystemInDarkTheme()

        // Colors matching app's side menu and background gradient
        val accentColor = if (isDark) Color(0xFF60A5FA) else Color(0xFF3B82F6)
        // Use the exact gradient colors from the app background
        val backgroundColor = if (isDark) {
            InnovexiaColors.DarkGradientStart  // #171A1E
        } else {
            InnovexiaColors.LightGradientEnd   // #FFFFFF
        }
        val surfaceOverlay = if (isDark) {
            InnovexiaColors.DarkGradientMid.copy(alpha = 0.4f)  // #1E2329 with transparency
        } else {
            InnovexiaColors.LightGradientMid.copy(alpha = 0.3f)  // #EAF6FF with transparency
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = backgroundColor
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 4.dp
            )
        ) {
            Box(
                modifier = Modifier
                    .background(
                        color = surfaceOverlay,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .border(
                        width = 0.5.dp,
                        color = accentColor.copy(alpha = if (isDark) 0.3f else 0.2f),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .padding(12.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Compact header with close button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sign in to unlock features",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) DarkColors.PrimaryText else LightColors.PrimaryText
                        )
                        IconButton(
                            onClick = { isVisible = false },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Dismiss",
                                tint = if (isDark) DarkColors.SecondaryText else LightColors.SecondaryText,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // Compact features list with icons
                    val features = listOf(
                        Triple(Icons.Rounded.People, "Multiple AI personas", "Switch between different AI personalities"),
                        Triple(Icons.Rounded.Mic, "Voice chat", "Hands-free conversations"),
                        Triple(Icons.Rounded.Cloud, "Cloud sync", "Access chats anywhere")
                    )

                    features.forEach { (icon, title, _) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                color = if (isDark) DarkColors.PrimaryText else LightColors.PrimaryText
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Action buttons with press animations
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AnimatedButton(
                            onClick = onSignUp,
                            modifier = Modifier.weight(1f).height(44.dp),
                            isPrimary = true,
                            isDark = isDark
                        ) {
                            Text(
                                text = "Sign Up",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                        }

                        AnimatedButton(
                            onClick = onSignIn,
                            modifier = Modifier.weight(1f).height(44.dp),
                            isPrimary = false,
                            isDark = isDark
                        ) {
                            Text(
                                text = "Log In",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = accentColor
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Animated button with press scale and elevation effects
 */
@Composable
private fun AnimatedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean,
    isDark: Boolean,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        label = "button_scale"
    )

    val accentColor = if (isDark) Color(0xFF60A5FA) else Color(0xFF3B82F6)

    if (isPrimary) {
        Button(
            onClick = onClick,
            modifier = modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = accentColor,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(14.dp),
            interactionSource = interactionSource,
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp
            ),
            content = content
        )
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
            border = BorderStroke(
                1.dp,
                accentColor
            ),
            shape = RoundedCornerShape(14.dp),
            interactionSource = interactionSource,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = accentColor
            ),
            content = content
        )
    }
}

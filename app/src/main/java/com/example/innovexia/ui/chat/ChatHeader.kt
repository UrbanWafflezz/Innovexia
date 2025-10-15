package com.example.innovexia.ui.chat

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import com.example.innovexia.R
import com.example.innovexia.ui.theme.InnovexiaTokens

/**
 * Chat header with refined dark mode design and adaptive New Chat/Incognito button.
 *
 * Features:
 * - Premium dark gray elevated surface
 * - Animated button swap (New Chat ↔ Incognito)
 * - 60/90/120 Hz optimized transitions
 * - Subtle borders and proper spacing
 */
@Composable
fun ChatHeader(
    title: String,
    isNewChat: Boolean,
    onNewChat: () -> Unit,
    onIncognito: () -> Unit,
    onOpenMenu: () -> Unit,
    modifier: Modifier = Modifier,
    isIncognitoActive: Boolean = false,
    onIncognitoOff: () -> Unit = {},
    onTitleClick: (() -> Unit)? = null,
    isGuest: Boolean = false
) {
    // Match the gradient background used by GradientScaffold
    val isDark = isSystemInDarkTheme()
    val surfaceColor = if (isDark) {
        com.example.innovexia.ui.theme.InnovexiaColors.DarkGradientStart
    } else {
        com.example.innovexia.ui.theme.InnovexiaColors.LightGradientStart
    }
    val iconTint = MaterialTheme.colorScheme.onBackground
    val textColor = MaterialTheme.colorScheme.onBackground

    Surface(
        color = surfaceColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(
                    horizontal = InnovexiaTokens.Space.M,
                    vertical = InnovexiaTokens.Space.XS
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left menu button - smaller
            IconButton(
                onClick = onOpenMenu,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Menu,
                    contentDescription = "Menu",
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.width(8.dp))

            // Title with glass background - clickable if onTitleClick provided and not guest
            if (onTitleClick != null && !isGuest) {
                // Glass background with subtle gradient
                val glassTint = if (isDark) {
                    Color.White.copy(alpha = 0.08f)
                } else {
                    Color.White.copy(alpha = 0.72f)
                }

                val glassBorder = if (isDark) {
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.15f),
                            Color.White.copy(alpha = 0.05f)
                        )
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.8f),
                            Color.White.copy(alpha = 0.4f)
                        )
                    )
                }

                // Wrapper with weight to control expansion
                Box(modifier = Modifier.weight(1f)) {
                    // Glass pill centered or left-aligned, never fills full width
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .widthIn(max = 280.dp) // Max width constraint
                            .clip(RoundedCornerShape(20.dp))
                            .border(1.dp, glassBorder, RoundedCornerShape(20.dp))
                            .background(glassTint)
                            .drawWithCache {
                                onDrawBehind {
                                    // Subtle specular highlight
                                    val highlight = Brush.radialGradient(
                                        colors = if (isDark) {
                                            listOf(
                                                Color.White.copy(alpha = 0.08f),
                                                Color.Transparent
                                            )
                                        } else {
                                            listOf(
                                                Color.White.copy(alpha = 0.5f),
                                                Color.Transparent
                                            )
                                        },
                                        center = Offset(size.width * 0.3f, size.height * 0.3f),
                                        radius = size.width * 0.4f
                                    )
                                    drawRect(highlight)
                                }
                            }
                            .clickable { onTitleClick() }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = title.ifEmpty { "New chat" },
                                color = textColor,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Outlined.KeyboardArrowDown,
                                contentDescription = "Open model settings",
                                tint = textColor.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            } else {
                // Non-clickable title (original behavior)
                Text(
                    text = title.ifEmpty { "New chat" },
                    color = textColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .weight(1f)
                        .basicMarquee(iterations = Int.MAX_VALUE),
                    maxLines = 1
                )
            }

            Spacer(Modifier.width(8.dp))

            // Right action button with animated swap (hidden for guests)
            // Priority: Incognito active > New Chat > Incognito (inactive)
            if (!isGuest) {
                val targetIcon = when {
                    isIncognitoActive -> "incognito_active"
                    isNewChat -> "incognito"
                    else -> "new"
                }
                Crossfade(
                    targetState = targetIcon,
                    animationSpec = tween(
                        durationMillis = InnovexiaTokens.Motion.Fast,
                        easing = LinearOutSlowInEasing
                    ),
                    label = "headerActionSwap"
                ) { state ->
                    when (state) {
                        "incognito_active" -> GlassIconButton(
                            icon = painterResource(R.drawable.ic_incognito_24),
                            contentDesc = "Incognito (Local Only)",
                            onClick = { /* No action - incognito is permanent */ },
                            showAccentDot = true // Show dot to indicate active state
                        )
                        "incognito" -> GlassIconButton(
                            icon = painterResource(R.drawable.ic_incognito_24),
                            contentDesc = "Enable Incognito",
                            onClick = onIncognito,
                            showAccentDot = false
                        )
                        else -> GlassIconButton(
                            icon = painterResource(R.drawable.ic_add_chat_24),
                            contentDesc = "New Chat",
                            onClick = onNewChat,
                            showAccentDot = true
                        )
                    }
                }
            }
        }
    }
}

/**
 * Legacy ChatHeader overload for backward compatibility.
 * Delegates to simplified version without isNewChat state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatHeader(
    title: String,
    subtitle: String? = null,
    onBack: () -> Unit,
    onRename: ((String) -> Unit)? = null,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showRenameDialog by remember { mutableStateOf(false) }

    TopAppBar(
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
        },
        title = {
            // Centered title with subtitle
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title.ifEmpty { "New chat" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .clickable(enabled = onRename != null) {
                            showRenameDialog = true
                        }
                        .basicMarquee()
                )

                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        actions = {
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Menu"
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface
        )
    )

    // Rename dialog
    if (showRenameDialog && onRename != null) {
        RenameDialog(
            currentTitle = title,
            onDismiss = { showRenameDialog = false },
            onConfirm = { newTitle ->
                onRename(newTitle)
                showRenameDialog = false
            }
        )
    }
}

@Composable
private fun RenameDialog(
    currentTitle: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newTitle by remember { mutableStateOf(currentTitle) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Rename Chat")
        },
        text = {
            OutlinedTextField(
                value = newTitle,
                onValueChange = { newTitle = it },
                label = { Text("Chat title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(newTitle) },
                enabled = newTitle.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Preview(name = "Chat Header")
@Composable
private fun ChatHeaderPreview() {
    MaterialTheme {
        Surface {
            ChatHeader(
                title = "Quick chat",
                subtitle = "Memory: On",
                onBack = {},
                onRename = {},
                onMenuClick = {}
            )
        }
    }
}

@Preview(name = "Chat Header Long Title")
@Composable
private fun ChatHeaderLongTitlePreview() {
    MaterialTheme {
        Surface {
            ChatHeader(
                title = "Very Long…",
                onBack = {},
                onRename = {},
                onMenuClick = {}
            )
        }
    }
}

@Preview(name = "Chat Header Dark")
@Composable
private fun ChatHeaderDarkPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface {
            ChatHeader(
                title = "Hello wor…",
                subtitle = "Persona: Assistant",
                onBack = {},
                onRename = {},
                onMenuClick = {}
            )
        }
    }
}

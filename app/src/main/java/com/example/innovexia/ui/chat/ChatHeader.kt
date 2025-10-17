package com.example.innovexia.ui.chat

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import com.example.innovexia.R
import com.example.innovexia.ui.theme.InnovexiaTokens
import kotlin.math.PI
import kotlin.math.sin

/**
 * Material 3 Chat Header - Compact Edition
 *
 * Features:
 * - Matches ChatComposerV3 background color exactly
 * - Compact layout with minimal padding
 * - Small, clean buttons
 * - Smooth model dropdown
 * - Material 3 design principles
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
    // Match ChatComposerV3 background exactly
    val containerColor = Color(0xFF171A1E) // Same as ChatComposerV3
    val iconTint = Color(0xFFE5EAF0) // Light icons
    val textColor = Color(0xFFE5EAF0)

    Surface(
        color = containerColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(
                    horizontal = 12.dp,
                    vertical = 4.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left menu button - slightly bigger for easy access
            IconButton(
                onClick = onOpenMenu,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Menu,
                    contentDescription = "Menu",
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.width(8.dp))

            // Title with Material 3 dropdown style - compact
            if (onTitleClick != null && !isGuest) {
                // Wrapper with weight to control expansion
                Box(modifier = Modifier.weight(1f)) {
                    // M3 surface container - compact
                    Surface(
                        onClick = onTitleClick,
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF2A323B).copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, Color(0xFF4A525B).copy(alpha = 0.3f)),
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .widthIn(max = 240.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = title.ifEmpty { "New chat" },
                                color = textColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                            )

                            Spacer(Modifier.width(4.dp))

                            // Static chevron
                            Icon(
                                imageVector = Icons.Outlined.KeyboardArrowDown,
                                contentDescription = "Open model settings",
                                tint = textColor.copy(alpha = 0.6f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            } else {
                // Non-clickable title
                Text(
                    text = title.ifEmpty { "New chat" },
                    color = textColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .weight(1f)
                        .basicMarquee(iterations = Int.MAX_VALUE),
                    maxLines = 1
                )
            }

            Spacer(Modifier.width(8.dp))

            // Right action buttons - compact, no animations
            if (!isGuest) {
                val targetIcon = when {
                    isIncognitoActive -> "incognito_active"
                    isNewChat -> "incognito"
                    else -> "new"
                }

                Crossfade(
                    targetState = targetIcon,
                    animationSpec = tween(
                        durationMillis = 200,
                        easing = FastOutSlowInEasing
                    ),
                    label = "headerActionSwap"
                ) { state ->
                    when (state) {
                        "incognito_active" -> CompactGlassIconButton(
                            icon = painterResource(R.drawable.ic_incognito_24),
                            contentDesc = "Incognito (Local Only)",
                            onClick = { /* No action - incognito is permanent */ },
                            showAccentDot = true
                        )
                        "incognito" -> CompactGlassIconButton(
                            icon = painterResource(R.drawable.ic_incognito_24),
                            contentDesc = "Enable Incognito",
                            onClick = onIncognito,
                            showAccentDot = false
                        )
                        else -> CompactGlassIconButton(
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

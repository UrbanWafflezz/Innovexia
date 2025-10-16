package com.example.innovexia.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.innovexia.ui.models.RecentChat
import com.example.innovexia.ui.theme.InnovexiaTheme
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SideMenu(
    recent: List<RecentChat>,
    archived: List<RecentChat>,
    trash: List<RecentChat>,
    selectedChatId: String?,
    onNewChat: () -> Unit,
    onOpenChat: (String) -> Unit,
    onPin: (String) -> Unit,
    onArchive: (String) -> Unit,
    onDelete: (String) -> Unit,
    onRestoreFromArchive: (String) -> Unit,
    onRestoreFromTrash: (String) -> Unit,
    onEmptyTrash: () -> Unit,
    onAboutClick: () -> Unit,
    modifier: Modifier = Modifier,
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Cloud restore parameters
    showCloudRestoreButton: Boolean = false,
    cloudChatCount: Int = 0,
    onRestoreFromCloud: () -> Unit = {},
    onHideCloudRestoreButton: () -> Unit = {},
    onOpenCloudSyncSettings: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    ModalDrawerSheet(
        modifier = modifier
            .fillMaxHeight()
            .widthIn(max = 420.dp)
            .fillMaxWidth(0.88f),
        drawerContainerColor = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { focusManager.clearFocus() })
                }
                .clip(RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp))
                .background(
                    if (darkTheme) {
                        Brush.verticalGradient(
                            colors = listOf(
                                com.example.innovexia.ui.theme.InnovexiaColors.DarkGradientStart,
                                com.example.innovexia.ui.theme.InnovexiaColors.DarkGradientMid,
                                com.example.innovexia.ui.theme.InnovexiaColors.DarkGradientEnd
                            )
                        )
                    } else {
                        Brush.verticalGradient(
                            colors = listOf(
                                com.example.innovexia.ui.theme.InnovexiaColors.LightGradientStart,
                                com.example.innovexia.ui.theme.InnovexiaColors.LightGradientMid,
                                com.example.innovexia.ui.theme.InnovexiaColors.LightGradientEnd
                            )
                        )
                    }
                )
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize(),
                contentPadding = PaddingValues(vertical = 20.dp)
            ) {
                // Header with logo + Glass "+ New" button
                item {
                    DrawerHeader(
                        onNewChat = onNewChat
                    )
                }

                // Search bar matching chat composer
                item {
                    DrawerSearchBar(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                    )
                }

                // Cloud Restore Button (if visible)
                if (showCloudRestoreButton) {
                    item {
                        CloudRestoreButton(
                            chatCount = cloudChatCount,
                            onClick = onRestoreFromCloud,
                            onHide = onHideCloudRestoreButton,
                            onOpenSettings = onOpenCloudSyncSettings,
                            darkTheme = darkTheme,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )
                    }
                }

                // Recent Chats section
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "RECENT CHATS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.4.sp
                        ),
                        color = if (darkTheme) SideMenuTokens.SecondaryText
                                else SideMenuTokens.LightSecondaryText,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                    )
                }

                if (recent.isEmpty()) {
                    item {
                        EmptyStateText(
                            text = "No chats yet",
                            darkTheme = darkTheme
                        )
                    }
                } else {
                    items(recent, key = { it.id }) { chat ->
                        ChatCard(
                            chat = chat,
                            isSelected = chat.id == selectedChatId,
                            onOpenChat = { onOpenChat(chat.id) },
                            onPin = { onPin(chat.id) },
                            onArchive = { onArchive(chat.id) },
                            onDelete = { onDelete(chat.id) },
                            darkTheme = darkTheme,
                            modifier = Modifier
                                .padding(horizontal = 20.dp, vertical = 6.dp)
                                .animateItemPlacement()
                        )
                    }
                }

                // Archived section
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                    CollapsibleSection(
                        title = "ARCHIVED",
                        count = archived.size,
                        darkTheme = darkTheme
                    ) {
                        if (archived.isEmpty()) {
                            EmptyStateText(
                                text = "No archived chats",
                                darkTheme = darkTheme
                            )
                        } else {
                            archived.forEach { chat ->
                                ChatCard(
                                    chat = chat,
                                    isSelected = chat.id == selectedChatId,
                                    onOpenChat = { onOpenChat(chat.id) },
                                    onRestore = { onRestoreFromArchive(chat.id) },
                                    onDelete = { onDelete(chat.id) },
                                    darkTheme = darkTheme,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                // Trash section
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    CollapsibleSection(
                        title = "TRASH",
                        count = trash.size,
                        darkTheme = darkTheme,
                        trailingAction = if (trash.isNotEmpty()) {
                            { EmptyTrashButton(onEmptyTrash, darkTheme) }
                        } else null
                    ) {
                        if (trash.isEmpty()) {
                            EmptyStateText(
                                text = "Trash is empty",
                                darkTheme = darkTheme
                            )
                        } else {
                            trash.forEach { chat ->
                                ChatCard(
                                    chat = chat,
                                    isSelected = false,
                                    onOpenChat = { onOpenChat(chat.id) },
                                    onRestore = { onRestoreFromTrash(chat.id) },
                                    onDeleteForever = { onDelete(chat.id) },
                                    darkTheme = darkTheme,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                // Footer
                item {
                    Spacer(modifier = Modifier.height(28.dp))
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = if (darkTheme) SideMenuTokens.Dividers
                                else SideMenuTokens.LightCardBorder
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    AboutRow(
                        onClick = onAboutClick,
                        darkTheme = darkTheme
                    )

                    Text(
                        text = "v1.0",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = if (darkTheme) SideMenuTokens.SecondaryText.copy(alpha = 0.5f)
                                else SideMenuTokens.LightSecondaryText.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                    )
                }
            }
        }
    }
}

// Design tokens - Matching chat container theme
private object SideMenuTokens {
    // Dark theme - matching chat container
    val DrawerBg = Color(0xFF2A2A2A) // Same as chat container
    val CardBg = Color(0xFF1F1F1F) // Slightly darker for cards
    val CardBorder = Color(0xFF404040).copy(alpha = 0.5f)
    val PrimaryText = Color(0xFFD4AF37) // Yellowish gold
    val SecondaryText = Color(0xFFA89968) // Muted gold
    val AccentRing = Color(0xFF8B4513) // Reddish brown (saddle brown)
    val SelectedDot = Color(0xFFB8860B) // Dark goldenrod
    val Dividers = Color(0xFF3A3A3A)
    val FocusOutline = Color(0xFF8B4513) // Reddish brown
    val NewButtonBg = Color(0xFF1F1F1F)
    val NewButtonBorder = Color(0xFF404040)
    val UnreadDot = Color(0xFFD4AF37) // Gold for unread

    // Light theme (fallback to Material3)
    val LightPrimaryText = Color(0xFF1F2937)
    val LightSecondaryText = Color(0xFF6B7280)
    val LightCardBg = Color(0xFFF9FAFB)
    val LightCardBorder = Color(0xFFE5E7EB)
    val LightAccentRing = Color(0xFF3B82F6)
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatCard(
    chat: RecentChat,
    isSelected: Boolean,
    onOpenChat: () -> Unit,
    darkTheme: Boolean,
    modifier: Modifier = Modifier,
    onPin: ((String) -> Unit)? = null,
    onArchive: ((String) -> Unit)? = null,
    onDelete: ((String) -> Unit)? = null,
    onRestore: ((String) -> Unit)? = null,
    onDeleteForever: ((String) -> Unit)? = null
) {
    val view = LocalView.current
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onOpenChat,
                onLongClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    showMenu = true
                }
            ),
        shape = RoundedCornerShape(14.dp),
        color = if (darkTheme) SideMenuTokens.CardBg
                else SideMenuTokens.LightCardBg,
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) {
                if (darkTheme) SideMenuTokens.AccentRing
                else SideMenuTokens.LightAccentRing
            } else {
                if (darkTheme) SideMenuTokens.CardBorder
                else SideMenuTokens.LightCardBorder
            }
        ),
        shadowElevation = if (isSelected) 4.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Left: emoji/avatar or pin star
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (darkTheme) Color(0xFF1F2937)
                        else Color(0xFFF3F4F6)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (chat.pinned) {
                    Icon(
                        imageVector = Icons.Rounded.Star,
                        contentDescription = "Pinned",
                        modifier = Modifier.size(18.dp),
                        tint = SideMenuTokens.SelectedDot
                    )
                } else {
                    Text(
                        text = chat.emoji ?: "💬",
                        fontSize = 18.sp
                    )
                }
            }

            // Middle: title + timestamp
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = chat.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = if (chat.unread) FontWeight.SemiBold else FontWeight.Medium,
                        fontSize = 15.sp,
                        lineHeight = 20.sp
                    ),
                    color = (if (darkTheme) SideMenuTokens.PrimaryText else SideMenuTokens.LightPrimaryText)
                        .copy(alpha = if (isSelected) 1f else 0.92f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = chat.timestamp,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 16.sp
                    ),
                    color = if (darkTheme) SideMenuTokens.SecondaryText
                            else SideMenuTokens.LightSecondaryText
                )
            }

            // Right: incognito icon or unread dot
            if (chat.isIncognito) {
                Icon(
                    painter = androidx.compose.ui.res.painterResource(com.example.innovexia.R.drawable.ic_incognito_24),
                    contentDescription = "Incognito",
                    modifier = Modifier.size(16.dp),
                    tint = if (darkTheme) SideMenuTokens.SecondaryText
                           else SideMenuTokens.LightSecondaryText
                )
            } else if (chat.unread) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (darkTheme) SideMenuTokens.UnreadDot
                            else SideMenuTokens.LightAccentRing
                        )
                )
            }
        }

        // Context menu (triggered by long press) - Styled popup
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier
                .background(
                    if (darkTheme) SideMenuTokens.CardBg else Color.White,
                    shape = RoundedCornerShape(16.dp)
                )
                .border(
                    width = 1.dp,
                    color = if (darkTheme) SideMenuTokens.CardBorder else SideMenuTokens.LightCardBorder,
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            onPin?.let {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = if (chat.pinned) "Unpin" else "Pin",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                            color = if (darkTheme) SideMenuTokens.PrimaryText else SideMenuTokens.LightPrimaryText
                        )
                    },
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        showMenu = false
                        onPin(chat.id)
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (chat.pinned) Icons.Rounded.StarBorder
                                          else Icons.Rounded.Star,
                            contentDescription = null,
                            tint = if (darkTheme) SideMenuTokens.AccentRing else SideMenuTokens.LightAccentRing,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
            onArchive?.let {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "Archive",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                            color = if (darkTheme) SideMenuTokens.PrimaryText else SideMenuTokens.LightPrimaryText
                        )
                    },
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        showMenu = false
                        onArchive(chat.id)
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Archive,
                            contentDescription = null,
                            tint = if (darkTheme) SideMenuTokens.SecondaryText else SideMenuTokens.LightSecondaryText,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
            onRestore?.let {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "Restore",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                            color = if (darkTheme) SideMenuTokens.PrimaryText else SideMenuTokens.LightPrimaryText
                        )
                    },
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        showMenu = false
                        onRestore(chat.id)
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Restore,
                            contentDescription = null,
                            tint = if (darkTheme) SideMenuTokens.AccentRing else SideMenuTokens.LightAccentRing,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
            onDelete?.let {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "Delete",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                            color = if (darkTheme) Color(0xFFE74C3C) else Color(0xFFDC2626)
                        )
                    },
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        showMenu = false
                        onDelete(chat.id)
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = null,
                            tint = if (darkTheme) Color(0xFFE74C3C) else Color(0xFFDC2626),
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
            onDeleteForever?.let {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "Delete Forever",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                            color = if (darkTheme) Color(0xFFE74C3C) else Color(0xFFDC2626)
                        )
                    },
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        showMenu = false
                        onDeleteForever(chat.id)
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.DeleteForever,
                            contentDescription = null,
                            tint = if (darkTheme) Color(0xFFE74C3C) else Color(0xFFDC2626),
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun CollapsibleSection(
    title: String,
    count: Int,
    darkTheme: Boolean,
    trailingAction: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "chevron rotation"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        // Header row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    modifier = Modifier
                        .size(18.dp)
                        .rotate(rotationAngle),
                    tint = if (darkTheme) SideMenuTokens.SecondaryText
                           else SideMenuTokens.LightSecondaryText
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.4.sp
                    ),
                    color = if (darkTheme) SideMenuTokens.SecondaryText
                            else SideMenuTokens.LightSecondaryText
                )
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = if (darkTheme) SideMenuTokens.SecondaryText.copy(alpha = 0.6f)
                            else SideMenuTokens.LightSecondaryText.copy(alpha = 0.6f)
                )
            }

            trailingAction?.invoke()
        }

        // Content (expanded)
        if (expanded) {
            Column(
                modifier = Modifier.padding(top = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun EmptyTrashButton(
    onEmptyTrash: () -> Unit,
    darkTheme: Boolean
) {
    val view = LocalView.current

    TextButton(
        onClick = {
            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            onEmptyTrash()
        },
        colors = ButtonDefaults.textButtonColors(
            contentColor = if (darkTheme) SideMenuTokens.SecondaryText
                           else SideMenuTokens.LightSecondaryText
        )
    ) {
        Text(
            text = "Empty",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

@Composable
private fun EmptyStateText(
    text: String,
    darkTheme: Boolean
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = if (darkTheme) SideMenuTokens.SecondaryText.copy(alpha = 0.5f)
                else SideMenuTokens.LightSecondaryText.copy(alpha = 0.5f),
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
    )
}

@Composable
private fun AboutRow(
    onClick: () -> Unit,
    darkTheme: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = onClick,
                role = Role.Button
            )
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.Info,
            contentDescription = "About",
            modifier = Modifier.size(20.dp),
            tint = if (darkTheme) SideMenuTokens.SecondaryText
                   else SideMenuTokens.LightSecondaryText
        )
        Text(
            text = "About",
            style = MaterialTheme.typography.bodyMedium,
            color = if (darkTheme) SideMenuTokens.PrimaryText
                    else SideMenuTokens.LightPrimaryText
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CloudRestoreButton(
    chatCount: Int,
    onClick: () -> Unit,
    onHide: () -> Unit,
    onOpenSettings: () -> Unit,
    darkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    showMenu = true
                }
            ),
        shape = RoundedCornerShape(14.dp),
        color = if (darkTheme) SideMenuTokens.CardBg.copy(alpha = 0.8f)
                else SideMenuTokens.LightCardBg.copy(alpha = 0.9f),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (darkTheme) Color(0xFF4A9EFF).copy(alpha = 0.5f)
                    else Color(0xFF3B82F6).copy(alpha = 0.5f)
        ),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Cloud icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (darkTheme) Color(0xFF1E3A8A).copy(alpha = 0.3f)
                        else Color(0xFFDBEAFE)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.CloudDownload,
                    contentDescription = "Cloud Restore",
                    modifier = Modifier.size(20.dp),
                    tint = if (darkTheme) Color(0xFF60A5FA)
                           else Color(0xFF2563EB)
                )
            }

            // Text content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "Restore from Cloud",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    ),
                    color = if (darkTheme) Color(0xFF60A5FA)
                            else Color(0xFF2563EB)
                )
                if (chatCount > 0) {
                    Text(
                        text = "$chatCount chat${if (chatCount != 1) "s" else ""} available",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp
                        ),
                        color = if (darkTheme) SideMenuTokens.SecondaryText
                                else SideMenuTokens.LightSecondaryText
                    )
                }
            }

            // Badge if chats available
            if (chatCount > 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (darkTheme) Color(0xFF1E3A8A)
                            else Color(0xFF3B82F6)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = chatCount.toString(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        color = Color.White
                    )
                }
            }
        }

        // Context menu for long press
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier
                .background(
                    if (darkTheme) SideMenuTokens.CardBg else Color.White,
                    shape = RoundedCornerShape(16.dp)
                )
                .border(
                    width = 1.dp,
                    color = if (darkTheme) SideMenuTokens.CardBorder else SideMenuTokens.LightCardBorder,
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        text = "Hide this button",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                        color = if (darkTheme) SideMenuTokens.PrimaryText else SideMenuTokens.LightPrimaryText
                    )
                },
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                    showMenu = false
                    onHide()
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.VisibilityOff,
                        contentDescription = null,
                        tint = if (darkTheme) SideMenuTokens.SecondaryText else SideMenuTokens.LightSecondaryText,
                        modifier = Modifier.size(20.dp)
                    )
                },
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            DropdownMenuItem(
                text = {
                    Text(
                        text = "Cloud Sync Settings",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                        color = if (darkTheme) SideMenuTokens.PrimaryText else SideMenuTokens.LightPrimaryText
                    )
                },
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                    showMenu = false
                    onOpenSettings()
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = null,
                        tint = if (darkTheme) SideMenuTokens.AccentRing else SideMenuTokens.LightAccentRing,
                        modifier = Modifier.size(20.dp)
                    )
                },
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

// Demo data seeders
fun demoRecent(): List<RecentChat> = listOf(
    RecentChat(
        id = "1",
        title = "Morning standup notes",
        emoji = "☀️",
        timestamp = "2m ago",
        unread = true,
        pinned = false
    ),
    RecentChat(
        id = "2",
        title = "Q4 Planning Discussion",
        emoji = "📊",
        timestamp = "1h ago",
        unread = false,
        pinned = true
    ),
    RecentChat(
        id = "3",
        title = "Code review feedback",
        emoji = "💻",
        timestamp = "3h ago",
        unread = false,
        pinned = false
    ),
    RecentChat(
        id = "4",
        title = "Design system updates",
        emoji = "🎨",
        timestamp = "Yesterday",
        unread = false,
        pinned = false
    ),
    RecentChat(
        id = "5",
        title = "API integration help",
        emoji = "🔌",
        timestamp = "Yesterday",
        unread = false,
        pinned = false
    ),
    RecentChat(
        id = "6",
        title = "Team building ideas",
        emoji = "🎉",
        timestamp = "2 days ago",
        unread = false,
        pinned = false
    )
)

fun demoArchived(): List<RecentChat> = listOf(
    RecentChat(
        id = "a1",
        title = "Old project notes",
        emoji = "📁",
        timestamp = "1 week ago",
        unread = false,
        pinned = false
    ),
    RecentChat(
        id = "a2",
        title = "Archive test conversation",
        emoji = "🗄️",
        timestamp = "2 weeks ago",
        unread = false,
        pinned = false
    )
)

fun demoTrash(): List<RecentChat> = listOf(
    RecentChat(
        id = "t1",
        title = "Deleted conversation",
        emoji = "🗑️",
        timestamp = "3 days ago",
        unread = false,
        pinned = false
    )
)

// Previews
@Preview(name = "SideMenu Dark", showBackground = true, widthDp = 320, heightDp = 800)
@Composable
fun SideMenuPreview_Dark() {
    InnovexiaTheme(darkTheme = true) {
        SideMenu(
            recent = demoRecent(),
            archived = demoArchived(),
            trash = demoTrash(),
            selectedChatId = "2",
            onNewChat = {},
            onOpenChat = {},
            onPin = {},
            onArchive = {},
            onDelete = {},
            onRestoreFromArchive = {},
            onRestoreFromTrash = {},
            onEmptyTrash = {},
            onAboutClick = {},
            darkTheme = true
        )
    }
}

@Preview(name = "SideMenu Empty", showBackground = true, widthDp = 320, heightDp = 800)
@Composable
fun SideMenuPreview_Empty() {
    InnovexiaTheme(darkTheme = true) {
        SideMenu(
            recent = emptyList(),
            archived = emptyList(),
            trash = emptyList(),
            selectedChatId = null,
            onNewChat = {},
            onOpenChat = {},
            onPin = {},
            onArchive = {},
            onDelete = {},
            onRestoreFromArchive = {},
            onRestoreFromTrash = {},
            onEmptyTrash = {},
            onAboutClick = {},
            darkTheme = true
        )
    }
}

@Preview(name = "SideMenu Light", showBackground = true, widthDp = 320, heightDp = 800)
@Composable
fun SideMenuPreview_Light() {
    InnovexiaTheme(darkTheme = false) {
        SideMenu(
            recent = demoRecent(),
            archived = demoArchived(),
            trash = demoTrash(),
            selectedChatId = "2",
            onNewChat = {},
            onOpenChat = {},
            onPin = {},
            onArchive = {},
            onDelete = {},
            onRestoreFromArchive = {},
            onRestoreFromTrash = {},
            onEmptyTrash = {},
            onAboutClick = {},
            darkTheme = false
        )
    }
}

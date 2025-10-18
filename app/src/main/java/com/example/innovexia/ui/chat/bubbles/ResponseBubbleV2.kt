package com.example.innovexia.ui.chat.bubbles
import com.example.innovexia.core.ai.getModelLabel

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.outlined.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material3.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.Dialog
import com.example.innovexia.data.local.entities.MessageEntity
import com.example.innovexia.ui.theme.InnovexiaColors
import com.example.innovexia.data.ai.GroundingMetadata
import com.example.innovexia.ui.animations.MotionDefaults
import java.text.SimpleDateFormat
import java.util.*

// Import source utilities
import com.example.innovexia.ui.utils.SourceItem
import com.example.innovexia.ui.utils.extractSources
import com.example.innovexia.ui.utils.openUrlInApp
import com.example.innovexia.ui.webview.WebViewDialog

/**
 * Advanced Response Bubble V2
 * Premium markdown rendering with dynamic blocks, collapsibles, and modern typography
 * Now supports both regular responses AND web-grounded responses with sources
 */
@Composable
fun ResponseBubbleV2(
    message: MessageEntity,
    modifier: Modifier = Modifier,
    isStreaming: Boolean = false,
    messageStatus: com.example.innovexia.ui.models.MessageStatus = com.example.innovexia.ui.models.MessageStatus.COMPLETE,
    modelName: String = "Innovexia",
    isTruncated: Boolean = false,
    groundingMetadata: GroundingMetadata? = null,
    onRegenerate: (String) -> Unit = {},
    onCopy: (String) -> Unit = {},
    onContinue: (String) -> Unit = {}
) {
    // Check if we're in SENDING state or STREAMING state
    // A message is "sending" if it's streaming but has no text yet (waiting for first token)
    val streaming = message.getStreamStateEnum() == com.example.innovexia.data.local.entities.StreamState.STREAMING
    val isSending = (isStreaming || streaming) && message.text.isBlank()

    // Disable actions while streaming or if there's an error
    val actionsEnabled = !isStreaming && !streaming && message.getStreamStateEnum() != com.example.innovexia.data.local.entities.StreamState.ERROR

    val blocks = remember(message.text, message.id) {
        if (message.text.isNotEmpty()) MarkdownParser.parse(message.text) else emptyList()
    }

    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    // Better gray colors for premium look
    val isDark = isSystemInDarkTheme()
    val bubbleColor = if (isDark) Color(0xFF1E2329) else Color(0xFFF5F5F7)
    val borderColor = if (isDark) Color(0xFF2A323B).copy(alpha = 0.6f) else Color(0xFFE0E0E3)
    val textPrimary = if (isDark) Color(0xFFECEFF4) else Color(0xFF1C1C1E)
    val textSecondary = if (isDark) Color(0xFFB7C0CC) else Color(0xFF86868B)
    val dividerColor = if (isDark) Color(0xFF2A323B).copy(alpha = 0.4f) else Color(0xFFD1D1D6)

    // Fast Material 3 entrance animation - optimized for streaming
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(message.id) {
        isVisible = true
    }

    val offsetX by animateDpAsState(
        targetValue = if (isVisible) 0.dp else (-20).dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "bubble_slide_in"
    )

    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = 150,
            easing = LinearOutSlowInEasing
        ),
        label = "bubble_fade_in"
    )

    Surface(
        color = bubbleColor,
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 2.dp,
        shadowElevation = 4.dp,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .fillMaxWidth()
            .graphicsLayer {
                translationX = offsetX.toPx()
                this.alpha = alpha
            }
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessHigh
                )
            )
    ) {
        // Dialog states
        var showSourcesDialog by remember { mutableStateOf(false) }
        var webViewUrl by remember { mutableStateOf<String?>(null) }

        // Extract sources from grounding metadata
        val sources = remember(groundingMetadata) {
            android.util.Log.d("ResponseBubbleV2", "🔍 Message ${message.id} - groundingMetadata: ${groundingMetadata != null}")
            if (groundingMetadata != null) {
                val extracted = extractSources(groundingMetadata)
                android.util.Log.d("ResponseBubbleV2", "  → Extracted ${extracted.size} sources")
                extracted
            } else {
                android.util.Log.w("ResponseBubbleV2", "  ⚠️ No groundingMetadata provided!")
                emptyList()
            }
        }

        Column {
            // Message content with padding
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessHigh,
                            visibilityThreshold = IntSize(1, 1)
                        )
                    ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Header row with web search badge, status pill and sources badge
                if (streaming || sources.isNotEmpty() || groundingMetadata != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left side - Web Search badge OR Status pill
                        if (streaming) {
                            StatusPill(text = "Regenerating…", textSecondary = textSecondary)
                        } else if (groundingMetadata != null && !isSending) {
                            // Web Search badge for grounded responses
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = InnovexiaColors.Gold.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, InnovexiaColors.Gold.copy(alpha = 0.25f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.TravelExplore,
                                        contentDescription = "Web Search",
                                        tint = InnovexiaColors.Gold,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "Web Search",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = InnovexiaColors.Gold
                                        )
                                    )
                                }
                            }
                        } else {
                            Spacer(Modifier.width(1.dp))
                        }

                        // Sources badge on right
                        if (sources.isNotEmpty() && !isSending) {
                            Surface(
                                modifier = Modifier.clickable { showSourcesDialog = true },
                                shape = RoundedCornerShape(999.dp),
                                color = InnovexiaColors.BlueAccent.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, InnovexiaColors.BlueAccent.copy(alpha = 0.25f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Link,
                                        contentDescription = "Sources",
                                        tint = InnovexiaColors.BlueAccent,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "${sources.size}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            color = InnovexiaColors.BlueAccent
                                        )
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }

                // Show "Sending..." if in SENDING state, otherwise show content
                if (isSending) {
                    SendingIndicator(textSecondary = textSecondary)
                } else if (streaming && message.text.isBlank()) {
                    // Show skeleton while regenerating with no text yet
                    ResponseBubbleSkeleton(textSecondary)
                } else {
                    if (blocks.isNotEmpty()) {
                        MarkdownBody(
                            blocks = blocks,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            bubbleBorder = borderColor
                        )
                    }

                    if (isStreaming && !streaming) {
                        StreamingIndicator(textSecondary)
                    }
                }

                // Error footer
                if (message.getStreamStateEnum() == com.example.innovexia.data.local.entities.StreamState.ERROR && message.error != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = message.error,
                        color = Color(0xFFFF5252),
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                // Truncation footer (token limit reached)
                if (isTruncated && !isStreaming && !streaming) {
                    Spacer(Modifier.height(8.dp))
                    TruncationFooter(
                        textSecondary = textSecondary,
                        onContinue = { onContinue(message.id) }
                    )
                }
            }

            // Related Searches (only for web grounded responses with valid sources)
            val relatedSearches = remember(groundingMetadata) {
                generateRelatedSearches(
                    metadata = groundingMetadata,
                    userQuery = message.text.take(100) // Use first part of response as context
                )
            }

            if (!isStreaming && !streaming && relatedSearches.isNotEmpty()) {
                RelatedSearchesSection(
                    searches = relatedSearches,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    dividerColor = dividerColor,
                    isDark = isDark,
                    onSearchClick = { query ->
                        // Trigger a new search with the related query
                        onRegenerate(query)
                    }
                )
            }

            // Footer with divider and actions
            if (!isStreaming && !streaming) {
                FooterRow(
                    message = message,
                    timestamp = message.createdAt,
                    modelName = modelName,
                    textSecondary = textSecondary,
                    dividerColor = dividerColor,
                    actionsEnabled = actionsEnabled,
                    onCopy = onCopy,
                    onRegenerate = onRegenerate
                )
            }
        }

        // Sources dialog popup
        if (showSourcesDialog && sources.isNotEmpty()) {
            SourcesDialog(
                sources = sources,
                onDismiss = { showSourcesDialog = false },
                onOpenUrl = { url ->
                    webViewUrl = url
                    showSourcesDialog = false
                },
                textPrimary = textPrimary,
                textSecondary = textSecondary
            )
        }

        // In-app WebView browser
        webViewUrl?.let { url ->
            WebViewDialog(
                url = url,
                onDismiss = { webViewUrl = null }
            )
        }
    }
}

/**
 * Premium Sources Dialog - Modern bottomsheet-style with seamless animations
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SourcesDialog(
    sources: List<SourceItem>,
    onDismiss: () -> Unit,
    onOpenUrl: (String) -> Unit,
    textPrimary: Color,
    textSecondary: Color
) {
    val isDark = isSystemInDarkTheme()

    // Animated entry
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    val animatedAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(200),
        label = "dialog_alpha"
    )

    val animatedScale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.92f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "dialog_scale"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = if (isDark) Color(0xFF1C1C1E) else Color(0xFFFAFAFA),
            tonalElevation = 0.dp,
            shadowElevation = 24.dp,
            border = BorderStroke(
                1.5.dp,
                if (isDark) Color(0xFF2C2C2E).copy(alpha = 0.8f) else Color(0xFFE5E5EA).copy(alpha = 0.8f)
            ),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = 560.dp)
                .graphicsLayer {
                    alpha = animatedAlpha
                    scaleX = animatedScale
                    scaleY = animatedScale
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                // Modern pull handle
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 12.dp, bottom = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(textSecondary.copy(alpha = 0.3f))
                    )
                }

                // Header section
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Gradient icon background
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(InnovexiaColors.BlueAccent.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Public,
                                    contentDescription = null,
                                    tint = InnovexiaColors.BlueAccent,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = "Sources",
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 22.sp,
                                        color = textPrimary
                                    )
                                )
                                Text(
                                    text = "${sources.size} ${if (sources.size == 1) "reference" else "references"}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = textSecondary,
                                        fontSize = 13.sp
                                    )
                                )
                            }
                        }

                        // Close button
                        IconButton(
                            onClick = { onDismiss() },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Close",
                                tint = textSecondary
                            )
                        }
                    }
                }

                // Premium divider with fade
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(
                            androidx.compose.ui.graphics.Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    textSecondary.copy(alpha = 0.15f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Scrollable sources list with padding
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    sources.forEachIndexed { index, source ->
                        SourceCard(
                            source = source,
                            index = index,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            isDark = isDark,
                            onClick = { onOpenUrl(source.url) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual source card with premium interactions
 */
@Composable
private fun SourceCard(
    source: SourceItem,
    index: Int,
    textPrimary: Color,
    textSecondary: Color,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }

    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "card_press"
    )

    Surface(
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        shape = RoundedCornerShape(16.dp),
        color = if (isDark) Color(0xFF2C2C2E) else Color.White,
        tonalElevation = 0.dp,
        shadowElevation = 2.dp,
        border = BorderStroke(
            1.dp,
            if (isDark) Color(0xFF3A3A3C).copy(alpha = 0.6f) else Color(0xFFE5E5EA).copy(alpha = 0.8f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Number badge with gradient
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        androidx.compose.ui.graphics.Brush.linearGradient(
                            colors = listOf(
                                InnovexiaColors.BlueAccent.copy(alpha = 0.2f),
                                InnovexiaColors.BlueAccent.copy(alpha = 0.1f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${index + 1}",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = InnovexiaColors.BlueAccent
                    )
                )
            }

            // Source content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = source.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        lineHeight = 20.sp,
                        color = textPrimary
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Link,
                        contentDescription = null,
                        tint = InnovexiaColors.BlueAccent.copy(alpha = 0.7f),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = source.domain,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 13.sp,
                            color = textSecondary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Arrow icon with subtle animation
            Icon(
                imageVector = Icons.Outlined.ArrowForward,
                contentDescription = "Open",
                tint = InnovexiaColors.BlueAccent.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Status pill for "Regenerating..." / "Sending..."
 */
@Composable
private fun StatusPill(text: String, textSecondary: Color) {
    val isDark = isSystemInDarkTheme()
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = if (isDark) Color(0xFF2A323B).copy(alpha = 0.5f) else Color(0xFFE0E0E3).copy(alpha = 0.5f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall.copy(color = textSecondary)
        )
    }
}

/**
 * Skeleton shimmer for loading state
 */
@Composable
private fun ResponseBubbleSkeleton(textSecondary: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(3) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (it == 2) 0.6f else 1f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(textSecondary.copy(alpha = 0.1f))
            )
        }
    }
}

/**
 * Footer row with branding, timestamp, and action buttons
 */
@Composable
private fun FooterRow(
    message: MessageEntity,
    timestamp: Long,
    modelName: String,
    textSecondary: Color,
    dividerColor: Color,
    actionsEnabled: Boolean,
    onCopy: (String) -> Unit,
    onRegenerate: (String) -> Unit
) {
    val context = LocalContext.current

    // Format time as 12-hour clock (e.g., "2:34 PM")
    val timeStr = remember(timestamp) {
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        sdf.format(Date(timestamp))
    }

    Column {
        // Divider
        HorizontalDivider(
            color = dividerColor,
            thickness = 1.dp
        )

        // Footer content
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Model name display
            Text(
                text = modelName,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = textSecondary
                )
            )

            // Action buttons removed - will be added later
            Spacer(Modifier.width(8.dp))

            // Right: Timestamp
            Text(
                text = timeStr,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    color = textSecondary.copy(alpha = 0.7f)
                )
            )
        }
    }
}


/**
 * Sending indicator - Ultra-smooth Material 3 animation with fun rotating messages
 * Fast, responsive, no lag
 */
@Composable
private fun SendingIndicator(textSecondary: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "sending_animation")

    // Fun thinking messages that rotate
    val thinkingMessages = remember {
        listOf(
            "Sending",
            "Thinking",
            "Processing",
            "Chatting",
            "Pondering",
            "Analyzing",
            "Crafting response",
            "Contemplating",
            "Computing",
            "Brainstorming",
            "Working on it",
            "Just a moment",
            "Formulating",
            "Preparing reply"
        )
    }

    // Rotate message every 1.5 seconds
    var currentMessageIndex by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1500)
            currentMessageIndex = (currentMessageIndex + 1) % thinkingMessages.size
        }
    }

    // Smooth fade animation for text transitions
    val textAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 800,
                easing = LinearOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "text_pulse"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        // Animated dots with smooth wave effect
        repeat(3) { index ->
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.6f,
                targetValue = 1.3f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 500,
                        delayMillis = index * 120,
                        easing = LinearOutSlowInEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "sending_dot_scale_$index"
            )

            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.35f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 500,
                        delayMillis = index * 120,
                        easing = LinearOutSlowInEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "sending_dot_alpha_$index"
            )

            Box(
                modifier = Modifier
                    .size(8.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
                    .background(
                        color = InnovexiaColors.BlueAccent,
                        shape = RoundedCornerShape(50)
                    )
            )
        }

        // Animated rotating text with crossfade
        androidx.compose.animation.AnimatedContent(
            targetState = thinkingMessages[currentMessageIndex],
            transitionSpec = {
                fadeIn(
                    animationSpec = tween(300, easing = LinearOutSlowInEasing)
                ) togetherWith fadeOut(
                    animationSpec = tween(300, easing = FastOutLinearInEasing)
                )
            },
            label = "thinking_text_transition"
        ) { message ->
            Text(
                text = message,
                fontSize = 13.sp,
                color = textSecondary,
                fontStyle = FontStyle.Italic,
                modifier = Modifier.graphicsLayer {
                    alpha = textAlpha
                }
            )
        }
    }
}

/**
 * Streaming indicator - Ultra-smooth Material 3 wave animation
 * Optimized for seamless, lag-free streaming experience
 */
@Composable
private fun StreamingIndicator(textSecondary: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "streaming_animation")

    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        repeat(3) { index ->
            // Fast, smooth oscillation with minimal delay
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.6f,
                targetValue = 1.3f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 500,
                        delayMillis = index * 120,
                        easing = LinearOutSlowInEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "streaming_dot_scale_$index"
            )

            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.35f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 500,
                        delayMillis = index * 120,
                        easing = LinearOutSlowInEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "streaming_dot_alpha_$index"
            )

            // Smooth gradient effect
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
                    .background(
                        color = InnovexiaColors.TealAccent,
                        shape = RoundedCornerShape(50)
                    )
            )
        }
    }
}

/**
 * Truncation footer - shows when response hit token limit
 */
@Composable
private fun TruncationFooter(
    textSecondary: Color,
    onContinue: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isDark) Color(0xFF2A323B).copy(alpha = 0.3f) else Color(0xFFE0E0E3).copy(alpha = 0.3f))
            .clickable { onContinue() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.MoreHoriz,
            contentDescription = "Continue",
            tint = textSecondary,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = "Response truncated — Continue",
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = textSecondary
            ),
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Outlined.Refresh,
            contentDescription = "Continue",
            tint = textSecondary,
            modifier = Modifier.size(16.dp)
        )
    }
}

/**
 * Generate smart related searches from grounding metadata
 * Always returns exactly 4 high-quality, meaningful searches
 */
private fun generateRelatedSearches(
    metadata: GroundingMetadata?,
    userQuery: String
): List<String> {
    if (metadata == null) return emptyList()

    val searches = mutableListOf<String>()

    // 1. Use web search queries if they're meaningful (not too generic)
    metadata.webSearchQueries?.let { queries ->
        queries
            .filter { it.length > 10 } // Filter out very short queries
            .filter { !it.contains("site:", ignoreCase = true) } // Remove site-specific searches
            .filter { !it.startsWith("\"") } // Remove exact match queries
            .forEach { searches.add(it) }
    }

    // 2. Extract topics from source titles (these are real, validated sources)
    metadata.groundingChunks.forEach { chunk ->
        val title = chunk.web.title
        // Extract meaningful phrases from titles
        if (title.length > 15 && !title.contains("404") && !title.contains("Error")) {
            // Clean up the title to make it search-friendly
            val cleanTitle = title
                .replace(Regex("[|–—-].*"), "") // Remove everything after separator
                .trim()
                .take(50) // Limit length for better readability

            if (cleanTitle.length > 10 && cleanTitle.split(" ").size >= 2) {
                searches.add(cleanTitle)
            }
        }
    }

    // 3. Filter and ensure exactly 4 quality searches
    val filteredSearches = searches
        .filter { it.isNotBlank() }
        .distinct()
        .take(4) // Always show exactly 4
        .toMutableList()

    // 4. If we don't have 4, pad with fallback searches based on the topic
    while (filteredSearches.size < 4 && metadata.groundingChunks.isNotEmpty()) {
        val chunk = metadata.groundingChunks.getOrNull(filteredSearches.size)
        if (chunk != null) {
            val domain = try {
                android.net.Uri.parse(chunk.web.uri).host?.replace("www.", "") ?: ""
            } catch (e: Exception) { "" }

            if (domain.isNotEmpty() && !filteredSearches.contains(domain)) {
                filteredSearches.add("$domain insights")
            }
        } else {
            break
        }
    }

    return filteredSearches.take(4)
}

/**
 * Related Searches Section - Material 3 design with 2x2 grid showing full text
 * Animated entrance for smooth streaming
 */
@Composable
private fun RelatedSearchesSection(
    searches: List<String>,
    textPrimary: Color,
    textSecondary: Color,
    dividerColor: Color,
    isDark: Boolean,
    onSearchClick: (String) -> Unit
) {
    val haptics = LocalHapticFeedback.current

    // Ensure exactly 4 searches for 2x2 grid
    if (searches.size < 4) return

    // Animated entrance
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(searches) {
        kotlinx.coroutines.delay(100) // Small delay for smooth appearance
        isVisible = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 400, easing = LinearOutSlowInEasing),
        label = "related_searches_alpha"
    )

    val offsetY by animateDpAsState(
        targetValue = if (isVisible) 0.dp else 20.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "related_searches_offset"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp)
            .graphicsLayer {
                this.alpha = alpha
                translationY = offsetY.toPx()
            },
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Material 3 Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Explore,
                contentDescription = "Related",
                tint = InnovexiaColors.Gold,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "Related Searches",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = textPrimary
                )
            )
        }

        // 2x2 Grid - Material 3 Cards with staggered animation
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // First row (2 cards) - cards appear with delay
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(
                    animationSpec = tween(durationMillis = 300, delayMillis = 50)
                ) + expandVertically(
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    searches.take(2).forEach { query ->
                        RelatedSearchCard(
                            query = query,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            isDark = isDark,
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSearchClick(query)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Second row (2 cards) - appears slightly after first row
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(
                    animationSpec = tween(durationMillis = 300, delayMillis = 150)
                ) + expandVertically(
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    searches.drop(2).take(2).forEach { query ->
                        RelatedSearchCard(
                            query = query,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            isDark = isDark,
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSearchClick(query)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Material 3 Related Search Card - Shows full text with auto-scaling
 */
@Composable
private fun RelatedSearchCard(
    query: String,
    textPrimary: Color,
    textSecondary: Color,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "card_press"
    )

    // Material 3 Filled Card with outlined style
    ElevatedCard(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isDark) Color(0xFF1E1E1E) else Color(0xFFFAFAFA),
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 1.dp,
            pressedElevation = 3.dp
        ),
        modifier = modifier
            .height(90.dp)
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Icon at top
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(InnovexiaColors.Gold.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = InnovexiaColors.Gold,
                    modifier = Modifier.size(14.dp)
                )
            }

            // Query text - auto-scales to fit
            Text(
                text = query,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    lineHeight = 16.sp,
                    color = textPrimary
                ),
                fontSize = when {
                    query.length <= 20 -> 13.sp
                    query.length <= 30 -> 12.sp
                    else -> 11.sp
                },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                minLines = 2
            )
        }

        // Subtle indicator overlay in bottom-right
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Icon(
                imageVector = Icons.Outlined.ArrowOutward,
                contentDescription = "Open",
                tint = InnovexiaColors.Gold.copy(alpha = 0.5f),
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

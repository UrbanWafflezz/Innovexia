package com.example.innovexia.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.innovexia.core.ai.GeminiModels
import com.example.innovexia.core.ai.ModelIds
import com.example.innovexia.core.ai.ModelInfo
import com.example.innovexia.ui.theme.InnovexiaColors

/**
 * EXTRAORDINARY Material 3 Model Switcher with Glassmorphism
 *
 * Features:
 * - Matches SideMenu gradient background exactly
 * - Compact bubble cards for models with message counts
 * - Premium glassmorphism effects
 * - Smooth Material 3 animations throughout
 * - Elevated card design for settings sections
 * - Gradient sliders and premium controls
 */

// ==================== TOKENS ====================

object M3SwitcherTokens {
    // Exact SideMenu gradient colors
    val DarkGradientStart = InnovexiaColors.DarkGradientStart
    val DarkGradientMid = InnovexiaColors.DarkGradientMid
    val DarkGradientEnd = InnovexiaColors.DarkGradientEnd

    val LightGradientStart = InnovexiaColors.LightGradientStart
    val LightGradientMid = InnovexiaColors.LightGradientMid
    val LightGradientEnd = InnovexiaColors.LightGradientEnd

    // Seamless blending colors - no section cards, just background
    val CardBgDark = Color.Transparent
    val CardBgLight = Color.Transparent
    val CardBorderDark = Color(0xFF404040).copy(alpha = 0.15f)
    val CardBorderLight = Color(0xFFE5E7EB).copy(alpha = 0.15f)

    // Text colors
    val TitleDark = Color(0xFFECEFF4)
    val TitleLight = Color(0xFF1C1C1E)
    val SubtleDark = Color(0xFFB7C0CC)
    val SubtleLight = Color(0xFF6B7280)

    // Accent colors
    val GoldAccent = InnovexiaColors.GoldDim
    val BlueAccent = InnovexiaColors.BlueAccent
    val TealAccent = InnovexiaColors.TealAccent

    // Badge colors
    val BadgeBgDark = Color(0xFF2A323B)
    val BadgeBgLight = Color(0xFFE3F2FD)

    val Radius = 20.dp
    val CardRadius = 16.dp
}

// ==================== DATA MODELS ====================

/**
 * UI-only AI preferences for per-chat model selection
 */
data class UiAiPrefs(
    val model: String = "gemini-2.5-flash",
    val creativity: Float = 0.7f,
    val maxOutputTokens: Int = 8192,
    val safety: String = "Standard",
    val groundingEnabled: Boolean = false,
    val codeExecutionEnabled: Boolean = false,
    val fileCreationEnabled: Boolean = false,
    val reasoningDepth: String = "balanced"
)

/**
 * Model usage statistics
 */
data class ModelStats(
    val totalMessages: Int = 0
)

// ==================== HEADER TITLE (PRESSABLE) ====================

@Composable
fun ChatHeaderTitle(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    darkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val titleColor = if (darkTheme) M3SwitcherTokens.TitleDark else M3SwitcherTokens.TitleLight
    val subtleColor = if (darkTheme) M3SwitcherTokens.SubtleDark else M3SwitcherTokens.SubtleLight

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onToggle() }
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(
            title,
            color = titleColor,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Icon(
            imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
            contentDescription = if (expanded) "Close model switcher" else "Open model switcher",
            tint = subtleColor,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

// ==================== MODEL SWITCHER PANEL ====================

@Composable
fun ModelSwitcherPanel(
    prefs: UiAiPrefs,
    onPrefsChange: (UiAiPrefs) -> Unit,
    onOpenFullSettings: () -> Unit,
    onDismiss: () -> Unit,
    darkTheme: Boolean,
    modifier: Modifier = Modifier,
    modelStats: Map<String, ModelStats> = emptyMap() // New: message counts per model
) {
    // Entrance animation
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "panel_fade"
    )

    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.92f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "panel_scale"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        this.alpha = alpha
                        scaleX = scale
                        scaleY = scale
                    },
                shape = RoundedCornerShape(M3SwitcherTokens.Radius),
                color = Color.Transparent,
                tonalElevation = 0.dp,
                shadowElevation = 24.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(M3SwitcherTokens.Radius))
                        .background(
                            if (darkTheme) {
                                Brush.verticalGradient(
                                    colors = listOf(
                                        M3SwitcherTokens.DarkGradientStart,
                                        M3SwitcherTokens.DarkGradientMid,
                                        M3SwitcherTokens.DarkGradientEnd
                                    )
                                )
                            } else {
                                Brush.verticalGradient(
                                    colors = listOf(
                                        M3SwitcherTokens.LightGradientStart,
                                        M3SwitcherTokens.LightGradientMid,
                                        M3SwitcherTokens.LightGradientEnd
                                    )
                                )
                            }
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 24.dp, horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        PanelContent(
                            prefs = prefs,
                            onPrefsChange = onPrefsChange,
                            onOpenFullSettings = onOpenFullSettings,
                            onDismiss = onDismiss,
                            darkTheme = darkTheme,
                            modelStats = modelStats
                        )
                    }
                }
            }
        }
    }
}

// ==================== PANEL CONTENT ====================

@Composable
private fun PanelContent(
    prefs: UiAiPrefs,
    onPrefsChange: (UiAiPrefs) -> Unit,
    onOpenFullSettings: () -> Unit,
    onDismiss: () -> Unit,
    darkTheme: Boolean,
    modelStats: Map<String, ModelStats>
) {
    val subtleColor = if (darkTheme) M3SwitcherTokens.SubtleDark else M3SwitcherTokens.SubtleLight

    // --- Header ---
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            "AI Settings",
            color = if (darkTheme) M3SwitcherTokens.TitleDark else M3SwitcherTokens.TitleLight,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Close",
                tint = if (darkTheme) M3SwitcherTokens.SubtleDark else M3SwitcherTokens.SubtleLight,
                modifier = Modifier.size(24.dp)
            )
        }
    }

    // --- Models Section ---
    SectionCard(darkTheme) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader("Model Selection", Icons.Rounded.Psychology, subtleColor)

            GeminiModels.forEach { model ->
                ModelBubbleCard(
                    model = model,
                    selected = prefs.model == model.id,
                    darkTheme = darkTheme,
                    messageCount = modelStats[model.id]?.totalMessages ?: 0,
                    onClick = { onPrefsChange(prefs.copy(model = model.id)) }
                )
            }
        }
    }

    // --- Quick Settings ---
    SectionCard(darkTheme) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SectionHeader("Quick Settings", Icons.Rounded.Tune, subtleColor)

            // Creativity Slider with Gradient
            GradientSlider(
                title = "Creativity",
                value = prefs.creativity,
                darkTheme = darkTheme,
                onValueChange = { onPrefsChange(prefs.copy(creativity = it)) }
            )

            // Response Length Stepper
            TokenStepper(
                value = prefs.maxOutputTokens,
                darkTheme = darkTheme,
                onChange = { onPrefsChange(prefs.copy(maxOutputTokens = it)) }
            )

            // Safety Level
            SafetySelector(
                current = prefs.safety,
                darkTheme = darkTheme,
                onSelect = { onPrefsChange(prefs.copy(safety = it)) }
            )
        }
    }

    // --- Tools Section ---
    SectionCard(darkTheme) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader("AI Tools", Icons.Rounded.Build, subtleColor)

            ToolToggleCard(
                title = "Grounding with Search",
                description = "Search the web for real-time information",
                icon = Icons.Rounded.Search,
                enabled = prefs.groundingEnabled,
                darkTheme = darkTheme,
                onToggle = { onPrefsChange(prefs.copy(groundingEnabled = it)) }
            )

            ToolToggleCard(
                title = "Code Execution",
                description = "Run Python code for calculations",
                icon = Icons.Rounded.Code,
                enabled = prefs.codeExecutionEnabled,
                darkTheme = darkTheme,
                onToggle = { onPrefsChange(prefs.copy(codeExecutionEnabled = it)) }
            )

            ToolToggleCard(
                title = "File Creation",
                description = "Generate downloadable PDF documents",
                icon = Icons.Rounded.Description,
                enabled = prefs.fileCreationEnabled,
                darkTheme = darkTheme,
                onToggle = { onPrefsChange(prefs.copy(fileCreationEnabled = it)) }
            )
        }
    }

    // --- Reasoning Depth ---
    SectionCard(darkTheme) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader("Reasoning Depth", Icons.Rounded.AutoAwesome, subtleColor)

            ReasoningChip(
                title = "Concise",
                description = "Quick responses",
                selected = prefs.reasoningDepth == "concise",
                darkTheme = darkTheme,
                onClick = { onPrefsChange(prefs.copy(reasoningDepth = "concise")) }
            )

            ReasoningChip(
                title = "Balanced",
                description = "Moderate reasoning",
                selected = prefs.reasoningDepth == "balanced",
                darkTheme = darkTheme,
                onClick = { onPrefsChange(prefs.copy(reasoningDepth = "balanced")) }
            )

            ReasoningChip(
                title = "Comprehensive",
                description = "Deep analysis",
                selected = prefs.reasoningDepth == "comprehensive",
                darkTheme = darkTheme,
                onClick = { onPrefsChange(prefs.copy(reasoningDepth = "comprehensive")) }
            )
        }
    }

    // --- Full Settings Button ---
    FilledTonalButton(
        onClick = onOpenFullSettings,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = if (darkTheme) M3SwitcherTokens.GoldAccent.copy(alpha = 0.15f)
                            else M3SwitcherTokens.GoldAccent.copy(alpha = 0.2f),
            contentColor = M3SwitcherTokens.GoldAccent
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.SettingsApplications,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text("Advanced Settings", fontWeight = FontWeight.SemiBold)
    }
}

// ==================== SECTION COMPONENTS ====================

@Composable
private fun SectionCard(
    darkTheme: Boolean,
    content: @Composable ColumnScope.() -> Unit
) {
    // Seamless section - blends with gradient background
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        content = content
    )
}

@Composable
private fun SectionHeader(
    title: String,
    icon: ImageVector,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = title,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp
        )
    }
}

// ==================== MODEL BUBBLE CARD ====================

@Composable
private fun ModelBubbleCard(
    model: ModelInfo,
    selected: Boolean,
    darkTheme: Boolean,
    messageCount: Int,
    onClick: () -> Unit
) {
    // Animation for selection
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.02f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "model_scale"
    )

    // Seamless colors that blend with gradient
    val bgColor = if (selected) {
        if (darkTheme) {
            Color(0xFF2A2F38).copy(alpha = 0.8f) // Darker, subtle highlight
        } else {
            Color(0xFFF3F4F6).copy(alpha = 0.9f)
        }
    } else {
        if (darkTheme) {
            Color(0xFF1E2128).copy(alpha = 0.4f) // Very subtle, blends with gradient
        } else {
            Color(0xFFFFFFFF).copy(alpha = 0.3f)
        }
    }

    val borderColor = if (selected) {
        if (darkTheme) {
            M3SwitcherTokens.GoldAccent.copy(alpha = 0.5f) // Softer gold
        } else {
            M3SwitcherTokens.GoldAccent.copy(alpha = 0.6f)
        }
    } else {
        if (darkTheme) Color(0xFF404040).copy(alpha = 0.2f)
        else Color(0xFFE5E7EB).copy(alpha = 0.3f)
    }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = borderColor
        ),
        tonalElevation = 0.dp,
        shadowElevation = if (selected) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon bubble - seamless blend
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected) {
                            if (darkTheme) M3SwitcherTokens.GoldAccent.copy(alpha = 0.18f)
                            else M3SwitcherTokens.GoldAccent.copy(alpha = 0.15f)
                        } else {
                            if (darkTheme) Color(0xFF252932).copy(alpha = 0.6f)
                            else Color(0xFFF3F4F6).copy(alpha = 0.7f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (model.id) {
                        ModelIds.GEM_PRO -> Icons.Rounded.Psychology
                        ModelIds.GEM_FLASH -> Icons.Rounded.Bolt
                        else -> Icons.Rounded.Speed
                    },
                    contentDescription = null,
                    tint = if (selected) {
                        M3SwitcherTokens.GoldAccent.copy(alpha = 0.9f)
                    } else {
                        if (darkTheme) M3SwitcherTokens.SubtleDark.copy(alpha = 0.7f)
                        else M3SwitcherTokens.SubtleLight.copy(alpha = 0.8f)
                    },
                    modifier = Modifier.size(22.dp)
                )
            }

            // Model info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = model.label,
                        color = if (darkTheme) M3SwitcherTokens.TitleDark else M3SwitcherTokens.TitleLight,
                        fontSize = 15.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
                    )

                    // Message count badge
                    if (messageCount > 0) {
                        Badge(
                            containerColor = if (darkTheme) M3SwitcherTokens.BadgeBgDark
                                            else M3SwitcherTokens.BadgeBgLight,
                            contentColor = if (darkTheme) M3SwitcherTokens.TitleDark
                                          else M3SwitcherTokens.TitleLight
                        ) {
                            Text(
                                text = "$messageCount",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Text(
                    text = model.description,
                    color = if (darkTheme) M3SwitcherTokens.SubtleDark else M3SwitcherTokens.SubtleLight,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Selection indicator - subtle
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = "Selected",
                    tint = M3SwitcherTokens.GoldAccent.copy(alpha = 0.8f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

// ==================== GRADIENT SLIDER ====================

@Composable
private fun GradientSlider(
    title: String,
    value: Float,
    darkTheme: Boolean,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = if (darkTheme) M3SwitcherTokens.TitleDark else M3SwitcherTokens.TitleLight,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (darkTheme) Color(0xFF2A323B) else Color(0xFFE3F2FD)
            ) {
                Text(
                    text = String.format("%.1f", value),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    color = M3SwitcherTokens.BlueAccent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..1f,
            steps = 9,
            colors = SliderDefaults.colors(
                thumbColor = M3SwitcherTokens.BlueAccent,
                activeTrackColor = M3SwitcherTokens.BlueAccent,
                inactiveTrackColor = if (darkTheme) Color(0xFF2A323B) else Color(0xFFE3F2FD)
            )
        )

        Text(
            text = "Higher = more creative, lower = more focused",
            color = if (darkTheme) M3SwitcherTokens.SubtleDark else M3SwitcherTokens.SubtleLight,
            fontSize = 11.sp,
            lineHeight = 14.sp
        )
    }
}

// ==================== TOKEN STEPPER ====================

@Composable
private fun TokenStepper(
    value: Int,
    darkTheme: Boolean,
    onChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Response Length",
                    color = if (darkTheme) M3SwitcherTokens.TitleDark else M3SwitcherTokens.TitleLight,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "~${value / 4} words maximum",
                    color = if (darkTheme) M3SwitcherTokens.SubtleDark else M3SwitcherTokens.SubtleLight,
                    fontSize = 11.sp
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledIconButton(
                    onClick = { onChange((value - 1024).coerceAtLeast(1024)) },
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (darkTheme) Color(0xFF2A323B) else Color(0xFFE3F2FD),
                        contentColor = if (darkTheme) M3SwitcherTokens.TitleDark else M3SwitcherTokens.TitleLight
                    )
                ) {
                    Icon(Icons.Outlined.Remove, "Decrease", modifier = Modifier.size(18.dp))
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = M3SwitcherTokens.TealAccent.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "$value",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        color = M3SwitcherTokens.TealAccent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                FilledIconButton(
                    onClick = { onChange((value + 1024).coerceAtMost(65536)) },
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (darkTheme) Color(0xFF2A323B) else Color(0xFFE3F2FD),
                        contentColor = if (darkTheme) M3SwitcherTokens.TitleDark else M3SwitcherTokens.TitleLight
                    )
                ) {
                    Icon(Icons.Outlined.Add, "Increase", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

// ==================== SAFETY SELECTOR ====================

@Composable
private fun SafetySelector(
    current: String,
    darkTheme: Boolean,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Safety",
            color = if (darkTheme) M3SwitcherTokens.TitleDark else M3SwitcherTokens.TitleLight,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SafetyChip(
                label = "Standard",
                selected = current == "Standard",
                darkTheme = darkTheme,
                onClick = { onSelect("Standard") }
            )
            SafetyChip(
                label = "Unfiltered",
                selected = current == "Unfiltered",
                darkTheme = darkTheme,
                onClick = { onSelect("Unfiltered") }
            )
        }
    }
}

@Composable
private fun SafetyChip(
    label: String,
    selected: Boolean,
    darkTheme: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        },
        leadingIcon = if (selected) {
            {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        } else null,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = if (darkTheme) {
                M3SwitcherTokens.GoldAccent.copy(alpha = 0.18f)
            } else {
                M3SwitcherTokens.GoldAccent.copy(alpha = 0.15f)
            },
            selectedLabelColor = M3SwitcherTokens.GoldAccent.copy(alpha = 0.9f),
            selectedLeadingIconColor = M3SwitcherTokens.GoldAccent.copy(alpha = 0.9f),
            containerColor = if (darkTheme) {
                Color(0xFF252932).copy(alpha = 0.5f)
            } else {
                Color(0xFFF3F4F6).copy(alpha = 0.6f)
            },
            labelColor = if (darkTheme) {
                M3SwitcherTokens.SubtleDark.copy(alpha = 0.75f)
            } else {
                M3SwitcherTokens.SubtleLight.copy(alpha = 0.8f)
            }
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = if (selected) {
                M3SwitcherTokens.GoldAccent.copy(alpha = 0.4f)
            } else {
                Color.Transparent
            },
            selectedBorderColor = M3SwitcherTokens.GoldAccent.copy(alpha = 0.5f),
            borderWidth = if (selected) 1.5.dp else 0.dp
        )
    )
}

// ==================== TOOL TOGGLE CARD ====================

@Composable
private fun ToolToggleCard(
    title: String,
    description: String,
    icon: ImageVector,
    enabled: Boolean,
    darkTheme: Boolean,
    onToggle: (Boolean) -> Unit
) {
    // Seamless tool card colors
    val toolBgColor = if (enabled) {
        if (darkTheme) {
            Color(0xFF1E2A2F).copy(alpha = 0.5f) // Teal-tinted dark
        } else {
            Color(0xFFE0F2F1).copy(alpha = 0.4f)
        }
    } else {
        if (darkTheme) {
            Color(0xFF1E2128).copy(alpha = 0.3f)
        } else {
            Color.White.copy(alpha = 0.25f)
        }
    }

    val toolBorderColor = if (enabled) {
        if (darkTheme) M3SwitcherTokens.TealAccent.copy(alpha = 0.35f)
        else M3SwitcherTokens.TealAccent.copy(alpha = 0.4f)
    } else {
        if (darkTheme) Color(0xFF404040).copy(alpha = 0.2f)
        else Color(0xFFE5E7EB).copy(alpha = 0.3f)
    }

    Surface(
        onClick = { onToggle(!enabled) },
        shape = RoundedCornerShape(12.dp),
        color = toolBgColor,
        border = BorderStroke(
            width = if (enabled) 1.5.dp else 1.dp,
            color = toolBorderColor
        ),
        tonalElevation = 0.dp,
        shadowElevation = if (enabled) 1.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (enabled) {
                            if (darkTheme) M3SwitcherTokens.TealAccent.copy(alpha = 0.15f)
                            else M3SwitcherTokens.TealAccent.copy(alpha = 0.12f)
                        } else {
                            if (darkTheme) Color(0xFF252932).copy(alpha = 0.5f)
                            else Color(0xFFF3F4F6).copy(alpha = 0.6f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) {
                        M3SwitcherTokens.TealAccent.copy(alpha = 0.85f)
                    } else {
                        if (darkTheme) M3SwitcherTokens.SubtleDark.copy(alpha = 0.6f)
                        else M3SwitcherTokens.SubtleLight.copy(alpha = 0.7f)
                    },
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = if (darkTheme) M3SwitcherTokens.TitleDark else M3SwitcherTokens.TitleLight,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    color = if (darkTheme) M3SwitcherTokens.SubtleDark else M3SwitcherTokens.SubtleLight,
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                )
            }

            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = M3SwitcherTokens.TealAccent,
                    checkedTrackColor = M3SwitcherTokens.TealAccent.copy(alpha = 0.5f),
                    uncheckedThumbColor = if (darkTheme) Color(0xFF4A525B) else Color(0xFFD1D1D6),
                    uncheckedTrackColor = if (darkTheme) Color(0xFF2A323B) else Color(0xFFF3F4F6)
                )
            )
        }
    }
}

// ==================== REASONING CHIP ====================

@Composable
private fun ReasoningChip(
    title: String,
    description: String,
    selected: Boolean,
    darkTheme: Boolean,
    onClick: () -> Unit
) {
    // Seamless reasoning chip colors
    val reasoningBgColor = if (selected) {
        if (darkTheme) {
            Color(0xFF2A2F38).copy(alpha = 0.7f)
        } else {
            Color(0xFFFFF8E1).copy(alpha = 0.5f)
        }
    } else {
        if (darkTheme) {
            Color(0xFF1E2128).copy(alpha = 0.35f)
        } else {
            Color.White.copy(alpha = 0.3f)
        }
    }

    val reasoningBorderColor = if (selected) {
        if (darkTheme) M3SwitcherTokens.GoldAccent.copy(alpha = 0.5f)
        else M3SwitcherTokens.GoldAccent.copy(alpha = 0.6f)
    } else {
        if (darkTheme) Color(0xFF404040).copy(alpha = 0.2f)
        else Color(0xFFE5E7EB).copy(alpha = 0.3f)
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = reasoningBgColor,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = reasoningBorderColor
        ),
        tonalElevation = 0.dp,
        shadowElevation = if (selected) 1.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = M3SwitcherTokens.GoldAccent,
                    unselectedColor = if (darkTheme) M3SwitcherTokens.SubtleDark
                                     else M3SwitcherTokens.SubtleLight
                )
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = if (darkTheme) M3SwitcherTokens.TitleDark else M3SwitcherTokens.TitleLight,
                    fontSize = 14.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
                )
                Text(
                    text = description,
                    color = if (darkTheme) M3SwitcherTokens.SubtleDark else M3SwitcherTokens.SubtleLight,
                    fontSize = 11.sp
                )
            }

            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = "Selected",
                    tint = M3SwitcherTokens.GoldAccent.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

package com.example.innovexia.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.innovexia.core.ai.GeminiModels
import com.example.innovexia.core.ai.ModelInfo
import com.example.innovexia.ui.theme.InnovexiaColors

/**
 * Simplified Model Switcher for Gemini 2.5 Family Only
 * Provides a pressable title that opens a model selection panel with quick AI settings.
 */

// ==================== TOKENS ====================

object HeaderPanelTokens {
    val SurfaceLight = InnovexiaColors.LightSurfaceElevated
    val SurfaceDark = InnovexiaColors.DarkSurfaceElevated
    val BorderLight = InnovexiaColors.LightBorder.copy(alpha = 0.6f)
    val BorderDark = InnovexiaColors.DarkBorder.copy(alpha = 0.6f)
    val TitleLight = InnovexiaColors.LightTextPrimary
    val TitleDark = InnovexiaColors.DarkTextPrimary
    val SubtleLight = InnovexiaColors.LightTextSecondary
    val SubtleDark = InnovexiaColors.DarkTextSecondary
    val Accent = InnovexiaColors.GoldDim
    val Radius = 16.dp
}

// ==================== DATA MODELS ====================

/**
 * UI-only AI preferences for per-chat model selection
 */
data class UiAiPrefs(
    val model: String = "gemini-2.5-flash",
    val creativity: Float = 0.7f, // Temperature for generation
    val maxOutputTokens: Int = 8192,
    val safety: String = "Standard", // Standard or Unfiltered
    // Tools
    val groundingEnabled: Boolean = false,
    val codeExecutionEnabled: Boolean = false,
    val fileCreationEnabled: Boolean = false,
    // Reasoning Depth
    val reasoningDepth: String = "balanced" // "concise" | "balanced" | "comprehensive"
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
    val titleColor = if (darkTheme) HeaderPanelTokens.TitleDark else HeaderPanelTokens.TitleLight
    val subtleColor = if (darkTheme) HeaderPanelTokens.SubtleDark else HeaderPanelTokens.SubtleLight

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
    modifier: Modifier = Modifier
) {
    val surfaceColor = if (darkTheme) HeaderPanelTokens.SurfaceDark else HeaderPanelTokens.SurfaceLight
    val borderColor = if (darkTheme) HeaderPanelTokens.BorderDark else HeaderPanelTokens.BorderLight

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
                .padding(horizontal = 16.dp)
                .heightIn(max = 600.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(HeaderPanelTokens.Radius),
                color = surfaceColor,
                tonalElevation = 8.dp,
                shadowElevation = 16.dp,
                border = BorderStroke(1.dp, borderColor)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 600.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 16.dp)
                ) {
                    PanelContent(
                        prefs = prefs,
                        onPrefsChange = onPrefsChange,
                        onOpenFullSettings = onOpenFullSettings,
                        darkTheme = darkTheme
                    )
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
    darkTheme: Boolean
) {
    val borderColor = if (darkTheme) HeaderPanelTokens.BorderDark else HeaderPanelTokens.BorderLight
    val subtleColor = if (darkTheme) HeaderPanelTokens.SubtleDark else HeaderPanelTokens.SubtleLight

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Gemini Models Section (Scrollable) ---
        Text(
            "Gemini 2.5 Models",
            color = subtleColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )

        // Scrollable model list container
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 240.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            GeminiModels.forEach { model ->
                ModelRow(
                    model = model,
                    selected = prefs.model == model.id,
                    darkTheme = darkTheme,
                    onClick = { onPrefsChange(prefs.copy(model = model.id)) }
                )
            }
        }

        HorizontalDivider(color = borderColor)

        // --- Quick Settings ---
        Text(
            "Quick Settings",
            color = subtleColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )

        // Creativity (Temperature)
        SettingRowWithDescription(
            title = "Creativity",
            description = "Higher = more creative, lower = more focused",
            darkTheme = darkTheme
        ) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    String.format("%.1f", prefs.creativity),
                    fontSize = 13.sp,
                    color = if (darkTheme) HeaderPanelTokens.TitleDark else HeaderPanelTokens.TitleLight,
                    fontWeight = FontWeight.Medium
                )
                Slider(
                    value = prefs.creativity,
                    onValueChange = { onPrefsChange(prefs.copy(creativity = it)) },
                    valueRange = 0f..1f,
                    steps = 9,
                    modifier = Modifier.width(160.dp)
                )
            }
        }

        // Max Output Tokens with user-friendly explanation
        Column(modifier = Modifier.fillMaxWidth()) {
            StepperRow(
                title = "Response Length",
                value = prefs.maxOutputTokens,
                min = 1024,
                max = 65536,
                step = 1024,
                darkTheme = darkTheme
            ) {
                onPrefsChange(prefs.copy(maxOutputTokens = it))
            }
            Text(
                "Max tokens in response (~${prefs.maxOutputTokens / 4} words). " +
                "Higher = longer responses, costs more. Range: 1K-65K",
                color = subtleColor,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
        }

        // Safety Level
        DropdownRow(
            title = "Safety",
            current = prefs.safety,
            items = listOf("Standard", "Unfiltered"),
            darkTheme = darkTheme
        ) {
            onPrefsChange(prefs.copy(safety = it))
        }

        HorizontalDivider(color = borderColor)

        // --- Tools Section ---
        ToolsSection(
            prefs = prefs,
            onPrefsChange = onPrefsChange,
            darkTheme = darkTheme
        )

        HorizontalDivider(color = borderColor)

        // --- Reasoning Depth Section ---
        ReasoningDepthSection(
            prefs = prefs,
            onPrefsChange = onPrefsChange,
            darkTheme = darkTheme
        )

        HorizontalDivider(color = borderColor)

        // Link to full settings
        OutlinedButton(
            onClick = onOpenFullSettings,
            border = BorderStroke(1.dp, borderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("View All AI Settings")
        }
    }
}

// ==================== MODEL ROW ====================

@Composable
private fun ModelRow(
    model: ModelInfo,
    selected: Boolean,
    darkTheme: Boolean,
    onClick: () -> Unit
) {
    val titleColor = if (darkTheme) HeaderPanelTokens.TitleDark else HeaderPanelTokens.TitleLight
    val subtleColor = if (darkTheme) HeaderPanelTokens.SubtleDark else HeaderPanelTokens.SubtleLight

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = HeaderPanelTokens.Accent
            )
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp)
        ) {
            Text(
                model.label,
                color = titleColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
            Text(
                model.description,
                color = subtleColor,
                fontSize = 13.sp,
                lineHeight = 16.sp
            )
        }
    }
}

// ==================== SETTING ROWS ====================

@Composable
private fun SettingRow(
    title: String,
    darkTheme: Boolean,
    trailing: @Composable () -> Unit
) {
    val titleColor = if (darkTheme) HeaderPanelTokens.TitleDark else HeaderPanelTokens.TitleLight

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            color = titleColor,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
        trailing()
    }
}

@Composable
private fun SettingRowWithDescription(
    title: String,
    description: String,
    darkTheme: Boolean,
    trailing: @Composable () -> Unit
) {
    val titleColor = if (darkTheme) HeaderPanelTokens.TitleDark else HeaderPanelTokens.TitleLight
    val subtleColor = if (darkTheme) HeaderPanelTokens.SubtleDark else HeaderPanelTokens.SubtleLight

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    color = titleColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    description,
                    color = subtleColor,
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                )
            }
            trailing()
        }
    }
}

@Composable
private fun StepperRow(
    title: String,
    value: Int,
    min: Int,
    max: Int,
    step: Int,
    darkTheme: Boolean,
    onChange: (Int) -> Unit
) {
    val titleColor = if (darkTheme) HeaderPanelTokens.TitleDark else HeaderPanelTokens.TitleLight

    SettingRow(title, darkTheme) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { onChange((value - step).coerceAtLeast(min)) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Outlined.Remove,
                    contentDescription = "Decrease",
                    tint = titleColor
                )
            }
            Text(
                "$value",
                color = titleColor,
                fontSize = 14.sp,
                modifier = Modifier.widthIn(min = 56.dp),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )
            IconButton(
                onClick = { onChange((value + step).coerceAtMost(max)) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Outlined.Add,
                    contentDescription = "Increase",
                    tint = titleColor
                )
            }
        }
    }
}

@Composable
private fun DropdownRow(
    title: String,
    current: String,
    items: List<String>,
    darkTheme: Boolean,
    onPick: (String) -> Unit
) {
    val borderColor = if (darkTheme) HeaderPanelTokens.BorderDark else HeaderPanelTokens.BorderLight
    var open by remember { mutableStateOf(false) }

    SettingRow(title, darkTheme) {
        Box {
            OutlinedButton(
                onClick = { open = true },
                border = BorderStroke(1.dp, borderColor)
            ) {
                Text(current, fontSize = 13.sp)
            }
            DropdownMenu(
                expanded = open,
                onDismissRequest = { open = false }
            ) {
                items.forEach { opt ->
                    DropdownMenuItem(
                        text = { Text(opt) },
                        onClick = {
                            open = false
                            onPick(opt)
                        }
                    )
                }
            }
        }
    }
}

// ==================== TOOLS SECTION ====================

@Composable
private fun ToolsSection(
    prefs: UiAiPrefs,
    onPrefsChange: (UiAiPrefs) -> Unit,
    darkTheme: Boolean
) {
    val subtleColor = if (darkTheme) HeaderPanelTokens.SubtleDark else HeaderPanelTokens.SubtleLight

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Tools",
            color = subtleColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )

        // Grounding with Search
        ToolToggleRow(
            title = "Grounding with Search",
            description = "Search the web for real-time information and cite sources",
            enabled = prefs.groundingEnabled,
            darkTheme = darkTheme,
            onToggle = { onPrefsChange(prefs.copy(groundingEnabled = it)) }
        )

        // Code Execution
        ToolToggleRow(
            title = "Code Execution",
            description = "Run Python code to perform calculations and data analysis",
            enabled = prefs.codeExecutionEnabled,
            darkTheme = darkTheme,
            onToggle = { onPrefsChange(prefs.copy(codeExecutionEnabled = it)) }
        )

        // File Creation (PDF)
        ToolToggleRow(
            title = "File Creation",
            description = "Generate downloadable PDF documents from responses",
            enabled = prefs.fileCreationEnabled,
            darkTheme = darkTheme,
            onToggle = { onPrefsChange(prefs.copy(fileCreationEnabled = it)) }
        )
    }
}

@Composable
private fun ToolToggleRow(
    title: String,
    description: String,
    enabled: Boolean,
    darkTheme: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val titleColor = if (darkTheme) HeaderPanelTokens.TitleDark else HeaderPanelTokens.TitleLight
    val subtleColor = if (darkTheme) HeaderPanelTokens.SubtleDark else HeaderPanelTokens.SubtleLight

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!enabled) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                title,
                color = titleColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                description,
                color = subtleColor,
                fontSize = 11.sp,
                lineHeight = 14.sp
            )
        }
        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = HeaderPanelTokens.Accent,
                checkedTrackColor = HeaderPanelTokens.Accent.copy(alpha = 0.5f)
            )
        )
    }
}

// ==================== REASONING DEPTH SECTION ====================

@Composable
private fun ReasoningDepthSection(
    prefs: UiAiPrefs,
    onPrefsChange: (UiAiPrefs) -> Unit,
    darkTheme: Boolean
) {
    val subtleColor = if (darkTheme) HeaderPanelTokens.SubtleDark else HeaderPanelTokens.SubtleLight
    val titleColor = if (darkTheme) HeaderPanelTokens.TitleDark else HeaderPanelTokens.TitleLight

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "Reasoning Depth",
            color = subtleColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )

        // Concise
        ReasoningDepthOption(
            title = "Concise",
            description = "Quick, direct responses for simple queries",
            selected = prefs.reasoningDepth == "concise",
            darkTheme = darkTheme,
            onClick = { onPrefsChange(prefs.copy(reasoningDepth = "concise")) }
        )

        // Balanced (default)
        ReasoningDepthOption(
            title = "Balanced",
            description = "Moderate reasoning for everyday questions",
            selected = prefs.reasoningDepth == "balanced",
            darkTheme = darkTheme,
            onClick = { onPrefsChange(prefs.copy(reasoningDepth = "balanced")) }
        )

        // Comprehensive
        ReasoningDepthOption(
            title = "Comprehensive",
            description = "Deep analysis with step-by-step reasoning for complex problems",
            selected = prefs.reasoningDepth == "comprehensive",
            darkTheme = darkTheme,
            onClick = { onPrefsChange(prefs.copy(reasoningDepth = "comprehensive")) }
        )
    }
}

@Composable
private fun ReasoningDepthOption(
    title: String,
    description: String,
    selected: Boolean,
    darkTheme: Boolean,
    onClick: () -> Unit
) {
    val titleColor = if (darkTheme) HeaderPanelTokens.TitleDark else HeaderPanelTokens.TitleLight
    val subtleColor = if (darkTheme) HeaderPanelTokens.SubtleDark else HeaderPanelTokens.SubtleLight

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = HeaderPanelTokens.Accent
            )
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp)
        ) {
            Text(
                title,
                color = titleColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                description,
                color = subtleColor,
                fontSize = 11.sp,
                lineHeight = 14.sp
            )
        }
    }
}

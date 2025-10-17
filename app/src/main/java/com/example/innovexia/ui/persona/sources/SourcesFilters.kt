package com.example.innovexia.ui.persona.sources

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.innovexia.ui.theme.DarkColors
import androidx.compose.ui.window.PopupProperties
import com.example.innovexia.ui.theme.InnovexiaColors
import com.example.innovexia.ui.theme.LightColors

/**
 * Filters bar with chips - Simplified Material 3 design
 */
@Composable
fun SourcesFilters(
    filter: SourceType?,
    query: String,
    sort: String,
    selecting: Boolean,
    onFilterChange: (SourceType?) -> Unit,
    onQueryChange: (String) -> Unit,
    onSortChange: (String) -> Unit,
    onToggleSelect: () -> Unit,
    darkTheme: Boolean,
    modifier: Modifier = Modifier,
    hideSearchAndControls: Boolean = false // Hide search, Recent, Select
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Type filter chips - Centered horizontally
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
            FilterChip(
                label = "All",
                selected = filter == null,
                onClick = { onFilterChange(null) },
                darkTheme = darkTheme
            )
            FilterChip(
                label = "URLs",
                selected = filter == SourceType.URL,
                onClick = { onFilterChange(SourceType.URL) },
                darkTheme = darkTheme
            )
            FilterChip(
                label = "Files",
                selected = filter == SourceType.FILE,
                onClick = { onFilterChange(SourceType.FILE) },
                darkTheme = darkTheme
            )
            FilterChip(
                label = "Images",
                selected = filter == SourceType.IMAGE,
                onClick = { onFilterChange(SourceType.IMAGE) },
                darkTheme = darkTheme
            )
        }

        // Only show search + sort + select if not hidden
        if (!hideSearchAndControls) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Search field
                SearchField(
                    value = query,
                    onValueChange = onQueryChange,
                    darkTheme = darkTheme,
                    modifier = Modifier.weight(1f)
                )

                // Sort dropdown
                SortDropdown(
                    currentSort = sort,
                    onSortChange = onSortChange,
                    darkTheme = darkTheme
                )

                // Select toggle
                TextButton(
                    onClick = onToggleSelect,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = if (selecting) {
                            if (darkTheme) InnovexiaColors.GoldDim else InnovexiaColors.Gold
                        } else {
                            if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText
                        }
                    )
                ) {
                    Text(
                        text = "Select",
                        fontSize = 14.sp,
                        fontWeight = if (selecting) FontWeight.SemiBold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * Filter chip with Material 3 design
 */
@Composable
private fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    darkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) {
            Color(0xFFE6B84A).copy(alpha = 0.2f)
        } else {
            Color(0xFF2A2A2A).copy(alpha = 0.6f)
        },
        label = "chip_bg"
    )

    val textColor by animateColorAsState(
        targetValue = if (selected) {
            Color(0xFFE6B84A)
        } else {
            Color(0xFF94A3B8)
        },
        label = "chip_text"
    )

    Surface(
        onClick = onClick,
        modifier = modifier.height(32.dp),
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        border = if (selected) BorderStroke(1.dp, Color(0xFFE6B84A).copy(alpha = 0.6f)) else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = textColor
            )
        }
    }
}

/**
 * Search field
 */
@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    darkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (darkTheme) Color(0xFF1E2530) else Color(0xFFF5F5F5)),
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            color = if (darkTheme) DarkColors.PrimaryText else LightColors.PrimaryText
        ),
        cursorBrush = SolidColor(if (darkTheme) InnovexiaColors.GoldDim else InnovexiaColors.Gold),
        singleLine = true,
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText,
                    modifier = Modifier.size(18.dp)
                )
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            text = "Search sources…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText,
                            fontSize = 14.sp
                        )
                    }
                    innerTextField()
                }
                if (value.isNotEmpty()) {
                    IconButton(
                        onClick = { onValueChange("") },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Clear,
                            contentDescription = "Clear",
                            tint = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    )
}

/**
 * Sort dropdown
 */
@Composable
private fun SortDropdown(
    currentSort: String,
    onSortChange: (String) -> Unit,
    darkTheme: Boolean
) {
    var expanded by remember { mutableStateOf(false) }
    val sortOptions = listOf("Recent", "A→Z", "Size", "Type")

    Box {
        Surface(
            onClick = { expanded = true },
            modifier = Modifier.height(44.dp),
            shape = RoundedCornerShape(12.dp),
            color = if (darkTheme) Color(0xFF1E2530) else Color(0xFFF5F5F5)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = currentSort,
                    color = if (darkTheme) DarkColors.PrimaryText else LightColors.PrimaryText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = Icons.Rounded.ArrowDropDown,
                    contentDescription = null,
                    tint = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(
                if (darkTheme) Color(0xFF1E2530) else Color.White,
                RoundedCornerShape(12.dp)
            )
        ) {
            sortOptions.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option,
                            color = if (option == currentSort) {
                                if (darkTheme) InnovexiaColors.GoldDim else InnovexiaColors.Gold
                            } else {
                                if (darkTheme) DarkColors.PrimaryText else LightColors.PrimaryText
                            },
                            fontSize = 14.sp,
                            fontWeight = if (option == currentSort) FontWeight.SemiBold else FontWeight.Medium
                        )
                    },
                    onClick = {
                        onSortChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

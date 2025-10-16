package com.example.innovexia.ui.persona

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.innovexia.core.persona.InnoPersonaDefaults
import com.example.innovexia.ui.theme.InnovexiaTheme

/**
 * Enhanced Persona Card V2 with:
 * - Immersive design with glass-morphism
 * - Active indicator with animated glow
 * - Created & last used timestamps
 * - 3-line bio with fade effect
 * - Long-press gesture for quick actions
 * - Better visual hierarchy
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun PersonaCardV2(
    persona: Persona,
    isActive: Boolean,
    onSelect: (Persona) -> Unit,
    onStar: (Persona) -> Unit,
    onDuplicate: (Persona) -> Unit,
    onRename: (Persona) -> Unit,
    onDelete: (Persona) -> Unit,
    modifier: Modifier = Modifier,
    showImport: Boolean = false,
    onImport: ((Persona) -> Unit)? = null,
    onEdit: ((Persona) -> Unit)? = null,
    onMakePublic: ((Persona) -> Unit)? = null
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showPublicDialog by remember { mutableStateOf(false) }

    // Check if this is Inno (the default persona)
    val isInno = persona.id == InnoPersonaDefaults.INNO_PERSONA_ID

    // Animated glow effect for active state
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    // Card styling with glass-morphism and active glow
    val goldColor = InnovexiaTheme.colors.goldDim
    val cardBorderColor = if (isActive) {
        goldColor
    } else {
        InnovexiaTheme.colors.personaCardBorder
    }

    val cardBorderWidth = if (isActive) 2.dp else 1.dp

    val cardModifier = if (isActive) {
        modifier
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = goldColor.copy(alpha = 0.25f)
            )
            .drawBehind {
                // Animated glow ring
                drawRoundRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            goldColor.copy(alpha = glowAlpha),
                            Color.Transparent
                        )
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()),
                    style = Stroke(width = 4.dp.toPx())
                )
            }
    } else {
        modifier
    }

    Card(
        modifier = cardModifier
            .fillMaxWidth()
            .wrapContentHeight() // Allow card to size based on content
            .combinedClickable(
                onClick = { onSelect(persona) },
                onLongClick = { menuExpanded = true },
                onClickLabel = "Select ${persona.name}",
                onLongClickLabel = "Show options for ${persona.name}"
            )
            .semantics {
                role = Role.Button
                contentDescription = "Persona card: ${persona.name}. ${persona.summary}"
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = InnovexiaTheme.colors.personaCardBg.copy(alpha = 0.95f)
        ),
        border = BorderStroke(cardBorderWidth, cardBorderColor)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(
                        start = 14.dp,
                        end = 14.dp,
                        top = if (isActive) 18.dp else 14.dp, // Extra top padding when active
                        bottom = 14.dp
                    ),
                verticalArrangement = Arrangement.spacedBy(12.dp) // Slightly more spacing
            ) {
                // ═══ Header Row: Avatar + Name + Menu ═══
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar with colored ring
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .border(2.5.dp, Color(persona.color), CircleShape)
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(InnovexiaTheme.colors.personaCardBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = persona.initial,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(persona.color),
                            fontSize = 14.sp
                        )
                    }

                    // Name - constrain width when active to prevent overlap
                    Text(
                        text = persona.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = if (isActive) {
                            Modifier.weight(1f, fill = false).widthIn(max = 80.dp)
                        } else {
                            Modifier.weight(1f)
                        }
                    )

                    // Active badge inline with header (instead of absolutely positioned)
                    if (isActive) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = InnovexiaTheme.colors.goldDim.copy(alpha = 0.95f),
                            shadowElevation = 4.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = InnovexiaTheme.colors.onGold,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "ACTIVE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = InnovexiaTheme.colors.onGold
                                )
                            }
                        }
                    }

                    // Overflow menu
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options for ${persona.name}",
                                tint = InnovexiaTheme.colors.personaMutedText,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            if (onEdit != null) {
                                DropdownMenuItem(
                                    text = { Text("Edit") },
                                    onClick = {
                                        menuExpanded = false
                                        onEdit(persona)
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Rename") },
                                onClick = {
                                    menuExpanded = false
                                    onRename(persona)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Duplicate") },
                                onClick = {
                                    menuExpanded = false
                                    onDuplicate(persona)
                                }
                            )
                            if (!showImport && onMakePublic != null) {
                                DropdownMenuItem(
                                    text = { Text("Make Public") },
                                    onClick = {
                                        menuExpanded = false
                                        showPublicDialog = true
                                    }
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    menuExpanded = false
                                    showDeleteDialog = true
                                }
                            )
                        }
                    }
                }

                // ═══ Bio/Summary (3 lines with fade) ═══
                Text(
                    text = persona.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 14.sp,
                    color = InnovexiaTheme.colors.personaMutedText.copy(alpha = 0.95f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 54.dp, max = 72.dp) // Consistent bio height
                )

                // ═══ Metadata Row: Created & Last Used ═══
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Created date
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Created",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            color = InnovexiaTheme.colors.personaMutedText.copy(alpha = 0.7f)
                        )
                        Text(
                            text = persona.createdAtFormatted.ifEmpty { "Recently" },
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = InnovexiaTheme.colors.personaMutedText
                        )
                    }

                    // Last used
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Last used",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            color = InnovexiaTheme.colors.personaMutedText.copy(alpha = 0.7f)
                        )
                        Text(
                            text = persona.lastUsedFormatted ?: "Never",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (persona.lastUsedFormatted != null) {
                                InnovexiaTheme.colors.goldDim.copy(alpha = 0.9f)
                            } else {
                                InnovexiaTheme.colors.personaMutedText
                            }
                        )
                    }
                }

                // ═══ Tags Row (including Inno badge) ═══
                if (persona.tags.isNotEmpty() || isInno) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 26.dp) // Prevent tags from taking too much space
                    ) {
                        // Show DEFAULT badge for Inno
                        if (isInno) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(persona.color).copy(alpha = 0.2f),
                                contentColor = Color(persona.color)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = null,
                                        tint = Color(persona.color),
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Text(
                                        text = "DEFAULT",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Regular tags
                        persona.tags.take(if (isInno) 2 else 3).forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF1E293B),
                                contentColor = Color(0xFF94A3B8)
                            ) {
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        val visibleCount = if (isInno) 2 else 3
                        if (persona.tags.size > visibleCount) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF1E293B).copy(alpha = 0.5f),
                                contentColor = Color(0xFF94A3B8)
                            ) {
                                Text(
                                    text = "+${persona.tags.size - visibleCount}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }

                // ═══ Action Row: Select/Import + Star ═══
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Select/Import button
                    if (showImport && onImport != null) {
                        Button(
                            onClick = { onImport(persona) },
                            modifier = Modifier.weight(1f).height(36.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Text("Import", style = MaterialTheme.typography.labelMedium, fontSize = 13.sp)
                        }
                    } else {
                        Button(
                            onClick = { onSelect(persona) },
                            modifier = Modifier.weight(1f).height(36.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = if (isActive) {
                                ButtonDefaults.buttonColors(
                                    containerColor = InnovexiaTheme.colors.goldDim,
                                    contentColor = InnovexiaTheme.colors.onGold
                                )
                            } else {
                                ButtonDefaults.buttonColors()
                            }
                        ) {
                            Text("Select", style = MaterialTheme.typography.labelMedium, fontSize = 13.sp)
                        }
                    }

                    // Star icon button
                    IconButton(
                        onClick = { onStar(persona) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (persona.starred) Icons.Filled.Star else Icons.Outlined.Star,
                            contentDescription = if (persona.starred) "Unstar ${persona.name}" else "Star ${persona.name}",
                            tint = if (persona.starred) InnovexiaTheme.colors.goldDim else InnovexiaTheme.colors.personaMutedText,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

        }
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Persona?") },
            text = { Text("Are you sure you want to delete \"${persona.name}\"? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete(persona)
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Make Public Confirmation Dialog
    if (showPublicDialog && onMakePublic != null) {
        AlertDialog(
            onDismissRequest = { showPublicDialog = false },
            title = { Text("Make Persona Public?") },
            text = { Text("\"${persona.name}\" will be visible to all users and they can import it to their collection.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPublicDialog = false
                        onMakePublic(persona)
                    }
                ) {
                    Text("Make Public")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPublicDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Previews
// ═════════════════════════════════════════════════════════════════════════════

@Preview(name = "Active Card V2 - Dark", showBackground = true, backgroundColor = 0xFF0F172A)
@Composable
private fun PersonaCardV2Preview_Active() {
    InnovexiaTheme(darkTheme = true) {
        Box(modifier = Modifier.padding(16.dp).width(180.dp)) {
            PersonaCardV2(
                persona = Persona(
                    id = "1",
                    name = "Code Assistant",
                    initial = "C",
                    color = 0xFF60A5FA,
                    summary = "Expert in Kotlin, Jetpack Compose, and Android development. Provides clean, idiomatic code with best practices and modern architecture patterns.",
                    tags = listOf("Coding", "Android", "Expert"),
                    starred = true,
                    updatedAt = "1h",
                    createdAtFormatted = "Jan 15, 2024",
                    lastUsedFormatted = "2h ago"
                ),
                isActive = true,
                onSelect = {},
                onStar = {},
                onDuplicate = {},
                onRename = {},
                onDelete = {}
            )
        }
    }
}

@Preview(name = "Inactive Card V2 - Dark", showBackground = true, backgroundColor = 0xFF0F172A)
@Composable
private fun PersonaCardV2Preview_Inactive() {
    InnovexiaTheme(darkTheme = true) {
        Box(modifier = Modifier.padding(16.dp).width(180.dp)) {
            PersonaCardV2(
                persona = Persona(
                    id = "2",
                    name = "Research Analyst",
                    initial = "R",
                    color = 0xFF34D399,
                    summary = "Deep researcher with systematic approach to complex topics and data synthesis. Excellent at finding patterns and insights.",
                    tags = listOf("Research", "Analysis"),
                    starred = false,
                    updatedAt = "2d",
                    createdAtFormatted = "Dec 20, 2023",
                    lastUsedFormatted = null
                ),
                isActive = false,
                onSelect = {},
                onStar = {},
                onDuplicate = {},
                onRename = {},
                onDelete = {}
            )
        }
    }
}

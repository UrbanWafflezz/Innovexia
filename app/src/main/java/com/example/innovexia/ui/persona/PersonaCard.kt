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
fun PersonaCard(
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

    // Card styling with green glow for active state
    val greenGlow = Color(0xFF10B981) // Emerald green
    val cardBorderColor = if (isActive) {
        greenGlow
    } else {
        InnovexiaTheme.colors.personaCardBorder
    }

    val cardBorderWidth = if (isActive) 2.5.dp else 1.dp

    val cardModifier = if (isActive) {
        modifier
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = greenGlow.copy(alpha = 0.4f)
            )
            .drawBehind {
                // Animated green glow ring
                drawRoundRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            greenGlow.copy(alpha = glowAlpha * 0.8f),
                            Color.Transparent
                        )
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()),
                    style = Stroke(width = 6.dp.toPx())
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
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // ═══ Persona Name ═══
                Text(
                    text = persona.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().padding(end = 36.dp) // Space for menu button
                )

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

                // ═══ Tags Row ═══
                if (persona.tags.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 26.dp) // Prevent tags from taking too much space
                    ) {
                        persona.tags.take(3).forEach { tag ->
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
                        if (persona.tags.size > 3) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF1E293B).copy(alpha = 0.5f),
                                contentColor = Color(0xFF94A3B8)
                            ) {
                                Text(
                                    text = "+${persona.tags.size - 3}",
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

            // ═══ Overflow Menu (absolutely positioned) ═══
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
            ) {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(32.dp)
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

@Preview(name = "Active Card - Dark", showBackground = true, backgroundColor = 0xFF0F172A)
@Composable
private fun PersonaCardPreview_Active() {
    InnovexiaTheme(darkTheme = true) {
        Box(modifier = Modifier.padding(16.dp).width(180.dp)) {
            PersonaCard(
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

@Preview(name = "Inactive Card - Dark", showBackground = true, backgroundColor = 0xFF0F172A)
@Composable
private fun PersonaCardPreview_Inactive() {
    InnovexiaTheme(darkTheme = true) {
        Box(modifier = Modifier.padding(16.dp).width(180.dp)) {
            PersonaCard(
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

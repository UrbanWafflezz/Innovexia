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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
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

    // Card styling matching side menu chat cards - subtle active state
    val cardBorderColor = Color(0xFF404040).copy(alpha = 0.5f)
    val cardBorderWidth = 1.dp

    Card(
        modifier = modifier
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
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1F1F1F)
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
                // ═══ Persona Name with Active Indicator ═══
                Row(
                    modifier = Modifier.fillMaxWidth().padding(end = 36.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = persona.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFFD4AF37),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    // Subtle active indicator dot
                    if (isActive) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE6B84A))
                        )
                    }
                }

                // ═══ Bio/Summary (3 lines with fade) ═══
                Text(
                    text = persona.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 14.sp,
                    color = Color(0xFFA89968),
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
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = persona.createdAtFormatted.ifEmpty { "Recently" },
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFA89968)
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
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = persona.lastUsedFormatted ?: "Never",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (persona.lastUsedFormatted != null) {
                                Color(0xFFE6B84A)
                            } else {
                                Color(0xFFA89968)
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
                                color = Color(0xFF2A2A2A),
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
                                color = Color(0xFF2A2A2A).copy(alpha = 0.7f),
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

                // ═══ Action Row: Active/Select + Turn Off (if active) + Star ═══
                if (showImport && onImport != null) {
                    // Import button for public personas
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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

                        // Star icon button
                        IconButton(
                            onClick = { onStar(persona) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (persona.starred) Icons.Filled.Star else Icons.Outlined.Star,
                                contentDescription = if (persona.starred) "Unstar ${persona.name}" else "Star ${persona.name}",
                                tint = if (persona.starred) Color(0xFFE6B84A) else Color(0xFF94A3B8),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                } else if (isActive) {
                    // Active persona: Show status chip and Turn Off button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Active status chip (not clickable)
                        Surface(
                            modifier = Modifier.height(36.dp),
                            shape = RoundedCornerShape(18.dp),
                            color = Color(0xFF2A2A2A).copy(alpha = 0.6f),
                            border = BorderStroke(1.dp, Color(0xFFE6B84A).copy(alpha = 0.3f))
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Active",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color(0xFFE6B84A).copy(alpha = 0.9f)
                                )
                            }
                        }

                        // Turn Off button
                        OutlinedButton(
                            onClick = { onSelect(persona) },
                            modifier = Modifier.weight(1f).height(36.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFA89968)
                            ),
                            border = BorderStroke(1.dp, Color(0xFF404040).copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "Turn Off",
                                style = MaterialTheme.typography.labelMedium,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }

                        // Star icon button
                        IconButton(
                            onClick = { onStar(persona) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (persona.starred) Icons.Filled.Star else Icons.Outlined.Star,
                                contentDescription = if (persona.starred) "Unstar ${persona.name}" else "Star ${persona.name}",
                                tint = if (persona.starred) Color(0xFFE6B84A) else Color(0xFF94A3B8),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                } else {
                    // Inactive persona: Show Select button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { onSelect(persona) },
                            modifier = Modifier.weight(1f).height(36.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFD4AF37)
                            ),
                            border = BorderStroke(1.dp, Color(0xFF404040).copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "Select",
                                style = MaterialTheme.typography.labelMedium,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }

                        // Star icon button
                        IconButton(
                            onClick = { onStar(persona) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (persona.starred) Icons.Filled.Star else Icons.Outlined.Star,
                                contentDescription = if (persona.starred) "Unstar ${persona.name}" else "Star ${persona.name}",
                                tint = if (persona.starred) Color(0xFFE6B84A) else Color(0xFF94A3B8),
                                modifier = Modifier.size(22.dp)
                            )
                        }
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
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(20.dp)
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier
                        .background(
                            color = Color(0xFF1E2329),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = Color(0xFF404040).copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp)
                        )
                ) {
                    // Turn Off option (only for active personas)
                    android.util.Log.d("PersonaCard", "Menu expanded - persona=${persona.name}, isActive=$isActive")
                    if (isActive) {
                        android.util.Log.d("PersonaCard", "Showing Turn Off menu item for ${persona.name}")
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Turn Off",
                                    color = Color(0xFFD4AF37),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            onClick = {
                                android.util.Log.d("PersonaCard", "Turn Off clicked for ${persona.name}")
                                menuExpanded = false
                                onSelect(persona)
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.PowerSettingsNew,
                                    contentDescription = null,
                                    tint = Color(0xFFA89968),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = Color(0xFF404040).copy(alpha = 0.3f)
                        )
                    }

                    if (onEdit != null) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Edit",
                                    color = Color(0xFFD4AF37),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onEdit(persona)
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = Color(0xFFA89968),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = {
                            Text(
                                "Rename",
                                color = Color(0xFFD4AF37),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onRename(persona)
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.DriveFileRenameOutline,
                                contentDescription = null,
                                tint = Color(0xFFA89968),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                "Duplicate",
                                color = Color(0xFFD4AF37),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onDuplicate(persona)
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = null,
                                tint = Color(0xFFA89968),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    )
                    if (!showImport && onMakePublic != null) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Make Public",
                                    color = Color(0xFFD4AF37),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                showPublicDialog = true
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Public,
                                    contentDescription = null,
                                    tint = Color(0xFFA89968),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = Color(0xFF404040).copy(alpha = 0.3f)
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                "Delete",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            showDeleteDialog = true
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
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

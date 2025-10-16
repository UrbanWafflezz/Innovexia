package com.example.innovexia.ui.persona.sources

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.innovexia.ui.theme.DarkColors
import com.example.innovexia.ui.theme.InnovexiaColors
import com.example.innovexia.ui.theme.LightColors

/**
 * Empty state for different scenarios
 */
@Composable
fun EmptyState(
    type: EmptyStateType,
    onEnableSources: (() -> Unit)? = null,
    onAddUrl: (() -> Unit)? = null,
    onAddFile: (() -> Unit)? = null,
    onAddImage: (() -> Unit)? = null,
    darkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val data = when (type) {
            EmptyStateType.DISABLED -> EmptyStateData(
                icon = Icons.Rounded.SourceOff,
                title = "Sources Disabled",
                message = "Enable Sources to attach files, images, and links to this persona for enhanced context.",
                actionLabel = "Enable Sources",
                onAction = onEnableSources
            )
            EmptyStateType.NO_ITEMS -> EmptyStateData(
                icon = Icons.Rounded.Source,
                title = "No Sources Yet",
                message = "Add URLs, files, or images to provide additional context for this persona.",
                actionLabel = "Add URL",
                onAction = onAddUrl
            )
            EmptyStateType.FILTER_EMPTY_URLS -> EmptyStateData(
                icon = Icons.Rounded.Link,
                title = "No URLs",
                message = "You haven't added any URLs yet. Add your first URL to get started.",
                actionLabel = "Add URL",
                onAction = onAddUrl
            )
            EmptyStateType.FILTER_EMPTY_FILES -> EmptyStateData(
                icon = Icons.Rounded.InsertDriveFile,
                title = "No Files",
                message = "You haven't added any files yet. Add your first file to get started.",
                actionLabel = "Add File",
                onAction = onAddFile
            )
            EmptyStateType.FILTER_EMPTY_IMAGES -> EmptyStateData(
                icon = Icons.Rounded.Image,
                title = "No Images",
                message = "You haven't added any images yet. Add your first image to get started.",
                actionLabel = "Add Image",
                onAction = onAddImage
            )
            is EmptyStateType.SEARCH_EMPTY -> EmptyStateData(
                icon = Icons.Rounded.SearchOff,
                title = "No Results",
                message = "No sources match '${type.query}'. Try a different search term.",
                actionLabel = null,
                onAction = null
            )
        }

        // Icon
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(60.dp))
                .background(
                    if (darkTheme) Color(0xFF1E2530) else Color(0xFFF5F5F5)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = data.icon,
                contentDescription = null,
                tint = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText,
                modifier = Modifier.size(56.dp)
            )
        }

        Spacer(Modifier.height(24.dp))

        // Title
        Text(
            text = data.title,
            color = if (darkTheme) DarkColors.PrimaryText else LightColors.PrimaryText,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(12.dp))

        // Message
        Text(
            text = data.message,
            color = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 280.dp)
        )

        // Action button
        if (data.actionLabel != null && data.onAction != null) {
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = data.onAction,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (darkTheme) InnovexiaColors.GoldDim else InnovexiaColors.Gold,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = data.actionLabel,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * Types of empty states
 */
sealed class EmptyStateType {
    object DISABLED : EmptyStateType()
    object NO_ITEMS : EmptyStateType()
    object FILTER_EMPTY_URLS : EmptyStateType()
    object FILTER_EMPTY_FILES : EmptyStateType()
    object FILTER_EMPTY_IMAGES : EmptyStateType()
    data class SEARCH_EMPTY(val query: String) : EmptyStateType()
}

/**
 * Data holder for empty state content
 */
private data class EmptyStateData(
    val icon: ImageVector,
    val title: String,
    val message: String,
    val actionLabel: String?,
    val onAction: (() -> Unit)?
)

// Fallback icon for API < 35
private val Icons.Rounded.SourceOff: ImageVector
    get() = Icons.Rounded.Block

private val Icons.Rounded.Source: ImageVector
    get() = Icons.Rounded.Folder

private val Icons.Rounded.SearchOff: ImageVector
    get() = Icons.Rounded.Search

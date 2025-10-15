package com.example.innovexia.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.innovexia.ui.theme.InnovexiaColors

/**
 * Avatar component with fallback to initials
 * Loads image with Coil if photoUrl provided, otherwise shows initials
 */
@Composable
fun Avatar(
    name: String,
    photoUrl: String?,
    size: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(InnovexiaColors.DarkSurfaceElevated),
        contentAlignment = Alignment.Center
    ) {
        if (!photoUrl.isNullOrBlank()) {
            // Load image with Coil
            AsyncImage(
                model = photoUrl,
                contentDescription = "Profile picture",
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            // Show initials
            Text(
                text = getInitials(name),
                color = InnovexiaColors.DarkTextPrimary,
                fontSize = (size.value / 2.2).sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Extract initials from name or email
 */
private fun getInitials(name: String): String {
    if (name.isBlank()) return "?"

    val parts = name.trim().split(" ", "@", ".")
    return when {
        parts.size >= 2 -> {
            // First and last name initials
            "${parts.first().firstOrNull()?.uppercase() ?: ""}${parts.last().firstOrNull()?.uppercase() ?: ""}"
        }
        else -> {
            // Single letter or first two letters
            parts.first().take(2).uppercase()
        }
    }
}

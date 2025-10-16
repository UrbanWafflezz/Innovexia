package com.example.innovexia.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.innovexia.ui.theme.InnovexiaColors

/**
 * Displays a stack of persona initials (up to 3), with +N indicator if more
 */
@Composable
fun PersonaStack(
    initials: List<String>,
    modifier: Modifier = Modifier
) {
    val shown = initials.take(3)

    Box(modifier = modifier) {
        shown.forEachIndexed { idx, s ->
            PersonaChipInitial(
                text = s.take(1).uppercase(),
                modifier = Modifier
                    .offset(x = (idx * 14).dp)
                    .size(28.dp)
            )
        }
        if (initials.size > 3) {
            PersonaChipInitial(
                text = "+${initials.size - 3}",
                modifier = Modifier
                    .offset(x = (3 * 14).dp)
                    .size(28.dp),
                dim = true
            )
        }
    }
}

/**
 * Individual persona chip showing an initial
 */
@Composable
fun PersonaChipInitial(
    text: String,
    modifier: Modifier = Modifier,
    dim: Boolean = false
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = if (dim) InnovexiaColors.DarkSurface else InnovexiaColors.DarkSurfaceElevated,
        border = BorderStroke(1.dp, InnovexiaColors.DarkBorder.copy(alpha = 0.6f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                color = InnovexiaColors.DarkTextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

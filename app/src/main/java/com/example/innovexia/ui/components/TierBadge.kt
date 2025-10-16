package com.example.innovexia.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.innovexia.data.models.TierInfo

/**
 * Tier badge component showing subscription plan
 * Displays colored badge with tier label
 */
@Composable
fun TierBadge(
    tier: TierInfo,
    modifier: Modifier = Modifier
) {
    val backgroundColor = tier.color.copy(alpha = 0.12f)
    val borderColor = tier.color.copy(alpha = 0.5f)

    Surface(
        shape = RoundedCornerShape(999.dp),
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
    ) {
        Text(
            text = tier.label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall.copy(
                color = tier.color,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}

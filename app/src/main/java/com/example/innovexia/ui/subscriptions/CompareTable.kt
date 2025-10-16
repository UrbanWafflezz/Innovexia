package com.example.innovexia.ui.subscriptions

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

data class FeatureRow(
    val category: String?,
    val feature: String,
    val free: CompareValue,
    val plus: CompareValue,
    val pro: CompareValue,
    val master: CompareValue
)

sealed class CompareValue {
    object Yes : CompareValue()
    object No : CompareValue()
    data class Text(val value: String) : CompareValue()
}

private val compareFeatures = listOf(
    FeatureRow(
        category = "AI Models",
        feature = "Gemini 2.5 Flash",
        free = CompareValue.Yes,
        plus = CompareValue.Yes,
        pro = CompareValue.Yes,
        master = CompareValue.Yes
    ),
    FeatureRow(
        category = null,
        feature = "Gemini 2.5 Pro",
        free = CompareValue.No,
        plus = CompareValue.Yes,
        pro = CompareValue.Yes,
        master = CompareValue.Yes
    ),
    FeatureRow(
        category = null,
        feature = "GPT-5",
        free = CompareValue.No,
        plus = CompareValue.No,
        pro = CompareValue.Yes,
        master = CompareValue.Yes
    ),
    FeatureRow(
        category = null,
        feature = "Claude 4.5",
        free = CompareValue.No,
        plus = CompareValue.No,
        pro = CompareValue.Yes,
        master = CompareValue.Yes
    ),
    FeatureRow(
        category = null,
        feature = "Perplexity Search",
        free = CompareValue.No,
        plus = CompareValue.No,
        pro = CompareValue.No,
        master = CompareValue.Yes
    ),
    FeatureRow(
        category = "Memory & Storage",
        feature = "Memory capacity",
        free = CompareValue.Text("50 entries"),
        plus = CompareValue.Text("500 entries"),
        pro = CompareValue.Text("Unlimited"),
        master = CompareValue.Text("Unlimited")
    ),
    FeatureRow(
        category = null,
        feature = "Sources (items)",
        free = CompareValue.Text("5"),
        plus = CompareValue.Text("50"),
        pro = CompareValue.Text("250"),
        master = CompareValue.Text("1,000+")
    ),
    FeatureRow(
        category = null,
        feature = "Max file size",
        free = CompareValue.Text("10MB"),
        plus = CompareValue.Text("50MB"),
        pro = CompareValue.Text("100MB"),
        master = CompareValue.Text("250MB")
    ),
    FeatureRow(
        category = null,
        feature = "Cloud backup",
        free = CompareValue.No,
        plus = CompareValue.Yes,
        pro = CompareValue.Yes,
        master = CompareValue.Yes
    ),
    FeatureRow(
        category = "Context & Performance",
        feature = "Context length",
        free = CompareValue.Text("32K"),
        plus = CompareValue.Text("128K"),
        pro = CompareValue.Text("256K"),
        master = CompareValue.Text("512K")
    ),
    FeatureRow(
        category = null,
        feature = "Priority lane",
        free = CompareValue.No,
        plus = CompareValue.No,
        pro = CompareValue.No,
        master = CompareValue.Yes
    ),
    FeatureRow(
        category = "Collaboration",
        feature = "Team spaces",
        free = CompareValue.No,
        plus = CompareValue.No,
        pro = CompareValue.Text("2 members"),
        master = CompareValue.Text("5 members")
    ),
    FeatureRow(
        category = null,
        feature = "Advanced personas",
        free = CompareValue.No,
        plus = CompareValue.No,
        pro = CompareValue.No,
        master = CompareValue.Yes
    )
)

@Composable
fun CompareTable(
    onSeeDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Compare Plans",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
            TextButton(onClick = onSeeDetails) {
                Text(
                    text = "See Details",
                    color = TierTokens.Master
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Table container with horizontal scroll
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(TierTokens.Radius),
            color = TierTokens.Card,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                // First column (pinned): Feature names
                Column(
                    modifier = Modifier
                        .width(180.dp)
                        .background(TierTokens.Card)
                ) {
                    // Header cell
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .padding(12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = "Features",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        )
                    }

                    // Feature rows
                    compareFeatures.forEachIndexed { index, row ->
                        if (row.category != null) {
                            // Category header
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .background(TierTokens.Surface)
                                    .padding(12.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = row.category,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TierTokens.Master
                                    )
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .background(
                                    if (index % 2 == 0) TierTokens.Card
                                    else TierTokens.Surface.copy(alpha = 0.3f)
                                )
                                .padding(12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = row.feature,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            )
                        }
                    }
                }

                // Tier columns
                listOf(
                    Triple("Free", TierTokens.Free, compareFeatures.map { it.free }),
                    Triple("Plus", TierTokens.Plus, compareFeatures.map { it.plus }),
                    Triple("Pro", TierTokens.Pro, compareFeatures.map { it.pro }),
                    Triple("Master", TierTokens.Master, compareFeatures.map { it.master })
                ).forEach { (tierName, tierColor, values) ->
                    Column(
                        modifier = Modifier.width(120.dp)
                    ) {
                        // Header with tier color
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .background(tierColor.copy(alpha = 0.15f))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = tierColor,
                                    modifier = Modifier.size(8.dp)
                                ) {}
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = tierName,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = tierColor
                                    )
                                )
                            }
                        }

                        // Value rows
                        values.forEachIndexed { index, value ->
                            val row = compareFeatures[index]

                            if (row.category != null) {
                                // Empty spacer for category header
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(40.dp)
                                        .background(TierTokens.Surface)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .background(
                                        if (index % 2 == 0) TierTokens.Card
                                        else TierTokens.Surface.copy(alpha = 0.3f)
                                    )
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                when (value) {
                                    is CompareValue.Yes -> {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Included",
                                            tint = tierColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    is CompareValue.No -> {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Not included",
                                            tint = Color.White.copy(alpha = 0.3f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    is CompareValue.Text -> {
                                        Text(
                                            text = value.value,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = Color.White.copy(alpha = 0.8f),
                                                textAlign = TextAlign.Center
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

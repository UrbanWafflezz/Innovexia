package com.example.innovexia.ui.previews

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.innovexia.ui.glass.GlassButton
import com.example.innovexia.ui.glass.GlassButtonStyle
import com.example.innovexia.ui.glass.GlassField
import com.example.innovexia.ui.glass.LiquidGlassSurface
import com.example.innovexia.ui.theme.*

/**
 * Global Consistency Preview
 *
 * Visual validation of the Innovexia design system showing:
 * - All color tokens (light/dark mode)
 * - Typography scale
 * - Spacing system
 * - Corner radii
 * - Button styles
 * - Input fields
 * - Cards and surfaces
 * - Glass components
 * - Icons and chips
 */

@Preview(name = "Light Mode", showBackground = true)
@Composable
fun GlobalConsistencyPreviewLight() {
    InnovexiaTheme(darkTheme = false) {
        GlobalConsistencyContent()
    }
}

@Preview(name = "Dark Mode", showBackground = true)
@Composable
fun GlobalConsistencyPreviewDark() {
    InnovexiaTheme(darkTheme = true) {
        GlobalConsistencyContent()
    }
}

@Composable
private fun GlobalConsistencyContent() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(InnovexiaDesign.Layout.ScreenPaddingHorizontal),
        verticalArrangement = Arrangement.spacedBy(InnovexiaDesign.Spacing.XXL)
    ) {
        item {
            Spacer(modifier = Modifier.height(InnovexiaDesign.Spacing.LG))
            SectionTitle("Typography Scale")
            TypographyShowcase()
        }

        item {
            SectionTitle("Color Palette")
            ColorPaletteShowcase()
        }

        item {
            SectionTitle("Button Styles")
            ButtonShowcase()
        }

        item {
            SectionTitle("Input Fields")
            InputFieldShowcase()
        }

        item {
            SectionTitle("Cards & Surfaces")
            CardShowcase()
        }

        item {
            SectionTitle("Glass Components")
            GlassShowcase()
        }

        item {
            SectionTitle("Chips & Tags")
            ChipShowcase()
        }

        item {
            SectionTitle("Spacing System")
            SpacingShowcase()
        }

        item {
            SectionTitle("Corner Radii")
            RadiusShowcase()
        }

        item {
            Spacer(modifier = Modifier.height(InnovexiaDesign.Spacing.XXL))
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.displayMedium,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(vertical = InnovexiaDesign.Spacing.MD)
    )
}

@Composable
private fun TypographyShowcase() {
    Column(
        verticalArrangement = Arrangement.spacedBy(InnovexiaDesign.Spacing.SM)
    ) {
        Text("Display Large", style = MaterialTheme.typography.displayLarge)
        Text("Display Medium", style = MaterialTheme.typography.displayMedium)
        Text("Title Large", style = MaterialTheme.typography.titleLarge)
        Text("Title Medium", style = MaterialTheme.typography.titleMedium)
        Text("Title Small", style = MaterialTheme.typography.titleSmall)
        Text("Body Large", style = MaterialTheme.typography.bodyLarge)
        Text("Body Medium", style = MaterialTheme.typography.bodyMedium)
        Text("Body Small", style = MaterialTheme.typography.bodySmall)
        Text("Label Large", style = MaterialTheme.typography.labelLarge)
        Text("Label Medium", style = MaterialTheme.typography.labelMedium)
        Text("Label Small", style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun ColorPaletteShowcase() {
    Column(
        verticalArrangement = Arrangement.spacedBy(InnovexiaDesign.Spacing.MD)
    ) {
        ColorRow("Primary", MaterialTheme.colorScheme.primary)
        ColorRow("Secondary", MaterialTheme.colorScheme.secondary)
        ColorRow("Surface", MaterialTheme.colorScheme.surface)
        ColorRow("Background", MaterialTheme.colorScheme.background)
        ColorRow("Gold", InnovexiaTheme.colors.goldDim)
        ColorRow("Coral", InnovexiaTheme.colors.coral)
        ColorRow("Blue Accent", InnovexiaColors.BlueAccent)
        ColorRow("Success", InnovexiaColors.Success)
        ColorRow("Warning", InnovexiaColors.Warning)
        ColorRow("Error", InnovexiaColors.Error)
    }
}

@Composable
private fun ColorRow(name: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(InnovexiaDesign.Spacing.MD)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(InnovexiaComponentShapes.Card)
                .background(color)
                .border(
                    width = InnovexiaDesign.Border.Thin,
                    color = MaterialTheme.colorScheme.outline,
                    shape = InnovexiaComponentShapes.Card
                )
        )
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun ButtonShowcase() {
    Column(
        verticalArrangement = Arrangement.spacedBy(InnovexiaDesign.Spacing.MD)
    ) {
        // Primary buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(InnovexiaDesign.Spacing.SM)
        ) {
            GlassButton(
                text = "Primary",
                onClick = {},
                style = GlassButtonStyle.Primary
            )
            GlassButton(
                text = "Secondary",
                onClick = {},
                style = GlassButtonStyle.Secondary
            )
            GlassButton(
                text = "Ghost",
                onClick = {},
                style = GlassButtonStyle.Ghost
            )
        }

        // Material buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(InnovexiaDesign.Spacing.SM)
        ) {
            Button(onClick = {}) {
                Text("Button")
            }
            OutlinedButton(onClick = {}) {
                Text("Outlined")
            }
            TextButton(onClick = {}) {
                Text("Text")
            }
        }
    }
}

@Composable
private fun InputFieldShowcase() {
    var text by remember { mutableStateOf("") }

    Column(
        verticalArrangement = Arrangement.spacedBy(InnovexiaDesign.Spacing.MD)
    ) {
        GlassField(
            value = text,
            onValueChange = { text = it },
            hint = "Glass input field"
        )

        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Material TextField") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun CardShowcase() {
    Column(
        verticalArrangement = Arrangement.spacedBy(InnovexiaDesign.Spacing.MD)
    ) {
        // Standard Material Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = InnovexiaComponentShapes.Card,
            elevation = CardDefaults.cardElevation(defaultElevation = InnovexiaDesign.Elevation.Card)
        ) {
            Column(
                modifier = Modifier.padding(InnovexiaDesign.Layout.CardPadding)
            ) {
                Text(
                    text = "Material Card",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(InnovexiaDesign.Spacing.SM))
                Text(
                    text = "This is a standard Material Design card using InnovexiaDesign tokens.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Surface variant
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = InnovexiaComponentShapes.Card,
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = InnovexiaDesign.Elevation.Low
        ) {
            Column(
                modifier = Modifier.padding(InnovexiaDesign.Layout.CardPadding)
            ) {
                Text(
                    text = "Surface Variant",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(InnovexiaDesign.Spacing.SM))
                Text(
                    text = "Surface with tonal elevation for subtle depth.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun GlassShowcase() {
    Column(
        verticalArrangement = Arrangement.spacedBy(InnovexiaDesign.Spacing.MD)
    ) {
        LiquidGlassSurface(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(InnovexiaDesign.Layout.CardPadding)
            ) {
                Text(
                    text = "Glass Surface",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(InnovexiaDesign.Spacing.SM))
                Text(
                    text = "Liquid glass surface with blur and gradient borders.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun ChipShowcase() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(InnovexiaDesign.Spacing.SM),
        modifier = Modifier.fillMaxWidth()
    ) {
        AssistChip(
            onClick = {},
            label = { Text("Assist") },
            leadingIcon = { Icon(Icons.Default.Star, contentDescription = null) }
        )
        FilterChip(
            selected = true,
            onClick = {},
            label = { Text("Filter") }
        )
        SuggestionChip(
            onClick = {},
            label = { Text("Suggestion") }
        )
    }
}

@Composable
private fun SpacingShowcase() {
    Column(
        verticalArrangement = Arrangement.spacedBy(InnovexiaDesign.Spacing.SM)
    ) {
        SpacingRow("XXS", InnovexiaDesign.Spacing.XXS)
        SpacingRow("XS", InnovexiaDesign.Spacing.XS)
        SpacingRow("SM", InnovexiaDesign.Spacing.SM)
        SpacingRow("MD", InnovexiaDesign.Spacing.MD)
        SpacingRow("LG", InnovexiaDesign.Spacing.LG)
        SpacingRow("XL", InnovexiaDesign.Spacing.XL)
        SpacingRow("XXL", InnovexiaDesign.Spacing.XXL)
        SpacingRow("XXXL", InnovexiaDesign.Spacing.XXXL)
    }
}

@Composable
private fun SpacingRow(name: String, size: androidx.compose.ui.unit.Dp) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(InnovexiaDesign.Spacing.MD)
    ) {
        Box(
            modifier = Modifier
                .width(size)
                .height(24.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = InnovexiaComponentShapes.Card
                )
        )
        Text(
            text = "$name = $size",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun RadiusShowcase() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(InnovexiaDesign.Spacing.MD),
        modifier = Modifier.fillMaxWidth()
    ) {
        RadiusBox("Small", InnovexiaDesign.Radius.Small)
        RadiusBox("Medium", InnovexiaDesign.Radius.Medium)
        RadiusBox("Large", InnovexiaDesign.Radius.Large)
        RadiusBox("XLarge", InnovexiaDesign.Radius.XLarge)
    }
}

@Composable
private fun RadiusBox(name: String, radius: androidx.compose.ui.unit.Dp) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(InnovexiaDesign.Spacing.XS)
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(radius))
                .background(MaterialTheme.colorScheme.primary)
        )
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

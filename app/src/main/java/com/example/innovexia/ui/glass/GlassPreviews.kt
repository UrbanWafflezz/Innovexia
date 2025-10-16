package com.example.innovexia.ui.glass

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.innovexia.ui.theme.InnovexiaTheme

@Preview(name = "LiquidGlassSurface Light", showBackground = true)
@Composable
fun LiquidGlassSurfacePreview_Light() {
    InnovexiaTheme(darkTheme = false) {
        Column(modifier = Modifier.padding(16.dp)) {
            LiquidGlassSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                darkTheme = false
            ) {
                Text(
                    text = "Liquid Glass Surface",
                    modifier = Modifier.padding(20.dp)
                )
            }
        }
    }
}

@Preview(name = "LiquidGlassSurface Dark", showBackground = true, backgroundColor = 0xFF0B1220)
@Composable
fun LiquidGlassSurfacePreview_Dark() {
    InnovexiaTheme(darkTheme = true) {
        Column(modifier = Modifier.padding(16.dp)) {
            LiquidGlassSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                darkTheme = true
            ) {
                Text(
                    text = "Liquid Glass Surface",
                    modifier = Modifier.padding(20.dp)
                )
            }
        }
    }
}

@Preview(name = "GlassField Light", showBackground = true)
@Composable
fun GlassFieldPreview_Light() {
    InnovexiaTheme(darkTheme = false) {
        var text by remember { mutableStateOf("") }
        Column(modifier = Modifier.padding(16.dp)) {
            GlassField(
                value = text,
                onValueChange = { text = it },
                hint = "Enter your email",
                leading = {
                    Icon(imageVector = Icons.Default.Email, contentDescription = null)
                },
                darkTheme = false
            )
        }
    }
}

@Preview(name = "GlassField Dark", showBackground = true, backgroundColor = 0xFF0B1220)
@Composable
fun GlassFieldPreview_Dark() {
    InnovexiaTheme(darkTheme = true) {
        var text by remember { mutableStateOf("") }
        Column(modifier = Modifier.padding(16.dp)) {
            GlassField(
                value = text,
                onValueChange = { text = it },
                hint = "Enter your email",
                leading = {
                    Icon(imageVector = Icons.Default.Email, contentDescription = null)
                },
                darkTheme = true
            )
        }
    }
}

@Preview(name = "GlassButtons Light", showBackground = true)
@Composable
fun GlassButtonsPreview_Light() {
    InnovexiaTheme(darkTheme = false) {
        Column(modifier = Modifier.padding(16.dp)) {
            GlassButton(
                text = "Primary Button",
                onClick = { },
                style = GlassButtonStyle.Primary,
                modifier = Modifier.fillMaxWidth(),
                darkTheme = false
            )

            Spacer(Modifier.height(12.dp))

            GlassButton(
                text = "Secondary Button",
                onClick = { },
                style = GlassButtonStyle.Secondary,
                modifier = Modifier.fillMaxWidth(),
                darkTheme = false
            )

            Spacer(Modifier.height(12.dp))

            GlassButton(
                text = "Ghost Button",
                onClick = { },
                style = GlassButtonStyle.Ghost,
                modifier = Modifier.fillMaxWidth(),
                darkTheme = false
            )
        }
    }
}

@Preview(name = "GlassButtons Dark", showBackground = true, backgroundColor = 0xFF0B1220)
@Composable
fun GlassButtonsPreview_Dark() {
    InnovexiaTheme(darkTheme = true) {
        Column(modifier = Modifier.padding(16.dp)) {
            GlassButton(
                text = "Primary Button",
                onClick = { },
                style = GlassButtonStyle.Primary,
                modifier = Modifier.fillMaxWidth(),
                darkTheme = true
            )

            Spacer(Modifier.height(12.dp))

            GlassButton(
                text = "Secondary Button",
                onClick = { },
                style = GlassButtonStyle.Secondary,
                modifier = Modifier.fillMaxWidth(),
                darkTheme = true
            )

            Spacer(Modifier.height(12.dp))

            GlassButton(
                text = "Ghost Button",
                onClick = { },
                style = GlassButtonStyle.Ghost,
                modifier = Modifier.fillMaxWidth(),
                darkTheme = true
            )
        }
    }
}

@Preview(name = "GlassSegmented Light", showBackground = true)
@Composable
fun GlassSegmentedPreview_Light() {
    InnovexiaTheme(darkTheme = false) {
        var selected by remember { mutableStateOf(0) }
        Column(modifier = Modifier.padding(16.dp)) {
            GlassSegmented(
                options = listOf("Profile", "Subscriptions", "Security"),
                selectedIndex = selected,
                onSelected = { selected = it },
                modifier = Modifier.fillMaxWidth(),
                darkTheme = false
            )
        }
    }
}

@Preview(name = "GlassSegmented Dark", showBackground = true, backgroundColor = 0xFF0B1220)
@Composable
fun GlassSegmentedPreview_Dark() {
    InnovexiaTheme(darkTheme = true) {
        var selected by remember { mutableStateOf(0) }
        Column(modifier = Modifier.padding(16.dp)) {
            GlassSegmented(
                options = listOf("Profile", "Subscriptions", "Security"),
                selectedIndex = selected,
                onSelected = { selected = it },
                modifier = Modifier.fillMaxWidth(),
                darkTheme = true
            )
        }
    }
}

@Preview(name = "GlassAvatar Light", showBackground = true)
@Composable
fun GlassAvatarPreview_Light() {
    InnovexiaTheme(darkTheme = false) {
        Column(modifier = Modifier.padding(16.dp)) {
            GlassAvatar(
                initials = "AS",
                size = 80.dp,
                darkTheme = false
            )
        }
    }
}

@Preview(name = "GlassAvatar Dark", showBackground = true, backgroundColor = 0xFF0B1220)
@Composable
fun GlassAvatarPreview_Dark() {
    InnovexiaTheme(darkTheme = true) {
        Column(modifier = Modifier.padding(16.dp)) {
            GlassAvatar(
                initials = "AS",
                size = 80.dp,
                darkTheme = true
            )
        }
    }
}

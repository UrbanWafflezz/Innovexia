package com.example.innovexia.ui.sheets.personas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.innovexia.ui.theme.InnovexiaTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Public personas tab - Coming Soon with Beta Signup
 */
@Composable
fun PersonasTabPublic(
    query: String,
    personas: List<com.example.innovexia.ui.persona.Persona>,
    onImport: (com.example.innovexia.ui.persona.Persona) -> Unit,
    onStar: (com.example.innovexia.ui.persona.Persona) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var email by rememberSaveable { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Load beta signup count from local storage
    val sharedPrefs = remember {
        context.getSharedPreferences("public_personas_beta", android.content.Context.MODE_PRIVATE)
    }
    val signupCount by remember {
        mutableStateOf(sharedPrefs.getInt("signup_count", 0))
    }
    val spotsRemaining = 100 - signupCount

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.navigationBars)
            .imePadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Spacer(Modifier.height(32.dp))

        // Icon
        Surface(
            modifier = Modifier.size(80.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFFE6B84A).copy(alpha = 0.15f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Public,
                    contentDescription = null,
                    tint = Color(0xFFE6B84A),
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        // Title
        Text(
            text = "Public Personas",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFD4AF37),
            fontSize = 28.sp
        )

        // Coming Soon Badge
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF2A2A2A),
            border = BorderStroke(1.dp, Color(0xFFE6B84A).copy(alpha = 0.3f))
        ) {
            Text(
                text = "COMING SOON",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE6B84A),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                letterSpacing = 1.5.sp
            )
        }

        // Launch Date
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = null,
                tint = Color(0xFFA89968),
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "Available January 13, 2026",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFFA89968),
                fontSize = 16.sp
            )
        }

        Spacer(Modifier.height(8.dp))

        // Description
        Text(
            text = "Discover and import community-created personas to enhance your AI experience. Share your own personas with the world!",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF94A3B8),
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(Modifier.height(16.dp))

        // Beta Signup Section
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1E2329),
            border = BorderStroke(1.dp, Color(0xFF404040).copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Beta Title
                Text(
                    text = "Join the Beta",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFD4AF37),
                    fontSize = 18.sp
                )

                Text(
                    text = "Be among the first 100 testers to access Public Personas before the official launch. Beta could be released sooner!",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFA89968),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                // Spots Remaining
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "$spotsRemaining",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE6B84A),
                        fontSize = 24.sp
                    )
                    Text(
                        text = "spots remaining",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF94A3B8)
                    )
                }

                if (!showSuccess) {
                    // Email Input
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF2A323B).copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, Color(0xFF404040).copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = Color(0xFFA89968),
                                modifier = Modifier.size(20.dp)
                            )
                            BasicTextField(
                                value = email,
                                onValueChange = {
                                    email = it
                                    errorMessage = null
                                },
                                modifier = Modifier.weight(1f),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color(0xFFD4AF37),
                                    fontSize = 15.sp
                                ),
                                cursorBrush = SolidColor(Color(0xFFE6B84A)),
                                singleLine = true,
                                decorationBox = { innerTextField ->
                                    if (email.isEmpty()) {
                                        Text(
                                            text = "Enter your email",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFFA89968).copy(alpha = 0.6f),
                                            fontSize = 15.sp
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                        }
                    }

                    // Error Message
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp
                        )
                    }

                    // Sign Up Button
                    Button(
                        onClick = {
                            when {
                                email.isBlank() -> {
                                    errorMessage = "Please enter your email"
                                }
                                !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                                    errorMessage = "Please enter a valid email"
                                }
                                signupCount >= 100 -> {
                                    errorMessage = "Beta is full. Join the waitlist instead!"
                                }
                                else -> {
                                    scope.launch {
                                        isSubmitting = true
                                        errorMessage = null

                                        // TODO: Firebase integration
                                        // FirebaseFirestore.getInstance()
                                        //     .collection("public_personas_beta_signups")
                                        //     .add(mapOf(
                                        //         "email" to email,
                                        //         "timestamp" to FieldValue.serverTimestamp(),
                                        //         "platform" to "android"
                                        //     ))

                                        // Local storage for now
                                        delay(500) // Simulate network delay
                                        sharedPrefs.edit()
                                            .putInt("signup_count", signupCount + 1)
                                            .putString("user_beta_email", email)
                                            .apply()

                                        isSubmitting = false
                                        showSuccess = true
                                        email = ""
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        enabled = !isSubmitting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE6B84A),
                            contentColor = Color(0xFF1E2329)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color(0xFF1E2329),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Join Beta Waitlist",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                        }
                    }
                } else {
                    // Success State
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF34D399),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "You're on the list!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFD4AF37)
                        )
                        Text(
                            text = "We'll email you when the beta is ready.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Previews
// ═════════════════════════════════════════════════════════════════════════════

@Preview(name = "Public Tab - Coming Soon", showBackground = true, backgroundColor = 0xFF0F172A, widthDp = 360, heightDp = 800)
@Composable
private fun PersonasTabPublicPreview() {
    InnovexiaTheme(darkTheme = true) {
        PersonasTabPublic(
            query = "",
            personas = emptyList(),
            onImport = {},
            onStar = {}
        )
    }
}

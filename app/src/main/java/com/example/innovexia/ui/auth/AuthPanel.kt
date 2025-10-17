package com.example.innovexia.ui.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.innovexia.ui.theme.DarkColors
import com.example.innovexia.ui.theme.LightColors
import com.example.innovexia.ui.viewmodels.AuthViewModel

enum class AuthScreen {
    SignIn,
    SignUp,
    ForgotPassword
}

data class AuthState(
    val screen: AuthScreen = AuthScreen.SignIn,
    val email: String = "",
    val username: String = "",
    val password: String = "",
    val agreedToTerms: Boolean = false,
    val rememberMe: Boolean = false
)

/**
 * Auth panel with Sign in, Sign up, and Forgot password flows.
 * Now wired to Firebase Auth.
 */
@Composable
fun AuthPanel(
    onSignInSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    darkTheme: Boolean = isSystemInDarkTheme(),
    viewModel: AuthViewModel = viewModel()
) {
    var authState by remember { mutableStateOf(AuthState()) }
    val busy by viewModel.busy.collectAsState()
    val signedIn by viewModel.signedIn.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val rememberMe by viewModel.rememberMe.collectAsState(initial = false)
    val savedEmail by viewModel.savedEmail.collectAsState(initial = null)

    // Load saved email if remember me is enabled
    LaunchedEffect(rememberMe, savedEmail) {
        val email = savedEmail
        if (rememberMe && !email.isNullOrBlank()) {
            authState = authState.copy(
                email = email,
                rememberMe = true
            )
        }
    }

    // Observe error messages
    LaunchedEffect(Unit) {
        viewModel.error.collect { error ->
            error?.let {
                snackbarHostState.showSnackbar(it)
            }
        }
    }

    // Auto-dismiss on successful sign-in
    LaunchedEffect(signedIn) {
        if (signedIn) {
            onSignInSuccess()
        }
    }

    Box(modifier = modifier) {
        AnimatedContent(
            targetState = authState.screen,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "auth_screen"
        ) { screen ->
            when (screen) {
                AuthScreen.SignIn -> SignInView(
                    state = authState,
                    onStateChange = { authState = it },
                    onSignIn = { email, password ->
                        viewModel.signIn(email, password, authState.rememberMe)
                    },
                    busy = busy,
                    darkTheme = darkTheme
                )

                AuthScreen.SignUp -> SignUpView(
                    state = authState,
                    onStateChange = { authState = it },
                    onSignUp = { username, email, password ->
                        viewModel.signUp(username, email, password)
                    },
                    busy = busy,
                    darkTheme = darkTheme
                )

                AuthScreen.ForgotPassword -> ForgotPasswordView(
                    state = authState,
                    onStateChange = { authState = it },
                    onSendReset = { email ->
                        viewModel.sendReset(email)
                    },
                    busy = busy,
                    darkTheme = darkTheme
                )
            }
        }

        // Snackbar for errors
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun SignInView(
    state: AuthState,
    onStateChange: (AuthState) -> Unit,
    onSignIn: (String, String) -> Unit,
    busy: Boolean,
    darkTheme: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Welcome header section
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Welcome back",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = if (darkTheme) DarkColors.PrimaryText else LightColors.PrimaryText
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "Sign in to continue to Innovexia",
                style = MaterialTheme.typography.bodyMedium,
                color = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText
            )
        }

        Spacer(Modifier.height(24.dp))

        // Input fields with Material 3
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            M3AuthTextField(
                value = state.email,
                onValueChange = { onStateChange(state.copy(email = it)) },
                label = "Email",
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        tint = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText
                    )
                },
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Email,
                darkTheme = darkTheme
            )

            M3AuthTextField(
                value = state.password,
                onValueChange = { onStateChange(state.copy(password = it)) },
                label = "Password",
                isPassword = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText
                    )
                },
                darkTheme = darkTheme
            )
        }

        // Remember me checkbox - Material 3 styled
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 4.dp)
        ) {
            Checkbox(
                checked = state.rememberMe,
                onCheckedChange = { onStateChange(state.copy(rememberMe = it)) },
                colors = CheckboxDefaults.colors(
                    checkedColor = if (darkTheme) Color(0xFF60A5FA) else Color(0xFF3B82F6),
                    uncheckedColor = if (darkTheme) DarkColors.SecondaryText.copy(alpha = 0.6f) else LightColors.SecondaryText.copy(alpha = 0.6f),
                    checkmarkColor = Color.White
                )
            )
            Text(
                text = "Remember me",
                style = MaterialTheme.typography.bodyMedium,
                color = if (darkTheme) DarkColors.PrimaryText else LightColors.PrimaryText,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Spacer(Modifier.height(20.dp))

        // Primary action button - Material 3 filled button
        Button(
            onClick = {
                if (state.email.isNotBlank() && state.password.isNotBlank()) {
                    onSignIn(state.email, state.password)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            enabled = !busy && state.email.isNotBlank() && state.password.isNotBlank(),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (darkTheme) Color(0xFF60A5FA) else Color(0xFF3B82F6),
                contentColor = Color.White,
                disabledContainerColor = if (darkTheme) Color(0xFF374151) else Color(0xFFD1D5DB),
                disabledContentColor = if (darkTheme) Color(0xFF6B7280) else Color(0xFF9CA3AF)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            if (busy) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            Text(
                text = if (busy) "Signing in…" else "Sign in",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold
                )
            )
        }

        Spacer(Modifier.height(12.dp))

        // Secondary actions - Material 3 outlined button
        OutlinedButton(
            onClick = { onStateChange(state.copy(screen = AuthScreen.SignUp)) },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = if (darkTheme) DarkColors.PrimaryText else LightColors.PrimaryText
            ),
            border = BorderStroke(
                1.dp,
                if (darkTheme) Color(0xFF374151) else Color(0xFFD1D5DB)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Sign up",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium
                )
            )
        }

        Spacer(Modifier.height(12.dp))

        // Forgot password link - Material 3 text button
        TextButton(
            onClick = { onStateChange(state.copy(screen = AuthScreen.ForgotPassword)) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Forgot password?",
                style = MaterialTheme.typography.bodyMedium,
                color = if (darkTheme) Color(0xFF60A5FA) else Color(0xFF3B82F6)
            )
        }
    }
}

@Composable
private fun SignUpView(
    state: AuthState,
    onStateChange: (AuthState) -> Unit,
    onSignUp: (String, String, String) -> Unit,
    busy: Boolean,
    darkTheme: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header section
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Create account",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = if (darkTheme) DarkColors.PrimaryText else LightColors.PrimaryText
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "Join Innovexia and start creating",
                style = MaterialTheme.typography.bodyMedium,
                color = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText
            )
        }

        Spacer(Modifier.height(24.dp))

        // Input fields with Material 3
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            M3AuthTextField(
                value = state.username,
                onValueChange = { onStateChange(state.copy(username = it)) },
                label = "Username",
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText
                    )
                },
                darkTheme = darkTheme
            )

            M3AuthTextField(
                value = state.email,
                onValueChange = { onStateChange(state.copy(email = it)) },
                label = "Email",
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        tint = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText
                    )
                },
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Email,
                darkTheme = darkTheme
            )

            M3AuthTextField(
                value = state.password,
                onValueChange = { onStateChange(state.copy(password = it)) },
                label = "Password",
                isPassword = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText
                    )
                },
                supportingText = if (state.password.isNotEmpty() && state.password.length < 6) {
                    "Password must be at least 6 characters"
                } else null,
                isError = state.password.isNotEmpty() && state.password.length < 6,
                darkTheme = darkTheme
            )
        }

        // Terms checkbox - Material 3 styled
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 4.dp)
        ) {
            Checkbox(
                checked = state.agreedToTerms,
                onCheckedChange = { onStateChange(state.copy(agreedToTerms = it)) },
                colors = CheckboxDefaults.colors(
                    checkedColor = if (darkTheme) Color(0xFF60A5FA) else Color(0xFF3B82F6),
                    uncheckedColor = if (darkTheme) DarkColors.SecondaryText.copy(alpha = 0.6f) else LightColors.SecondaryText.copy(alpha = 0.6f),
                    checkmarkColor = Color.White
                )
            )
            Text(
                text = "I agree to Terms and Conditions",
                style = MaterialTheme.typography.bodyMedium,
                color = if (darkTheme) DarkColors.PrimaryText else LightColors.PrimaryText,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Spacer(Modifier.height(20.dp))

        // Primary action button - Material 3 filled button
        Button(
            onClick = {
                if (state.username.isNotBlank() &&
                    state.email.isNotBlank() &&
                    state.password.isNotBlank() &&
                    state.agreedToTerms
                ) {
                    onSignUp(state.username, state.email, state.password)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            enabled = !busy &&
                    state.username.isNotBlank() &&
                    state.email.isNotBlank() &&
                    state.password.length >= 6 &&
                    state.agreedToTerms,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (darkTheme) Color(0xFF60A5FA) else Color(0xFF3B82F6),
                contentColor = Color.White,
                disabledContainerColor = if (darkTheme) Color(0xFF374151) else Color(0xFFD1D5DB),
                disabledContentColor = if (darkTheme) Color(0xFF6B7280) else Color(0xFF9CA3AF)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            if (busy) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            Text(
                text = if (busy) "Creating account…" else "Create account",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold
                )
            )
        }

        Spacer(Modifier.height(16.dp))

        // Back to sign in - Material 3 text button
        TextButton(
            onClick = { onStateChange(state.copy(screen = AuthScreen.SignIn)) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Already have an account? Sign in",
                style = MaterialTheme.typography.bodyMedium,
                color = if (darkTheme) Color(0xFF60A5FA) else Color(0xFF3B82F6)
            )
        }
    }
}

@Composable
private fun ForgotPasswordView(
    state: AuthState,
    onStateChange: (AuthState) -> Unit,
    onSendReset: (String) -> Unit,
    busy: Boolean,
    darkTheme: Boolean
) {
    var resetSent by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!resetSent) {
            // Header section
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Reset password",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (darkTheme) DarkColors.PrimaryText else LightColors.PrimaryText
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "Enter your email address and we'll send you a reset link.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText
                )
            }

            Spacer(Modifier.height(24.dp))

            // Email input with Material 3
            M3AuthTextField(
                value = state.email,
                onValueChange = { onStateChange(state.copy(email = it)) },
                label = "Email",
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        tint = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText
                    )
                },
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Email,
                darkTheme = darkTheme
            )

            Spacer(Modifier.height(24.dp))

            // Send button - Material 3 filled button
            Button(
                onClick = {
                    if (state.email.isNotBlank()) {
                        onSendReset(state.email)
                        resetSent = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !busy && state.email.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (darkTheme) Color(0xFF60A5FA) else Color(0xFF3B82F6),
                    contentColor = Color.White,
                    disabledContainerColor = if (darkTheme) Color(0xFF374151) else Color(0xFFD1D5DB),
                    disabledContentColor = if (darkTheme) Color(0xFF6B7280) else Color(0xFF9CA3AF)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (busy) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                Text(
                    text = if (busy) "Sending…" else "Send reset link",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            Spacer(Modifier.height(16.dp))
        } else {
            // Success state - Material 3 styled
            Spacer(Modifier.height(32.dp))

            // Success icon with Material 3 surface
            Box(
                modifier = Modifier
                    .padding(16.dp)
                    .background(
                        color = if (darkTheme) Color(0xFF064E3B) else Color(0xFFD1FAE5),
                        shape = RoundedCornerShape(100.dp)
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (darkTheme) Color(0xFF34D399) else Color(0xFF10B981),
                    modifier = androidx.compose.ui.Modifier.padding(8.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            // Success message
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Reset link sent!",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (darkTheme) DarkColors.PrimaryText else LightColors.PrimaryText
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Check your email for the password reset link.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Spacer(Modifier.height(32.dp))
        }

        // Back to sign in - Material 3 text button
        TextButton(
            onClick = {
                resetSent = false
                onStateChange(state.copy(screen = AuthScreen.SignIn))
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Back to sign in",
                style = MaterialTheme.typography.bodyMedium,
                color = if (darkTheme) Color(0xFF60A5FA) else Color(0xFF3B82F6)
            )
        }
    }
}

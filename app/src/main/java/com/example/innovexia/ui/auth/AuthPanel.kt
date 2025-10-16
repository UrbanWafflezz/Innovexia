package com.example.innovexia.ui.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.innovexia.ui.glass.GlassButton
import com.example.innovexia.ui.glass.GlassButtonStyle
import com.example.innovexia.ui.glass.GlassField
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

        Spacer(Modifier.height(20.dp))

        // Input fields
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GlassField(
                value = state.email,
                onValueChange = { onStateChange(state.copy(email = it)) },
                hint = "Email or Username",
                leading = {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        tint = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText
                    )
                },
                darkTheme = darkTheme
            )

            GlassField(
                value = state.password,
                onValueChange = { onStateChange(state.copy(password = it)) },
                hint = "Password",
                isPassword = true,
                leading = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText
                    )
                },
                darkTheme = darkTheme
            )
        }

        // Remember me checkbox
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, bottom = 2.dp)
        ) {
            Checkbox(
                checked = state.rememberMe,
                onCheckedChange = { onStateChange(state.copy(rememberMe = it)) },
                colors = CheckboxDefaults.colors(
                    checkedColor = if (darkTheme) Color(0xFF60A5FA) else Color(0xFF3B82F6),
                    uncheckedColor = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText
                )
            )
            Text(
                text = "Remember me",
                style = MaterialTheme.typography.bodyMedium,
                color = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        Spacer(Modifier.height(16.dp))

        // Primary action button
        GlassButton(
            text = if (busy) "Signing in…" else "Sign in",
            onClick = {
                if (state.email.isNotBlank() && state.password.isNotBlank()) {
                    onSignIn(state.email, state.password)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = !busy && state.email.isNotBlank() && state.password.isNotBlank(),
            darkTheme = darkTheme
        )

        Spacer(Modifier.height(10.dp))

        // Secondary actions
        GlassButton(
            text = "Sign up",
            onClick = { onStateChange(state.copy(screen = AuthScreen.SignUp)) },
            style = GlassButtonStyle.Secondary,
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            darkTheme = darkTheme
        )

        Spacer(Modifier.height(12.dp))

        // Forgot password link
        GlassButton(
            text = "Forgot password?",
            onClick = { onStateChange(state.copy(screen = AuthScreen.ForgotPassword)) },
            style = GlassButtonStyle.Ghost,
            modifier = Modifier.fillMaxWidth(),
            darkTheme = darkTheme
        )
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

        Spacer(Modifier.height(20.dp))

        // Input fields
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GlassField(
                value = state.username,
                onValueChange = { onStateChange(state.copy(username = it)) },
                hint = "Username",
                leading = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText
                    )
                },
                darkTheme = darkTheme
            )

            GlassField(
                value = state.email,
                onValueChange = { onStateChange(state.copy(email = it)) },
                hint = "Email",
                leading = {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        tint = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText
                    )
                },
                darkTheme = darkTheme
            )

            GlassField(
                value = state.password,
                onValueChange = { onStateChange(state.copy(password = it)) },
                hint = "Password",
                isPassword = true,
                leading = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText
                    )
                },
                darkTheme = darkTheme
            )
        }

        // Terms checkbox
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, bottom = 2.dp)
        ) {
            Checkbox(
                checked = state.agreedToTerms,
                onCheckedChange = { onStateChange(state.copy(agreedToTerms = it)) },
                colors = CheckboxDefaults.colors(
                    checkedColor = if (darkTheme) Color(0xFF60A5FA) else Color(0xFF3B82F6),
                    uncheckedColor = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText
                )
            )
            Text(
                text = "I agree to Terms and Conditions",
                style = MaterialTheme.typography.bodyMedium,
                color = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        Spacer(Modifier.height(16.dp))

        // Primary action button
        GlassButton(
            text = if (busy) "Creating account…" else "Create account",
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
                .height(50.dp),
            enabled = !busy &&
                    state.username.isNotBlank() &&
                    state.email.isNotBlank() &&
                    state.password.isNotBlank() &&
                    state.agreedToTerms,
            darkTheme = darkTheme
        )

        Spacer(Modifier.height(12.dp))

        // Back to sign in
        GlassButton(
            text = "Back to sign in",
            onClick = { onStateChange(state.copy(screen = AuthScreen.SignIn)) },
            style = GlassButtonStyle.Ghost,
            modifier = Modifier.fillMaxWidth(),
            darkTheme = darkTheme
        )
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

            Spacer(Modifier.height(20.dp))

            // Email input
            GlassField(
                value = state.email,
                onValueChange = { onStateChange(state.copy(email = it)) },
                hint = "Email",
                leading = {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        tint = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText
                    )
                },
                darkTheme = darkTheme
            )

            Spacer(Modifier.height(20.dp))

            // Send button
            GlassButton(
                text = if (busy) "Sending…" else "Send reset link",
                onClick = {
                    if (state.email.isNotBlank()) {
                        onSendReset(state.email)
                        resetSent = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !busy && state.email.isNotBlank(),
                darkTheme = darkTheme
            )

            Spacer(Modifier.height(12.dp))
        } else {
            // Success state
            Spacer(Modifier.height(16.dp))

            // Success icon
            Box(
                modifier = Modifier.padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.padding(8.dp)
                )
            }

            Spacer(Modifier.height(8.dp))

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

                Spacer(Modifier.height(6.dp))

                Text(
                    text = "Check your email for the password reset link.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText
                )
            }

            Spacer(Modifier.height(20.dp))
        }

        // Back to sign in
        GlassButton(
            text = "Back to sign in",
            onClick = {
                resetSent = false
                onStateChange(state.copy(screen = AuthScreen.SignIn))
            },
            style = GlassButtonStyle.Ghost,
            modifier = Modifier.fillMaxWidth(),
            darkTheme = darkTheme
        )
    }
}

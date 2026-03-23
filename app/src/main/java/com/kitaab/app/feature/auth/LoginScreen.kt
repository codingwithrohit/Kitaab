package com.kitaab.app.feature.auth

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kitaab.app.ui.theme.Teal50
import com.kitaab.app.ui.theme.Teal500
import com.kitaab.app.ui.theme.Teal700
import com.kitaab.app.ui.theme.WarmBorder
import com.kitaab.app.ui.theme.WarmMuted

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onLoginNeedsProfile: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.loginState.collectAsState()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AuthEvent.LoginSuccess -> onLoginSuccess()
                is AuthEvent.LoginNeedsProfile -> onLoginNeedsProfile()
                else -> Unit
            }
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearLoginError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            AppLogoMark()
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Welcome back",
                fontSize = 26.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Sign in to continue",
                fontSize = 14.sp,
                color = WarmMuted,
            )
            Spacer(modifier = Modifier.height(36.dp))

            // Google Sign-In button — now fully wired
            GoogleSignInButton(
                onClick = { viewModel.signInWithGoogle(context) },
                isLoading = state.isGoogleLoading,
                enabled = !state.isLoading && !state.isGoogleLoading
            )
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = WarmBorder)
                Text(text = "  or  ", fontSize = 12.sp, color = WarmMuted)
                HorizontalDivider(modifier = Modifier.weight(1f), color = WarmBorder)
            }
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = state.email,
                onValueChange = { viewModel.onLoginEmailChanged(it) },
                label = { Text("Email address") },
                isError = state.emailError != null,
                supportingText = state.emailError?.let { { Text(it) } },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Email,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                ),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = kitaabTextFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = state.password,
                onValueChange = { viewModel.onLoginPasswordChanged(it) },
                label = { Text("Password") },
                isError = state.passwordError != null,
                supportingText = state.passwordError?.let { { Text(it) } },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            modifier = Modifier.size(18.dp),
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() },
                ),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = kitaabTextFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { viewModel.signInWithEmail() },
                enabled = !state.isLoading && !state.isGoogleLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Teal500),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp),
                    )
                } else {
                    Text(
                        text = "Sign in",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = "Don't have an account? ", fontSize = 13.sp, color = WarmMuted)
                Text(
                    text = "Create one",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Teal500,
                    modifier = Modifier.clickable { onNavigateToSignUp() },
                )
            }
        }
    }
}

@Composable
fun AppLogoMark() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(36.dp)) {
            val w = size.width
            val h = size.height
            drawRoundRect(
                color = Teal500,
                topLeft = androidx.compose.ui.geometry.Offset(w * 0.05f, h * 0.1f),
                size = androidx.compose.ui.geometry.Size(w * 0.5f, h * 0.82f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f),
            )
            drawRoundRect(
                color = Teal50,
                topLeft = androidx.compose.ui.geometry.Offset(w * 0.3f, h * 0.2f),
                size = androidx.compose.ui.geometry.Size(w * 0.5f, h * 0.68f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f),
            )
            drawRoundRect(
                color = Color(0xFF9FE1CB),
                topLeft = androidx.compose.ui.geometry.Offset(w * 0.52f, h * 0.3f),
                size = androidx.compose.ui.geometry.Size(w * 0.4f, h * 0.55f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f),
            )
        }
        Spacer(modifier = Modifier.size(10.dp))
        Text(
            text = "Kitaab",
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            color = Teal700,
        )
    }
}

@Composable
private fun GoogleSignInButton(
    onClick: () -> Unit,
    isLoading: Boolean,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .border(1.dp, WarmBorder, RoundedCornerShape(12.dp)),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = Teal500,
                strokeWidth = 2.dp,
                modifier = Modifier.size(20.dp),
            )
        } else {
            // G logo drawn with Canvas — no image asset needed
            androidx.compose.foundation.Canvas(modifier = Modifier.size(18.dp)) {
                val w = size.width
                drawArc(
                    color = Color(0xFF4285F4),
                    startAngle = -30f,
                    sweepAngle = 120f,
                    useCenter = false,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.18f),
                )
                drawArc(
                    color = Color(0xFF34A853),
                    startAngle = 90f,
                    sweepAngle = 110f,
                    useCenter = false,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.18f),
                )
                drawArc(
                    color = Color(0xFFFBBC05),
                    startAngle = 200f,
                    sweepAngle = 80f,
                    useCenter = false,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.18f),
                )
                drawArc(
                    color = Color(0xFFEA4335),
                    startAngle = 280f,
                    sweepAngle = 50f,
                    useCenter = false,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.18f),
                )
            }
            Spacer(modifier = Modifier.size(10.dp))
            Text(
                text = "Continue with Google",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
fun kitaabTextFieldColors() =
    OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Teal500,
        unfocusedBorderColor = WarmBorder,
        focusedLabelColor = Teal500,
        unfocusedLabelColor = WarmMuted,
        cursorColor = Teal500,
    )
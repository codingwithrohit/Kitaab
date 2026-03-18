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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kitaab.app.ui.theme.Teal50
import com.kitaab.app.ui.theme.Teal500
import com.kitaab.app.ui.theme.Teal700
import com.kitaab.app.ui.theme.WarmBorder
import com.kitaab.app.ui.theme.WarmMuted

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToSignUp: () -> Unit,
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        // Header
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

        // Google sign in
        GoogleSignInButton(
            onClick = {
                // Google OAuth — wired in next step
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Divider
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = WarmBorder)
            Text(
                text = "  or  ",
                fontSize = 12.sp,
                color = WarmMuted,
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = WarmBorder)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Email field
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email address") },
            leadingIcon = {
                Icon(Icons.Outlined.Email, contentDescription = null, modifier = Modifier.size(18.dp))
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = kitaabTextFieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Password field
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            leadingIcon = {
                Icon(Icons.Outlined.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
            },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
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
                onDone = { focusManager.clearFocus() }
            ),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = kitaabTextFieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Sign in button
        Button(
            onClick = {
                isLoading = true
                // email/password auth wired in next step
            },
            enabled = email.isNotBlank() && password.isNotBlank() && !isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = Teal500),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            if (isLoading) {
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

        // Sign up link
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "Don't have an account? ",
                fontSize = 13.sp,
                color = WarmMuted,
            )
            Text(
                text = "Create one",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Teal500,
                modifier = Modifier.clickable {
                    onNavigateToSignUp()
                }
            )
        }
    }
}

@Composable
private fun AppLogoMark() {
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
private fun GoogleSignInButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .border(1.dp, WarmBorder, RoundedCornerShape(12.dp)),
    ) {
        // G logo using Canvas — avoids needing an image asset
        androidx.compose.foundation.Canvas(modifier = Modifier.size(18.dp)) {
            val w = size.width
            val h = size.height
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

@Composable
private fun kitaabTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Teal500,
    unfocusedBorderColor = WarmBorder,
    focusedLabelColor = Teal500,
    unfocusedLabelColor = WarmMuted,
    cursorColor = Teal500,
)
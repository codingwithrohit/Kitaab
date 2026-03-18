package com.kitaab.app.feature.auth

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kitaab.app.ui.theme.Teal50
import com.kitaab.app.ui.theme.Teal500
import com.kitaab.app.ui.theme.Teal700
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    val scale = remember { Animatable(0.8f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        scale.animateTo(1f, animationSpec = tween(600))
        alpha.animateTo(1f, animationSpec = tween(400))
        delay(800)
        onSplashFinished()
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Teal700),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            BookStackIcon(
                modifier =
                    Modifier
                        .scale(scale.value)
                        .alpha(alpha.value),
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Kitaab",
                style =
                    TextStyle(
                        fontSize = 32.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Teal50,
                        letterSpacing = (-0.5).sp,
                    ),
                modifier = Modifier.alpha(alpha.value),
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "buy · sell · donate books",
                style =
                    TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = Teal500,
                    ),
                modifier = Modifier.alpha(alpha.value),
            )
        }
    }
}

@Composable
private fun BookStackIcon(modifier: Modifier = Modifier) {
    // Three stacked book rects — simple, recognisable, no library needed
    androidx.compose.foundation.Canvas(
        modifier = modifier.size(80.dp),
    ) {
        val w = size.width
        val h = size.height

        // Book 1 — back, tallest
        drawRoundRect(
            color = Teal500,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.1f, h * 0.05f),
            size = androidx.compose.ui.geometry.Size(w * 0.55f, h * 0.85f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f),
        )
        // Book 2 — middle
        drawRoundRect(
            color = Teal50.copy(alpha = 0.85f),
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.3f, h * 0.15f),
            size = androidx.compose.ui.geometry.Size(w * 0.55f, h * 0.75f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f),
        )
        // Book 3 — front, shortest
        drawRoundRect(
            color = Color(0xFF9FE1CB),
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.25f),
            size = androidx.compose.ui.geometry.Size(w * 0.42f, h * 0.62f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f),
        )
        // Shelf line
        drawRoundRect(
            color = Color(0xFF085041),
            topLeft = androidx.compose.ui.geometry.Offset(0f, h * 0.88f),
            size = androidx.compose.ui.geometry.Size(w, h * 0.08f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f),
        )
    }
}

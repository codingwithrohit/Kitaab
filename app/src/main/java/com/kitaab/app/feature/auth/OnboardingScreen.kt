package com.kitaab.app.feature.auth

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kitaab.app.ui.theme.Teal50
import com.kitaab.app.ui.theme.Teal500
import com.kitaab.app.ui.theme.Teal700
import com.kitaab.app.ui.theme.WarmMuted
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val illustration: @Composable () -> Unit,
    val title: String,
    val subtitle: String,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val pages = rememberOnboardingPages()
    val pagerState = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Skip button
        TextButton(
            onClick = onFinished,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 48.dp, end = 16.dp),
        ) {
            Text(
                text = "Skip",
                color = WarmMuted,
                fontSize = 14.sp,
            )
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) { index ->
                OnboardingPage(page = pages[index])
            }

            // Dot indicators
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(bottom = 32.dp),
            ) {
                pages.indices.forEach { index ->
                    PageDot(isSelected = pagerState.currentPage == index)
                    if (index < pages.lastIndex) Spacer(modifier = Modifier.width(8.dp))
                }
            }

            // Next / Get started button
            Button(
                onClick = {
                    if (pagerState.currentPage < pages.lastIndex) {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    } else {
                        onFinished()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Teal500),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .navigationBarsPadding()
                    .height(52.dp),
            ) {
                Text(
                    text = if (pagerState.currentPage == pages.lastIndex) "Get started" else "Next",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun OnboardingPage(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(220.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Teal50),
            contentAlignment = Alignment.Center,
        ) {
            page.illustration()
        }

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = page.title,
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            lineHeight = 30.sp,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = page.subtitle,
            fontSize = 14.sp,
            color = WarmMuted,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
        )
    }
}

@Composable
private fun PageDot(isSelected: Boolean) {
    val width by animateDpAsState(
        targetValue = if (isSelected) 20.dp else 7.dp,
        animationSpec = tween(300),
        label = "dot_width",
    )
    Box(
        modifier = Modifier
            .height(7.dp)
            .width(width)
            .clip(CircleShape)
            .background(if (isSelected) Teal500 else WarmMuted.copy(alpha = 0.4f)),
    )
}

@Composable
private fun rememberOnboardingPages() = listOf(
    OnboardingPage(
        illustration = { FindBooksIllustration() },
        title = "Find books you need",
        subtitle = "Browse thousands of second-hand books from students near you",
    ),
    OnboardingPage(
        illustration = { SellDonateIllustration() },
        title = "Sell or donate yours",
        subtitle = "Your old books can fund your next purchase or help a student in need",
    ),
    OnboardingPage(
        illustration = { CommunityIllustration() },
        title = "Connect with readers",
        subtitle = "Build a community where knowledge passes from one student to another",
    ),
)

// ── Illustrations ────────────────────────────────────────────────────────────
// Simple Canvas illustrations — no external library, no copyright issues

@Composable
private fun FindBooksIllustration() {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(140.dp)) {
        val w = size.width
        val h = size.height

        // Shelf
        drawRoundRect(
            color = Color(0xFFC4955A),
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.05f, h * 0.7f),
            size = androidx.compose.ui.geometry.Size(w * 0.9f, h * 0.08f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f),
        )
        // Books
        val bookColors = listOf(
            Color(0xFF1D9E75),
            Color(0xFF534AB7),
            Color(0xFFD85A30),
            Color(0xFFBA7517),
        )
        val bookWidths = listOf(0.14f, 0.12f, 0.16f, 0.13f)
        val bookHeights = listOf(0.48f, 0.38f, 0.52f, 0.42f)
        var xOffset = 0.1f
        bookColors.forEachIndexed { i, color ->
            drawRoundRect(
                color = color,
                topLeft = androidx.compose.ui.geometry.Offset(
                    w * xOffset,
                    h * (0.7f - bookHeights[i]),
                ),
                size = androidx.compose.ui.geometry.Size(w * bookWidths[i], h * bookHeights[i]),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f),
            )
            xOffset += bookWidths[i] + 0.06f
        }
        // Magnifying glass
        drawCircle(
            color = Color(0xFF085041),
            radius = w * 0.12f,
            center = androidx.compose.ui.geometry.Offset(w * 0.78f, h * 0.3f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6f),
        )
        drawLine(
            color = Color(0xFF085041),
            start = androidx.compose.ui.geometry.Offset(w * 0.87f, h * 0.4f),
            end = androidx.compose.ui.geometry.Offset(w * 0.95f, h * 0.5f),
            strokeWidth = 6f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
    }
}

@Composable
private fun SellDonateIllustration() {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(140.dp)) {
        val w = size.width
        val h = size.height

        // Left hand (giving)
        drawRoundRect(
            color = Color(0xFF1D9E75),
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.05f, h * 0.45f),
            size = androidx.compose.ui.geometry.Size(w * 0.35f, h * 0.12f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(20f),
        )
        // Book on hand
        drawRoundRect(
            color = Color(0xFF534AB7),
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.08f, h * 0.28f),
            size = androidx.compose.ui.geometry.Size(w * 0.28f, h * 0.38f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f),
        )
        // Arrow
        drawLine(
            color = Color(0xFFBA7517),
            start = androidx.compose.ui.geometry.Offset(w * 0.42f, h * 0.5f),
            end = androidx.compose.ui.geometry.Offset(w * 0.58f, h * 0.5f),
            strokeWidth = 5f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
        drawLine(
            color = Color(0xFFBA7517),
            start = androidx.compose.ui.geometry.Offset(w * 0.53f, h * 0.43f),
            end = androidx.compose.ui.geometry.Offset(w * 0.58f, h * 0.5f),
            strokeWidth = 5f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
        drawLine(
            color = Color(0xFFBA7517),
            start = androidx.compose.ui.geometry.Offset(w * 0.53f, h * 0.57f),
            end = androidx.compose.ui.geometry.Offset(w * 0.58f, h * 0.5f),
            strokeWidth = 5f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
        // Right hand (receiving)
        drawRoundRect(
            color = Color(0xFF1D9E75),
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.6f, h * 0.45f),
            size = androidx.compose.ui.geometry.Size(w * 0.35f, h * 0.12f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(20f),
        )
        // Book on right hand
        drawRoundRect(
            color = Color(0xFFD85A30),
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.63f, h * 0.28f),
            size = androidx.compose.ui.geometry.Size(w * 0.28f, h * 0.38f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f),
        )
    }
}

@Composable
private fun CommunityIllustration() {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(140.dp)) {
        val w = size.width
        val h = size.height

        val avatarColor = Color(0xFF1D9E75)
        val lineColor = Color(0xFF9FE1CB)

        // Center person
        drawCircle(
            color = avatarColor,
            radius = w * 0.1f,
            center = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.4f),
        )
        // Top left person
        drawCircle(
            color = Color(0xFF534AB7),
            radius = w * 0.08f,
            center = androidx.compose.ui.geometry.Offset(w * 0.2f, h * 0.2f),
        )
        // Top right person
        drawCircle(
            color = Color(0xFFD85A30),
            radius = w * 0.08f,
            center = androidx.compose.ui.geometry.Offset(w * 0.8f, h * 0.2f),
        )
        // Bottom left
        drawCircle(
            color = Color(0xFFBA7517),
            radius = w * 0.08f,
            center = androidx.compose.ui.geometry.Offset(w * 0.2f, h * 0.72f),
        )
        // Bottom right
        drawCircle(
            color = Color(0xFF085041),
            radius = w * 0.08f,
            center = androidx.compose.ui.geometry.Offset(w * 0.8f, h * 0.72f),
        )
        // Connection lines
        listOf(
            Pair(
                androidx.compose.ui.geometry.Offset(w * 0.2f, h * 0.2f),
                androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.4f)
            ),
            Pair(
                androidx.compose.ui.geometry.Offset(w * 0.8f, h * 0.2f),
                androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.4f)
            ),
            Pair(
                androidx.compose.ui.geometry.Offset(w * 0.2f, h * 0.72f),
                androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.4f)
            ),
            Pair(
                androidx.compose.ui.geometry.Offset(w * 0.8f, h * 0.72f),
                androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.4f)
            ),
        ).forEach { (start, end) ->
            drawLine(
                color = lineColor,
                start = start,
                end = end,
                strokeWidth = 3f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
            )
        }
    }
}
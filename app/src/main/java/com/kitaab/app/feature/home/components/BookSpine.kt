package com.kitaab.app.feature.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Palette of distinct book spine colors matching Kitaab's brand
private val spineColors = listOf(
    Color(0xFF1D9E75), // Teal500
    Color(0xFF534AB7), // Purple
    Color(0xFFD85A30), // Coral
    Color(0xFFBA7517), // Amber
    Color(0xFF085041), // Dark teal
    Color(0xFF3B8BD4), // Blue
    Color(0xFF993556), // Pink
    Color(0xFF639922), // Green
)

fun spineColorForIndex(index: Int): Color = spineColors[index % spineColors.size]

@Composable
fun BookSpine(
    title: String,
    color: Color,
    width: Dp,
    height: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier
            .width(width)
            .height(height)
            .clickable { onClick() },
    ) {
        drawBookSpine(
            color = color,
            title = title,
            spineWidth = size.width,
            spineHeight = size.height,
        )
    }
}

private fun DrawScope.drawBookSpine(
    color: Color,
    title: String,
    spineWidth: Float,
    spineHeight: Float,
) {
    // Main spine body
    drawRoundRect(
        color = color,
        topLeft = Offset(0f, 0f),
        size = Size(spineWidth, spineHeight),
        cornerRadius = CornerRadius(4f),
    )

    // Slightly lighter highlight on left edge for 3D effect
    drawRoundRect(
        color = Color.White.copy(alpha = 0.15f),
        topLeft = Offset(0f, 0f),
        size = Size(spineWidth * 0.18f, spineHeight),
        cornerRadius = CornerRadius(4f),
    )

    // Draw vertical title text if spine is wide enough
    if (spineWidth >= 20.dp.toPx() && title.isNotBlank()) {
        val paint = android.graphics.Paint().apply {
            this.color = Color.White.copy(alpha = 0.9f).toArgb()
            textSize = 9.dp.toPx()
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
        }

        drawContext.canvas.nativeCanvas.save()
        drawContext.canvas.nativeCanvas.rotate(
            -90f,
            spineWidth / 2f,
            spineHeight / 2f,
        )

        val maxChars = (spineHeight / paint.textSize * 1.8f).toInt()
        val displayTitle = if (title.length > maxChars) title.take(maxChars - 1) + "…" else title

        drawContext.canvas.nativeCanvas.drawText(
            displayTitle,
            spineWidth / 2f,
            spineHeight / 2f + paint.textSize / 3f,
            paint,
        )
        drawContext.canvas.nativeCanvas.restore()
    }
}
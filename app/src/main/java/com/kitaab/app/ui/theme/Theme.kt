package com.kitaab.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary            = Teal500,
    onPrimary          = WarmIvory,
    primaryContainer   = Teal50,
    onPrimaryContainer = Teal900,
    background         = WarmIvory,
    onBackground       = WarmBlack,
    surface            = WarmSurface,
    onSurface          = WarmBlack,
    surfaceVariant     = WarmSurface,
    onSurfaceVariant   = WarmMuted,
    outline            = WarmBorder,
)

private val DarkColors = darkColorScheme(
    primary            = Teal100,
    onPrimary          = Teal900,
    primaryContainer   = Teal700,
    onPrimaryContainer = Teal50,
    background         = DarkBackground,
    onBackground       = WarmIvory,
    surface            = DarkSurface,
    onSurface          = WarmIvory,
    surfaceVariant     = DarkSurfaceHigh,
    onSurfaceVariant   = Teal100,
    outline            = DarkSurfaceHigh,
)

@Composable
fun KitaabTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography  = KitaabTypography,
        content     = content,
    )
}
package com.example.myintervaltimerproject.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
    darkColorScheme(
        primary = TealPrimary,
        onPrimary = Color.White,

        secondary = PurpleRest,
        onSecondary = Color.White,

        background = DarkBackground,
        onBackground = DarkTextPrimary,

        surface = DarkSurface,
        onSurface = DarkTextPrimary,

        surfaceVariant = DarkSurface,
        onSurfaceVariant = DarkTextSecondary,

        outline = DarkDivider
    )

private val LightColorScheme =
    lightColorScheme(
        primary = TealPrimary,
        onPrimary = Color.White,

        secondary = PurpleRest,
        onSecondary = LightTextPrimary,

        background = LightBackground,
        onBackground = LightTextPrimary,

        surface = LightSurface,
        onSurface = LightTextPrimary,

        surfaceVariant = LightSurfaceSecondary,
        onSurfaceVariant = LightTextSecondary,

        outline = LightDivider
    )

@Composable
fun IntervalTimerTheme(
    darkTheme: Boolean =
        isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme =
            if (darkTheme) {
                DarkColorScheme
            } else {
                LightColorScheme
            },

        typography =
            Typography,

        shapes =
            AppShapes
    ) {
        AppBackground(
            darkTheme = darkTheme,
            content = content
        )
    }
}
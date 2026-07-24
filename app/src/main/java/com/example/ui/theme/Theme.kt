package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = VibrantPurple,
    onPrimary = PureWhite,
    primaryContainer = VibrantPurpleLight,
    onPrimaryContainer = PureWhite,
    secondary = VibrantPurple,
    onSecondary = PureWhite,
    secondaryContainer = IceWhiteCard,
    onSecondaryContainer = VibrantPurple,
    tertiary = VibrantPurpleLight,
    onTertiary = PureWhite,
    background = SoftPurpleBg,
    onBackground = TextDarkPrimary,
    surface = IceWhiteCard,
    onSurface = TextDarkPrimary,
    surfaceVariant = PureWhite,
    onSurfaceVariant = TextDarkSecondary,
    outline = TextDarkMuted
)

@Composable
fun LyraTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = SoftPurpleBg.toArgb()
            window.navigationBarColor = PureWhite.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

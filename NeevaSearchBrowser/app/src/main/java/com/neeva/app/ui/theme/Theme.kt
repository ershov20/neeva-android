package com.neeva.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorPalette = darkColors(
        primary = BackgroundDark,
        primaryVariant = FillDark,
        background = TrayDark,
        onPrimary = Color.White,
        onBackground = Color.White,
        onSecondary = LabelSecondaryDark
)

private val LightColorPalette = lightColors(
        primary = BackgroundLight,
        primaryVariant = FillLight,
        background = TrayLight,
        onPrimary = Color.Black,
        onBackground = Color.Black,
        onSecondary = LabelSecondaryLight
        /* Other default colors to override
    surface = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.Black,
    */
)

@Composable
fun NeevaTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable() () -> Unit) {
    val colors = if (darkTheme) {
        DarkColorPalette
    } else {
        LightColorPalette
    }

    MaterialTheme(
            colors = colors,
            typography = Typography,
            shapes = Shapes,
            content = content
    )
}
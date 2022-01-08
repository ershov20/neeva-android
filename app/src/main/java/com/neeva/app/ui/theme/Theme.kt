package com.neeva.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorPalette = darkColors(
    primary = BackgroundDark,
    primaryVariant = ColorPalette.Ui.Gray20,
    secondary = ColorPalette.Ui.DarkModeBlue,
    background = Color.Black,

    onPrimary = ColorPalette.Ui.Gray99,
    onSecondary = ColorPalette.Ui.Gray97,
    onBackground = ColorPalette.Ui.Gray99
)

private val LightColorPalette = lightColors(
    primary = BackgroundLight,
    primaryVariant = ColorPalette.Ui.Gray94,
    secondary = ColorPalette.Ui.DefaultBlue,
    background = ColorPalette.Ui.DefaultBackground,

    onPrimary = Color.Black,
    onSecondary = ColorPalette.Ui.Gray50,
    onBackground = Color.Black
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

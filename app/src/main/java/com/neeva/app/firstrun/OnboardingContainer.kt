package com.neeva.app.firstrun

import android.view.Surface
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.neeva.app.ui.theme.ColorPalette

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingContainer(
    showBrowser: () -> Unit,
    useDarkThemeForPreviews: Boolean,
    content: @Composable () -> Unit
) {
    val backgroundColor = if (useDarkThemeForPreviews) {
        MaterialTheme.colorScheme.background
    } else {
        ColorPalette.Brand.Offwhite
    }

    Surface(color = backgroundColor) {
        Box(Modifier.fillMaxSize()) {
            content()
            CloseButton(
                onClick = showBrowser,
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }
    }
}

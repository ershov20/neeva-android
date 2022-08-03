package com.neeva.app.ui.widgets

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import com.neeva.app.ui.theme.Dimensions

/**
 * Draws a vertically scrollable [Column] that shows indicators at the top and bottom if there is
 * more the user can scroll to.
 */
@Composable
fun ColumnWithMoreIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.background,
    contentAlignment: Alignment = Alignment.Center,
    scrollState: ScrollState = rememberScrollState(),
    content: @Composable () -> Unit
) {
    val gradientHeightPx = with(LocalDensity.current) {
        Dimensions.SCROLL_GRADIENT_INDICATOR.toPx()
    }

    Box(
        modifier = modifier,
        contentAlignment = contentAlignment
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .verticalScroll(scrollState)
                .fillMaxSize()
        ) {
            content()
        }

        // Draw the overflow indicator at the top if the user can scroll upward.
        val topAlpha = (scrollState.value / gradientHeightPx).coerceAtMost(1.0f)
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(color, Color.Transparent)
                    ),
                    alpha = topAlpha
                )
                .fillMaxWidth()
                .height(Dimensions.SCROLL_GRADIENT_INDICATOR)
                .align(Alignment.TopCenter)
        )

        // Draw the overflow indicator at the bottom if the user can scroll further.
        val distanceFromBottom = scrollState.maxValue - scrollState.value
        val bottomAlpha = when (scrollState.maxValue) {
            Int.MAX_VALUE -> 0.0f
            else -> (distanceFromBottom / gradientHeightPx).coerceAtMost(1.0f)
        }
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, color)
                    ),
                    alpha = bottomAlpha
                )
                .fillMaxWidth()
                .height(Dimensions.SCROLL_GRADIENT_INDICATOR)
                .align(Alignment.BottomCenter)
        )
    }
}

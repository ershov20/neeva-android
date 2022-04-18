package com.neeva.app.firstrun

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

object FirstRunConstants {
    @Composable
    fun getSubtextStyle(color: Color = MaterialTheme.colorScheme.onSurfaceVariant): TextStyle {
        return MaterialTheme.typography.bodyMedium
            .copy(color = color)
    }
}

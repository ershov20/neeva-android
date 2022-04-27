package com.neeva.app.overflowmenu

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.neeva.app.ui.theme.Dimensions

@Composable
internal fun OverflowMenuText(text: String) {
    Text(
        modifier = Modifier.padding(horizontal = Dimensions.PADDING_SMALL),
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

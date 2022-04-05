package com.neeva.app.ui.widgets

import android.net.Uri
import android.webkit.URLUtil
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextOverflow
import com.neeva.app.ui.theme.getClickableAlpha

@Composable
fun StackedText(
    primaryLabel: String,
    secondaryLabel: String? = null,
    maxLines: Int = 1,
    showActualUrl: Boolean = false,
    enabled: Boolean = true
) {
    Column(modifier = Modifier.alpha(getClickableAlpha(enabled))) {
        Text(
            text = primaryLabel,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )

        secondaryLabel?.let {
            if (!showActualUrl && URLUtil.isValidUrl(secondaryLabel)) {
                UriDisplayView(Uri.parse(secondaryLabel))
            } else {
                Text(
                    text = secondaryLabel,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

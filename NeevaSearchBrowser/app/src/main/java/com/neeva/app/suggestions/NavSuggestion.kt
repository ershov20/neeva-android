package com.neeva.app.suggestions

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.neeva.app.ui.theme.NeevaTheme
import com.neeva.app.widgets.FaviconView

@Composable
fun NavSuggestion(
    faviconData: Bitmap?,
    onOpenUrl: (Uri) -> Unit,
    navSuggestion: NavSuggestion
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenUrl(navSuggestion.url) }
            .padding(
                horizontal = 12.dp,
                vertical = 10.dp
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 8.dp)
        ) {
            FaviconView(faviconData)

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1.0f)) {
                Text(
                    text = navSuggestion.label,
                    style = MaterialTheme.typography.body1,
                    color = MaterialTheme.colors.onPrimary,
                    maxLines = 1,
                )
                Text(
                    text = navSuggestion.secondaryLabel,
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSecondary,
                    maxLines = 1,
                )
            }
        }
    }
}

@Preview(name = "1x font size")
@Preview(name = "2x font size", fontScale = 2.0f)
@Composable
fun NavSuggestion_Preview() {
    NeevaTheme {
        NavSuggestion(
            faviconData = null,
            onOpenUrl = {},
            navSuggestion = NavSuggestion(
                url = Uri.parse("https://www.neeva.com"),
                label = "Primary label",
                secondaryLabel = "Secondary label"
            )
        )
    }
}
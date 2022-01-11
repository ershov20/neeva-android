package com.neeva.app.suggestions

import android.net.Uri
import android.webkit.URLUtil
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.neeva.app.R
import com.neeva.app.storage.Favicon
import com.neeva.app.ui.theme.NeevaTheme

@Composable
fun NavSuggestionRow(
    primaryLabel: String,
    onTapRow: () -> Unit,
    onTapRowContentDescription: String? = null,
    secondaryLabel: String? = null,
    onTapEdit: (() -> Unit)? = null,
    faviconData: Favicon? = null,
    imageURL: String? = null,
    drawableID: Int? = null,
    drawableTint: Color? = null
) {
    BaseSuggestionRow(
        onTapRow = onTapRow,
        onTapRowContentDescription = onTapRowContentDescription,
        onTapEdit = onTapEdit,
        faviconData = faviconData,
        imageURL = imageURL,
        drawableID = drawableID,
        drawableTint = drawableTint
    ) {
        Column(modifier = it) {
            Text(
                text = primaryLabel,
                style = MaterialTheme.typography.body1,
                color = MaterialTheme.colors.onPrimary,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth()
            )
            secondaryLabel?.let {
                if (URLUtil.isValidUrl(secondaryLabel)) {
                    UriDisplayView(Uri.parse(secondaryLabel))
                } else {
                    Text(
                        text = secondaryLabel,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSecondary,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Preview("No edit, 1x")
@Preview("No edit, 2x", fontScale = 2.0f)
@Preview("No edit, RTL, 1x", locale = "he")
@Preview("No edit, RTL, 2x", locale = "he", fontScale = 2.0f)
@Composable
fun NavSuggestionRow_Preview() {
    NeevaTheme {
        NavSuggestionRow(
            primaryLabel = "Primary label",
            onTapRow = {},
            secondaryLabel = "Secondary label",
            onTapEdit = null,
            faviconData = null
        )
    }
}

@Preview("With edit, 1x")
@Preview("With edit, 2x", fontScale = 2.0f)
@Preview("With edit, RTL, 1x", locale = "he")
@Preview("With edit, RTL, 2x", locale = "he", fontScale = 2.0f)
@Composable
fun NavSuggestionRow_PreviewEditable() {
    NeevaTheme {
        NavSuggestionRow(
            primaryLabel = "Primary label",
            onTapRow = {},
            secondaryLabel = "Secondary label",
            onTapEdit = {},
            faviconData = null
        )
    }
}

@Preview("Long text with edit, 1x")
@Preview("Long text with edit, 2x", fontScale = 2.0f)
@Preview("Long text with edit, RTL, 1x", locale = "he")
@Preview("Long text with edit, RTL, 2x", locale = "he", fontScale = 2.0f)
@Composable
fun NavSuggestionRow_PreviewLongTextEditable() {
    NeevaTheme {
        NavSuggestionRow(
            primaryLabel = stringResource(R.string.debug_long_string_primary),
            onTapRow = {},
            secondaryLabel = stringResource(R.string.debug_long_string_secondary),
            onTapEdit = {},
            faviconData = null
        )
    }
}

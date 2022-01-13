package com.neeva.app.suggestions

import android.net.Uri
import android.webkit.URLUtil
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.neeva.app.R
import com.neeva.app.storage.Favicon
import com.neeva.app.ui.BooleanPreviewParameterProvider
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

class NavSuggestionRowPreviews :
    BooleanPreviewParameterProvider<NavSuggestionRowPreviews.Params>(3) {
    data class Params(
        val darkTheme: Boolean,
        val useLongLabels: Boolean,
        val allowEditing: Boolean
    )

    override fun createParams(booleanArray: BooleanArray) = Params(
        darkTheme = booleanArray[0],
        useLongLabels = booleanArray[1],
        allowEditing = booleanArray[2]
    )

    @Preview("1x", locale = "en")
    @Preview("2x", locale = "en", fontScale = 2.0f)
    @Preview("RTL, 1x", locale = "he")
    @Preview("RTL, 2x", locale = "he", fontScale = 2.0f)
    @Composable
    fun DefaultPreview(
        @PreviewParameter(NavSuggestionRowPreviews::class) params: Params
    ) {
        val primaryLabel: String
        val secondaryLabel: String
        if (params.useLongLabels) {
            primaryLabel = stringResource(R.string.debug_long_string_primary)
            secondaryLabel = stringResource(R.string.debug_long_string_primary)
        } else {
            primaryLabel = "Primary label"
            secondaryLabel = "Secondary label"
        }
        val onTapEdit = {}.takeIf { params.allowEditing }

        NeevaTheme(darkTheme = params.darkTheme) {
            Box(modifier = Modifier.background(MaterialTheme.colors.primary)) {
                NavSuggestionRow(
                    primaryLabel = primaryLabel,
                    onTapRow = {},
                    secondaryLabel = secondaryLabel,
                    onTapEdit = onTapEdit,
                    faviconData = null
                )
            }
        }
    }
}

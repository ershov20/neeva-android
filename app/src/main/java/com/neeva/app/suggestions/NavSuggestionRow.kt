package com.neeva.app.suggestions

import android.net.Uri
import android.webkit.URLUtil
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.neeva.app.R
import com.neeva.app.ui.OneBooleanPreviewContainer

@Composable
fun NavSuggestionRow(
    iconParams: SuggestionRowIconParams,
    primaryLabel: String,
    onTapRow: () -> Unit,
    onTapRowContentDescription: String? = null,
    secondaryLabel: String? = null,
    actionParams: SuggestionRowActionParams? = null
) {
    BaseSuggestionRow(
        iconParams = iconParams,
        onTapRow = onTapRow,
        onTapRowContentDescription = onTapRowContentDescription,
        actionParams = actionParams
    ) {
        Column {
            Text(
                text = primaryLabel,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            secondaryLabel?.let {
                if (URLUtil.isValidUrl(secondaryLabel)) {
                    UriDisplayView(Uri.parse(secondaryLabel))
                } else {
                    Text(
                        text = secondaryLabel,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Preview("Long labels, LTR, 1x", locale = "en")
@Preview("Long labels, LTR, 2x", locale = "en", fontScale = 2.0f)
@Preview("Long labels, RTL, 1x", locale = "he")
@Preview("Long labels, RTL, 2x", locale = "he", fontScale = 2.0f)
@Composable
private fun NavSuggestionRowPreview_LongLabels() {
    OneBooleanPreviewContainer { allowEditing ->
        val primaryLabel = stringResource(R.string.debug_long_string_primary)
        val secondaryLabel = stringResource(R.string.debug_long_string_primary)
        val onTapEdit = {}.takeIf { allowEditing }
        NavSuggestionRow(
            iconParams = SuggestionRowIconParams(),
            primaryLabel = primaryLabel,
            onTapRow = {},
            secondaryLabel = secondaryLabel,
            actionParams = onTapEdit?.let {
                SuggestionRowActionParams(
                    onTapAction = it,
                    actionType = SuggestionRowActionParams.ActionType.REFINE
                )
            }
        )
    }
}

@Preview("Short labels, LTR, 1x", locale = "en")
@Preview("Short labels, LTR, 2x", locale = "en", fontScale = 2.0f)
@Preview("Short labels, RTL, 1x", locale = "he")
@Preview("Short labels, RTL, 2x", locale = "he", fontScale = 2.0f)
@Composable
private fun NavSuggestionRowPreview_ShortLabels() {
    OneBooleanPreviewContainer { allowEditing ->
        val primaryLabel = "Primary label"
        val secondaryLabel = "Secondary label"
        val onTapEdit = {}.takeIf { allowEditing }
        NavSuggestionRow(
            iconParams = SuggestionRowIconParams(),
            primaryLabel = primaryLabel,
            onTapRow = {},
            secondaryLabel = secondaryLabel,
            actionParams = onTapEdit?.let {
                SuggestionRowActionParams(
                    onTapAction = it,
                    actionType = SuggestionRowActionParams.ActionType.REFINE
                )
            }
        )
    }
}

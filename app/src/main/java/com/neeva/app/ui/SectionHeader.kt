package com.neeva.app.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.neeva.app.R
import com.neeva.app.ui.theme.Dimensions

@Composable
fun SectionHeader(stringId: Int) {
    Surface(
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(stringId),
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            modifier = Modifier.padding(
                horizontal = Dimensions.PADDING_LARGE,
                vertical = Dimensions.PADDING_SMALL
            ),
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Preview("SectionHeader, LTR, 1x", locale = "en")
@Preview("SectionHeader, LTR, 2x", locale = "en", fontScale = 2.0f)
@Preview("SectionHeader, RTL, 1x", locale = "he")
@Composable
private fun SectionHeaderPreview() {
    OneBooleanPreviewContainer { useLongText ->
        val titleString = if (useLongText) {
            R.string.debug_long_string_primary
        } else {
            R.string.debug_short_action
        }

        SectionHeader(titleString)
    }
}

package com.neeva.app.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.neeva.app.R
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.ui.theme.NeevaTheme
import java.util.Locale

@Composable
fun SectionHeader(stringId: Int) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(stringId).uppercase(Locale.getDefault()),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            color = LocalContentColor.current,
            modifier = Modifier.padding(Dimensions.PADDING_SMALL),
            overflow = TextOverflow.Ellipsis
        )
    }
}

class SectionHeaderPreviews : BooleanPreviewParameterProvider<SectionHeaderPreviews.Params>(2) {
    data class Params(
        val useDarkTheme: Boolean,
        val useLongText: Boolean
    )

    override fun createParams(booleanArray: BooleanArray) = Params(
        useDarkTheme = booleanArray[0],
        useLongText = booleanArray[1]
    )

    @Preview("1x", locale = "en")
    @Preview("2x", locale = "en", fontScale = 2.0f)
    @Preview("RTL, 1x", locale = "he")
    @Preview("RTL, 2x", locale = "he", fontScale = 2.0f)
    @Composable
    fun DefaultPreview(@PreviewParameter(SectionHeaderPreviews::class) params: Params) {
        val titleString = if (params.useLongText) {
            R.string.debug_long_string_primary
        } else {
            R.string.debug_short_action
        }

        NeevaTheme(useDarkTheme = params.useDarkTheme) {
            SectionHeader(titleString)
        }
    }
}

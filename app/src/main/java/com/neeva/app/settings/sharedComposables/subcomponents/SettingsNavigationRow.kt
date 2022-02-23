package com.neeva.app.settings.sharedComposables.subcomponents

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.neeva.app.R
import com.neeva.app.ui.BooleanPreviewParameterProvider
import com.neeva.app.ui.theme.NeevaTheme

@Composable
fun SettingsNavigationRow(
    title: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable { onClick() }.then(modifier)
    ) {
        SettingsLabelText(text = title, enabled = enabled, modifier = Modifier.weight(1.0f))
        // TODO(dan.alcantara) Use Material Icons extended library when CircleCI issues are resolved
        Image(
            painter = painterResource(R.drawable.ic_navigate_next),
            contentDescription = title,
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant)
        )
    }
}

class SettingsNavigationRowPreviews :
    BooleanPreviewParameterProvider<SettingsNavigationRowPreviews.Params>(2) {
    data class Params(
        val darkTheme: Boolean,
        val isEnabled: Boolean
    )

    override fun createParams(booleanArray: BooleanArray) = Params(
        darkTheme = booleanArray[0],
        isEnabled = booleanArray[1]
    )

    @Preview("1x scale", locale = "en")
    @Preview("2x scale", locale = "en", fontScale = 2.0f)
    @Preview("RTL, 1x scale", locale = "he")
    @Preview("RTL, 2x scale", locale = "he", fontScale = 2.0f)
    @Composable
    fun SettingsNavigationRow_Preview(
        @PreviewParameter(SettingsNavigationRowPreviews::class) params: Params
    ) {
        NeevaTheme(useDarkTheme = params.darkTheme) {
            SettingsNavigationRow(
                title = stringResource(R.string.debug_long_string_primary),
                enabled = params.isEnabled,
                onClick = {},
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 56.dp)
                    .padding(16.dp)
                    .background(MaterialTheme.colorScheme.surface)
            )
        }
    }
}

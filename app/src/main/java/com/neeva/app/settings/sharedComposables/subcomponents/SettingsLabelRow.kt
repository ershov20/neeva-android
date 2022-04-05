package com.neeva.app.settings.sharedComposables.subcomponents

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.neeva.app.R
import com.neeva.app.ui.TwoBooleanPreviewContainer
import com.neeva.app.ui.layouts.BaseRowLayout
import com.neeva.app.ui.widgets.StackedText

@Composable
fun SettingsLabelRow(
    primaryLabel: String,
    secondaryLabel: String? = null,
    enabled: Boolean = true
) {
    BaseRowLayout {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StackedText(
                primaryLabel = primaryLabel,
                secondaryLabel = secondaryLabel,
                enabled = enabled
            )
        }
    }
}

@Preview("SettingsLabel, LTR, 1x scale", locale = "en")
@Preview("SettingsLabel, LTR, 2x scale", locale = "en", fontScale = 2.0f)
@Preview("SettingsLabel, RTL, 1x scale", locale = "he")
@Preview("SettingsLabel, RTL, 2x scale", locale = "he", fontScale = 2.0f)
@Composable
private fun SettingsLabelRowPreview() {
    TwoBooleanPreviewContainer { isEnabled, showSecondaryLabel ->
        SettingsLabelRow(
            primaryLabel = stringResource(id = R.string.debug_long_string_primary),
            secondaryLabel = if (showSecondaryLabel) {
                stringResource(R.string.debug_long_string_primary)
            } else {
                null
            },
            enabled = isEnabled
        )
    }
}

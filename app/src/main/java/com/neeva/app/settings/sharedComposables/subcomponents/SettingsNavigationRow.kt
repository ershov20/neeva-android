package com.neeva.app.settings.sharedComposables.subcomponents

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.neeva.app.R
import com.neeva.app.ui.OneBooleanPreviewContainer
import com.neeva.app.ui.widgets.ClickableRow
import com.neeva.app.ui.widgets.RowActionIconParams

@Composable
fun SettingsNavigationRow(
    primaryLabel: String,
    secondaryLabel: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
    isForDebugOnly: Boolean = false
) {
    ClickableRow(
        primaryLabel = primaryLabel, secondaryLabel = secondaryLabel,
        actionIconParams = RowActionIconParams(
            onTapAction = onClick,
            actionType = RowActionIconParams.ActionType.NAVIGATE_TO_SCREEN
        ),
        enabled = enabled,
        isForDebugOnly = isForDebugOnly
    )
}

@Preview("SettingsNavigationRow, LTR, 1x scale", locale = "en")
@Preview("SettingsNavigationRow, LTR, 2x scale", locale = "en", fontScale = 2.0f)
@Preview("SettingsNavigationRow, RTL, 1x scale", locale = "he")
@Composable
fun SettingsNavigationRowPreview() {
    OneBooleanPreviewContainer { isEnabled ->
        SettingsNavigationRow(
            primaryLabel = stringResource(R.string.debug_long_string_primary),
            secondaryLabel = stringResource(R.string.debug_long_string_primary),
            enabled = isEnabled,
            onClick = {}
        )
    }
}

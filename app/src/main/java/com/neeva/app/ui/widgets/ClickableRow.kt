package com.neeva.app.ui.widgets

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.neeva.app.R
import com.neeva.app.ui.LightDarkPreviewContainer
import com.neeva.app.ui.layouts.BaseRowLayout

@Composable
fun ClickableRow(
    primaryLabel: String,
    secondaryLabel: String? = null,
    actionIconParams: RowActionIconParams,
    enabled: Boolean = true,
    isForDebugOnly: Boolean = false
) {
    val backgroundColor = if (isForDebugOnly) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    BaseRowLayout(
        onTapRow = actionIconParams.onTapAction.takeIf { enabled },
        onTapRowContentDescription = actionIconParams.contentDescription,
        endComposable = { RowActionIcon(actionIconParams) },
        backgroundColor = backgroundColor
    ) {
        StackedText(primaryLabel = primaryLabel, secondaryLabel = secondaryLabel, enabled = enabled)
    }
}

@Preview("ClickableRow navigate, LTR, 1x font scale", locale = "en")
@Preview("ClickableRow navigate, LTR, 2x font scale", locale = "en", fontScale = 2.0f)
@Preview("ClickableRow navigate, RTL, 1x font scale", locale = "he")
@Composable
fun ClickableRowPreviewNavigate() {
    LightDarkPreviewContainer {
        ClickableRow(
            primaryLabel = stringResource(id = R.string.debug_long_string_primary),
            actionIconParams = RowActionIconParams(
                onTapAction = {},
                RowActionIconParams.ActionType.NAVIGATE_TO_SCREEN
            )
        )
    }
}

@Preview("ClickableRow open url, LTR, 1x font scale", locale = "en")
@Preview("ClickableRow open url, LTR, 2x font scale", locale = "en", fontScale = 2.0f)
@Preview("ClickableRow open url, RTL, 1x font scale", locale = "he")
@Composable
fun ClickableRowPreviewOpenUrl() {
    LightDarkPreviewContainer {
        ClickableRow(
            primaryLabel = stringResource(id = R.string.debug_long_string_primary),
            actionIconParams = RowActionIconParams(
                onTapAction = {},
                RowActionIconParams.ActionType.OPEN_URL
            )
        )
    }
}

@Preview("ClickableRow refine, LTR, 1x font scale", locale = "en")
@Preview("ClickableRow refine, LTR, 2x font scale", locale = "en", fontScale = 2.0f)
@Preview("ClickableRow refine, RTL, 1x font scale", locale = "he")
@Composable
fun ClickableRowPreviewRefine() {
    LightDarkPreviewContainer {
        ClickableRow(
            primaryLabel = stringResource(id = R.string.debug_long_string_primary),
            actionIconParams = RowActionIconParams(
                onTapAction = {},
                RowActionIconParams.ActionType.REFINE
            )
        )
    }
}

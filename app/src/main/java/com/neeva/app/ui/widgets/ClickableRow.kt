package com.neeva.app.ui.widgets

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.neeva.app.R
import com.neeva.app.ui.LightDarkPreviewContainer
import com.neeva.app.ui.layouts.BaseRowLayout

@Composable
fun ClickableRow(
    label: String,
    actionIconParams: RowActionIconParams
) {
    BaseRowLayout(
        onTapRow = actionIconParams.onTapAction,
        onTapRowContentDescription = actionIconParams.contentDescription,
        endComposable = { RowActionIcon(actionIconParams) }
    ) {
        StackedText(primaryLabel = label)
    }
}

@Preview("ClickableRow navigate, LTR, 1x font scale", locale = "en")
@Preview("ClickableRow navigate, LTR, 2x font scale", locale = "en", fontScale = 2.0f)
@Preview("ClickableRow navigate, RTL, 1x font scale", locale = "he")
@Composable
fun ClickableRowPreviewNavigate() {
    LightDarkPreviewContainer {
        ClickableRow(
            label = stringResource(id = R.string.debug_long_string_primary),
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
            label = stringResource(id = R.string.debug_long_string_primary),
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
            label = stringResource(id = R.string.debug_long_string_primary),
            actionIconParams = RowActionIconParams(
                onTapAction = {},
                RowActionIconParams.ActionType.REFINE
            )
        )
    }
}

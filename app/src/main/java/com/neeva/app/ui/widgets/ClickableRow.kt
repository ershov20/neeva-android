package com.neeva.app.ui.widgets

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.neeva.app.R
import com.neeva.app.ui.LightDarkPreviewContainer
import com.neeva.app.ui.layouts.BaseRowLayout
import com.neeva.app.ui.theme.Dimensions

@Composable
fun ClickableRow(
    primaryLabel: String,
    secondaryLabel: String? = null,
    actionIconParams: RowActionIconParams,
    enabled: Boolean = true
) {
    val backgroundColor = MaterialTheme.colorScheme.surface
    BaseRowLayout(
        onTapRow = actionIconParams.onTapAction.takeIf { enabled },
        onTapRowContentDescription = actionIconParams.contentDescription,
        endComposable = { RowActionIconButton(actionIconParams) },
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
                RowActionIconParams.ActionType.NAVIGATE_TO_SCREEN,
                size = Dimensions.SIZE_ICON_SMALL
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
                RowActionIconParams.ActionType.OPEN_URL,
                size = Dimensions.SIZE_ICON_SMALL
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

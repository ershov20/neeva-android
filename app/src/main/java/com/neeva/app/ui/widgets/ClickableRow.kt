package com.neeva.app.ui.widgets

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.neeva.app.R
import com.neeva.app.ui.LightDarkPreviewContainer
import com.neeva.app.ui.PortraitPreviews
import com.neeva.app.ui.layouts.BaseRowLayout
import com.neeva.app.ui.theme.Dimensions

@Composable
fun ClickableRow(
    primaryLabel: String,
    secondaryLabel: String? = null,
    primaryMaxLines: Int = 1,
    secondaryMaxLines: Int = 1,
    isActionDangerous: Boolean = false,
    actionIconParams: RowActionIconParams,
    enabled: Boolean = true
) {
    val backgroundColor = MaterialTheme.colorScheme.surface
    BaseRowLayout(
        onTapRow = actionIconParams.onTapAction.takeIf { enabled },
        onTapRowContentDescription = actionIconParams.contentDescription,
        endComposable = if (actionIconParams.actionType != RowActionIconParams.ActionType.NONE) {
            { RowActionIconButton(actionIconParams) }
        } else {
            null
        },
        backgroundColor = backgroundColor
    ) {
        StackedText(
            primaryLabel = primaryLabel,
            secondaryLabel = secondaryLabel,
            primaryMaxLines = primaryMaxLines,
            secondaryMaxLines = secondaryMaxLines,
            primaryColor = if (isActionDangerous) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            enabled = enabled
        )
    }
}

@PortraitPreviews
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

@PortraitPreviews
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

@PortraitPreviews
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

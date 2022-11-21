// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.ui.widgets

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.neeva.app.R
import com.neeva.app.ui.LightDarkPreviewContainer
import com.neeva.app.ui.PortraitPreviews
import com.neeva.app.ui.layouts.BaseRowLayout
import com.neeva.app.ui.theme.Dimensions

/**
 * @param contentDescription will default to [primaryLabel] if omitted
 * */
@Composable
fun ClickableRow(
    primaryLabel: String,
    secondaryLabel: String? = null,
    primaryMaxLines: Int = 1,
    secondaryMaxLines: Int = 1,
    contentDescription: String? = null,
    isDangerousAction: Boolean = false,
    onTapAction: (() -> Unit)? = null,
    onDoubleTapAction: (() -> Unit)? = null,
    onLongTapAction: (() -> Unit)? = null,
    endComposable: @Composable (() -> Unit)? = null,
    enabled: Boolean = true
) {
    BaseRowLayout(
        onTapRow = onTapAction.takeIf { enabled },
        onDoubleTapRow = onDoubleTapAction.takeIf { enabled },
        onLongTap = onLongTapAction.takeIf { enabled },
        onTapRowContentDescription = contentDescription ?: primaryLabel,
        endComposable = endComposable,
        backgroundColor = MaterialTheme.colorScheme.surface,
        applyVerticalPadding = true
    ) {
        StackedText(
            primaryLabel = primaryLabel,
            secondaryLabel = secondaryLabel,
            primaryMaxLines = primaryMaxLines,
            secondaryMaxLines = secondaryMaxLines,
            primaryColor = if (isDangerousAction) {
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
            onTapAction = {},
            endComposable = {
                RowActionIconButton(
                    onTapAction = {},
                    actionType = RowActionIconParams.ActionType.NAVIGATE_TO_SCREEN,
                    size = Dimensions.SIZE_ICON_SMALL
                )
            }
        )
    }
}

@PortraitPreviews
@Composable
fun ClickableRowPreviewOpenUrl() {
    LightDarkPreviewContainer {
        ClickableRow(
            primaryLabel = stringResource(id = R.string.debug_long_string_primary),
            onTapAction = {},
            endComposable = {
                RowActionIconButton(
                    onTapAction = {},
                    actionType = RowActionIconParams.ActionType.OPEN_URL,
                    size = Dimensions.SIZE_ICON_SMALL
                )
            }
        )
    }
}

@PortraitPreviews
@Composable
fun ClickableRowPreviewRefine() {
    LightDarkPreviewContainer {
        ClickableRow(
            primaryLabel = stringResource(id = R.string.debug_long_string_primary),
            endComposable = {
                RowActionIconButton(
                    onTapAction = {},
                    actionType = RowActionIconParams.ActionType.REFINE,
                    contentDescription = stringResource(R.string.refine),
                )
            }
        )
    }
}

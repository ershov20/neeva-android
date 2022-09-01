// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.ui.widgets

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.neeva.app.R
import com.neeva.app.ui.OneBooleanPreviewContainer
import com.neeva.app.ui.PortraitPreviews
import com.neeva.app.ui.theme.Dimensions

@Composable
fun NavigationRow(
    primaryLabel: String,
    secondaryLabel: String? = null,
    primaryMaxLines: Int = Int.MAX_VALUE,
    secondaryMaxLines: Int = Int.MAX_VALUE,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    ClickableRow(
        primaryLabel = primaryLabel,
        secondaryLabel = secondaryLabel,
        primaryMaxLines = primaryMaxLines,
        secondaryMaxLines = secondaryMaxLines,
        actionIconParams = RowActionIconParams(
            onTapAction = onClick,
            actionType = RowActionIconParams.ActionType.NAVIGATE_TO_SCREEN,
            size = Dimensions.SIZE_ICON_SMALL,
            enabled = enabled
        ),
        enabled = enabled
    )
}

@PortraitPreviews
@Composable
fun NavigationRowPreview() {
    OneBooleanPreviewContainer(useDarkTheme = false) { isEnabled ->
        NavigationRow(
            primaryLabel = stringResource(R.string.debug_long_string_primary),
            secondaryLabel = stringResource(R.string.debug_long_string_primary),
            enabled = isEnabled
        ) {}
    }
}

@PortraitPreviews
@Composable
fun NavigationRowPreviewDark() {
    OneBooleanPreviewContainer(useDarkTheme = true) { isEnabled ->
        NavigationRow(
            primaryLabel = stringResource(R.string.debug_long_string_primary),
            secondaryLabel = stringResource(R.string.debug_long_string_primary),
            enabled = isEnabled
        ) {}
    }
}

// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.suggestions

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.neeva.app.R
import com.neeva.app.ui.OneBooleanPreviewContainer
import com.neeva.app.ui.widgets.RowActionIconParams
import com.neeva.app.ui.widgets.RowActionStartIconParams
import com.neeva.app.ui.widgets.StackedText

@Composable
fun NavSuggestionRow(
    iconParams: RowActionStartIconParams,
    primaryLabel: String,
    onTapRow: () -> Unit,
    onTapRowContentDescription: String? = null,
    onLongPress: (() -> Unit)? = null,
    secondaryLabel: String? = null,
    actionIconParams: RowActionIconParams? = null,
    showActualUrlInSecondaryLabel: Boolean = false
) {
    BaseSuggestionRow(
        iconParams = iconParams,
        onTapRow = onTapRow,
        onTapRowContentDescription = onTapRowContentDescription,
        onLongTap = onLongPress,
        actionIconParams = actionIconParams
    ) {
        StackedText(
            primaryLabel = primaryLabel,
            secondaryLabel = secondaryLabel,
            showActualUrl = showActualUrlInSecondaryLabel
        )
    }
}

@Preview("Long labels, LTR, 1x", locale = "en")
@Preview("Long labels, LTR, 2x", locale = "en", fontScale = 2.0f)
@Preview("Long labels, RTL, 1x", locale = "he")
@Preview("Long labels, RTL, 2x", locale = "he", fontScale = 2.0f)
@Composable
fun NavSuggestionRowPreview_LongLabels() {
    OneBooleanPreviewContainer { allowEditing ->
        val primaryLabel = stringResource(R.string.debug_long_string_primary)
        val secondaryLabel = stringResource(R.string.debug_long_string_primary)
        val onTapEdit = {}.takeIf { allowEditing }
        NavSuggestionRow(
            iconParams = RowActionStartIconParams(),
            primaryLabel = primaryLabel,
            onTapRow = {},
            secondaryLabel = secondaryLabel,
            actionIconParams = onTapEdit?.let {
                RowActionIconParams(
                    onTapAction = it,
                    actionType = RowActionIconParams.ActionType.REFINE
                )
            }
        )
    }
}

@Preview("Short labels, LTR, 1x", locale = "en")
@Preview("Short labels, LTR, 2x", locale = "en", fontScale = 2.0f)
@Preview("Short labels, RTL, 1x", locale = "he")
@Preview("Short labels, RTL, 2x", locale = "he", fontScale = 2.0f)
@Composable
fun NavSuggestionRowPreview_ShortLabels() {
    OneBooleanPreviewContainer { allowEditing ->
        val primaryLabel = "Primary label"
        val secondaryLabel = "Secondary label"
        val onTapEdit = {}.takeIf { allowEditing }
        NavSuggestionRow(
            iconParams = RowActionStartIconParams(),
            primaryLabel = primaryLabel,
            onTapRow = {},
            secondaryLabel = secondaryLabel,
            actionIconParams = onTapEdit?.let {
                RowActionIconParams(
                    onTapAction = it,
                    actionType = RowActionIconParams.ActionType.REFINE
                )
            }
        )
    }
}

// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.settings.sharedcomposables.subcomponents

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.neeva.app.R
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.ui.theme.NeevaTheme
import com.neeva.app.ui.widgets.ClickableRow
import com.neeva.app.ui.widgets.RowActionIconButton
import com.neeva.app.ui.widgets.RowActionIconParams.ActionType.OPEN_URL

@Composable
fun SettingsLinkRow(
    label: String,
    openUrl: () -> Unit
) {
    ClickableRow(
        primaryLabel = label,
        onTapAction = openUrl,
        endComposable = {
            RowActionIconButton(
                onTapAction = openUrl,
                actionType = OPEN_URL,
                size = Dimensions.SIZE_ICON_SMALL
            )
        }
    )
}

@Preview(name = "Settings Link Row, 1x font size", locale = "en")
@Preview(name = "Settings Link Row, 2x font size", locale = "en", fontScale = 2.0f)
@Preview(name = "Settings Link Row, RTL, 1x font size", locale = "he")
@Preview(name = "Settings Link Row, RTL, 2x font size", locale = "he", fontScale = 2.0f)
@Composable
fun SettingsLinkRow_Preview() {
    NeevaTheme {
        SettingsLinkRow(
            label = stringResource(id = R.string.debug_long_string_primary),
            openUrl = {}
        )
    }
}

@Preview(name = "Settings Link Row Dark, 1x font size", locale = "en")
@Preview(name = "Settings Link Row Dark, 2x font size", locale = "en", fontScale = 2.0f)
@Preview(name = "Settings Link Row Dark, RTL, 1x font size", locale = "he")
@Preview(name = "Settings Link Row Dark, RTL, 2x font size", locale = "he", fontScale = 2.0f)
@Composable
fun SettingsLinkRow_Dark_Preview() {
    NeevaTheme(useDarkTheme = true) {
        SettingsLinkRow(
            label = stringResource(id = R.string.debug_long_string_primary),
            openUrl = {}
        )
    }
}

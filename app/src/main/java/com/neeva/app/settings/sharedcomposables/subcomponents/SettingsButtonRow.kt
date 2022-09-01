// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.settings.sharedcomposables.subcomponents

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.neeva.app.R
import com.neeva.app.ui.layouts.BaseRowLayout
import com.neeva.app.ui.theme.NeevaTheme
import com.neeva.app.ui.widgets.StackedText

@Composable
fun SettingsButtonRow(
    primaryLabel: String,
    secondaryLabel: String? = null,
    onClick: (() -> Unit)?,
    onDoubleClick: (() -> Unit)? = null
) {
    BaseRowLayout(
        onTapRow = onClick,
        onDoubleTapRow = onDoubleClick
    ) {
        StackedText(
            primaryLabel = primaryLabel,
            secondaryLabel = secondaryLabel
        )
    }
}

@Preview(name = "Settings Button, 1x font size", locale = "en")
@Preview(name = "Settings Button, 2x font size", locale = "en", fontScale = 2.0f)
@Preview(name = "Settings Button, RTL, 1x font size", locale = "he")
@Preview(name = "Settings Button, RTL, 2x font size", locale = "he", fontScale = 2.0f)
@Composable
fun SettingsButton_Preview() {
    NeevaTheme {
        SettingsButtonRow(
            primaryLabel = stringResource(R.string.debug_long_string_primary),
            onClick = {}
        )
    }
}

@Preview(name = "Settings Button Dark, 1x font size", locale = "en")
@Preview(name = "Settings Button Dark, 2x font size", locale = "en", fontScale = 2.0f)
@Preview(name = "Settings Button Dark, RTL, 1x font size", locale = "he")
@Preview(name = "Settings Button Dark, RTL, 2x font size", locale = "he", fontScale = 2.0f)
@Composable
fun SettingsButton_Dark_Preview() {
    NeevaTheme(useDarkTheme = true) {
        SettingsButtonRow(
            primaryLabel = stringResource(R.string.debug_long_string_primary),
            onClick = {}
        )
    }
}

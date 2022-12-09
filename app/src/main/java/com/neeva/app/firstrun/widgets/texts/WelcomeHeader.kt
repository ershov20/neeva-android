// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.firstrun.widgets.texts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.neeva.app.R
import com.neeva.app.firstrun.widgets.icons.WordMark
import com.neeva.app.ui.LandscapePreviews
import com.neeva.app.ui.LandscapePreviewsDark
import com.neeva.app.ui.NeevaThemePreviewContainer
import com.neeva.app.ui.PortraitPreviews
import com.neeva.app.ui.PortraitPreviewsDark

@Composable
fun WelcomeHeader(
    primaryLabel: String,
    secondaryLabel: String? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        WordMark(ColorFilter.tint(MaterialTheme.colorScheme.onSurface))

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            modifier = Modifier,
            text = primaryLabel,
            style = MaterialTheme.typography.displaySmall
        )

        if (secondaryLabel != null) {
            Text(
                text = secondaryLabel,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@PortraitPreviews
@LandscapePreviews
@Composable
fun WelcomeHeaderPreview() {
    NeevaThemePreviewContainer(useDarkTheme = false) {
        WelcomeHeader(
            primaryLabel = stringResource(id = R.string.first_run_intro),
            secondaryLabel = stringResource(id = R.string.first_run_ad_free)
        )
    }
}

@PortraitPreviewsDark
@LandscapePreviewsDark
@Composable
fun WelcomeHeaderPreviewDark() {
    NeevaThemePreviewContainer(useDarkTheme = true) {
        WelcomeHeader(
            primaryLabel = stringResource(id = R.string.first_run_intro),
            secondaryLabel = stringResource(id = R.string.first_run_ad_free)
        )
    }
}

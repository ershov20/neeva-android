// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.zeroquery.RateNeevaPromo

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import com.neeva.app.LocalScreenState
import com.neeva.app.R
import com.neeva.app.ui.LandscapePreviews
import com.neeva.app.ui.LightDarkPreviewContainer
import com.neeva.app.ui.PortraitPreviews
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.ui.util.ScreenState
import com.neeva.app.ui.widgets.StackableButtons

@Composable
internal fun GaugeSentimentPrompt(
    onTapPositive: () -> Unit,
    onTapNegative: () -> Unit,
) {
    var stackButtons by remember { mutableStateOf(false) }
    val onTextLayout: (TextLayoutResult) -> Unit = { result ->
        if (!stackButtons && result.lineCount > 4) {
            stackButtons = true
        }
    }
    ResponsiveContainer(
        modifier = Modifier.padding(Dimensions.PADDING_LARGE)
    ) { _, textModifier, buttonModifier ->
        Text(
            stringResource(R.string.neeva_experience),
            modifier = textModifier,
            onTextLayout = onTextLayout,
        )
        StackableButtons(
            primaryText = stringResource(R.string.loving_it),
            onTapPrimary = onTapPositive,
            secondaryText = stringResource(R.string.needs_work),
            onTapSecondary = onTapNegative,
            modifier = buttonModifier,
            forceStacked = stackButtons
        )
    }
}

@PortraitPreviews
@Composable
fun Preview_RateNeevaPromo_GaugeSentiment() {
    LightDarkPreviewContainer {
        RateNeevaPromo()
    }
}

@LandscapePreviews
@Composable
fun Preview_RateNeevaPromo_landscape_GaugeSentiment() {
    LightDarkPreviewContainer {
        LocalScreenState.current.orientation = ScreenState.Orientation.Landscape
        RateNeevaPromo()
    }
}

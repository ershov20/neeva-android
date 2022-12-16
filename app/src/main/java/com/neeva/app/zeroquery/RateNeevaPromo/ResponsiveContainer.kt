// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.zeroquery.RateNeevaPromo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.neeva.app.LocalScreenState
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.ui.util.ScreenState

@Composable
internal fun ResponsiveContainer(
    modifier: Modifier,
    content: @Composable (
        iconModifier: Modifier,
        textModifier: Modifier,
        buttonModifier: Modifier
    ) -> Unit
) {
    when (LocalScreenState.current.orientation) {
        ScreenState.Orientation.Portrait -> {
            Column(
                modifier = modifier,
                verticalArrangement = Arrangement.spacedBy(Dimensions.PADDING_SMALL)
            ) {
                content(
                    iconModifier = Modifier.align(Alignment.CenterHorizontally),
                    textModifier = Modifier.fillMaxWidth(),
                    buttonModifier = Modifier.align(Alignment.End)
                )
            }
        }
        ScreenState.Orientation.Landscape -> {
            Row(
                modifier = modifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimensions.PADDING_SMALL)
            ) {
                content(
                    iconModifier = Modifier.align(Alignment.CenterVertically),
                    textModifier = Modifier.weight(1f),
                    buttonModifier = Modifier
                )
            }
        }
    }
}

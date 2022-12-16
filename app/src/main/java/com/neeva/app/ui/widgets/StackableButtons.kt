// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.ui.widgets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.neeva.app.ui.theme.Dimensions

/**
 * Displays two buttons.  If both buttons fit in one [Row], they will be rendered as
 * one row with the primary button oriented on the end.
 * When buttons don't fit in a [Row], it uses a [Column] that puts the primary button
 * on top.
 *
 * In Previews, the [Column] version only shows up correctly if in Interactive Mode.
 */
@Composable
fun StackableButtons(
    primaryText: String,
    onTapPrimary: () -> Unit,
    secondaryText: String,
    onTapSecondary: () -> Unit,
    padding: Dp = Dimensions.PADDING_SMALL,
    modifier: Modifier = Modifier,
    forceStacked: Boolean = false
) {
    var useRowLayout by remember(forceStacked) { mutableStateOf(!forceStacked) }

    val maxButtonLines = if (useRowLayout) 1 else Int.MAX_VALUE

    val onTextLayout: (TextLayoutResult) -> Unit = { result ->
        if (useRowLayout && result.hasVisualOverflow) {
            useRowLayout = false
        }
    }

    val PrimaryButton = @Composable {
        Button(onClick = onTapPrimary) {
            Text(
                text = primaryText,
                maxLines = maxButtonLines,
                onTextLayout = onTextLayout
            )
        }
    }

    val SecondaryButton = @Composable {
        Button(
            onClick = onTapSecondary,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = secondaryText,
                maxLines = maxButtonLines,
                onTextLayout = onTextLayout
            )
        }
    }
    if (useRowLayout) {
        Row(horizontalArrangement = Arrangement.End, modifier = modifier) {
            SecondaryButton()
            Spacer(Modifier.size(padding))
            PrimaryButton()
        }
    } else {
        Column(horizontalAlignment = Alignment.End, modifier = modifier) {
            PrimaryButton()
            Spacer(Modifier.size(padding))
            SecondaryButton()
        }
    }
}

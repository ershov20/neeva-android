// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.ui.layouts

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import kotlin.math.round

/** Computes how many items should be shown per row in a grid, then passes it to [content]. */
@Composable
fun GridContainer(
    minItemWidth: Dp,
    modifier: Modifier = Modifier,
    content: @Composable (numCellsPerRow: Int) -> Unit
) {
    BoxWithConstraints(modifier = modifier) {
        val numCellsPerRow = round(maxWidth / minItemWidth).toInt().coerceAtLeast(2)
        content(numCellsPerRow = numCellsPerRow)
    }
}

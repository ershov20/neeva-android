// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.ui.widgets

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.neeva.app.ui.theme.Dimensions

@Composable
fun PillSurface(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    foregroundColor: Color = contentColorFor(backgroundColor = backgroundColor),
    content: @Composable () -> Unit
) {
    Surface(
        color = backgroundColor,
        contentColor = foregroundColor,
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 2.dp,
        modifier = modifier.padding(vertical = Dimensions.PADDING_SMALL)
    ) {
        content()
    }
}

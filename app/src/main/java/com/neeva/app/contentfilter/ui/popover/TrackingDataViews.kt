// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.contentfilter.ui.popover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.neeva.app.ui.theme.Dimensions

@Composable
internal fun TrackingDataBox(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    TrackingDataSurface(modifier) {
        Column(
            Modifier.padding(
                horizontal = Dimensions.PADDING_LARGE,
                vertical = Dimensions.PADDING_MEDIUM
            ),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.outline,
                style = MaterialTheme.typography.titleMedium
            )

            content()
        }
    }
}

@Composable
internal fun TrackingDataSurface(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(Dimensions.RADIUS_MEDIUM),
        tonalElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        content()
    }
}

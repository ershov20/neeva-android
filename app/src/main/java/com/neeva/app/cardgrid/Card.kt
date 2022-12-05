// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.cardgrid

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.neeva.app.ui.theme.Dimensions

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Card(
    label: String,
    onSelect: () -> Unit,
    onLongPress: (() -> Unit)? = {},
    modifier: Modifier = Modifier,
    labelStartComposable: @Composable (() -> Unit)? = null,
    labelEndComposable: @Composable (() -> Unit)? = null,
    topContent: @Composable () -> Unit
) {
    Surface(
        modifier = modifier
            .combinedClickable(enabled = true, onLongClick = onLongPress, onClick = onSelect)
    ) {
        Column(
            modifier = Modifier.padding(Dimensions.PADDING_SMALL),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.height(MINIMUM_CARD_CONTENT_HEIGHT)) {
                topContent()
            }

            Spacer(Modifier.height(Dimensions.PADDING_SMALL))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
            ) {
                if (labelStartComposable != null) {
                    labelStartComposable()
                    Spacer(modifier = Modifier.width(Dimensions.PADDING_SMALL))
                }

                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (labelEndComposable != null) {
                    Spacer(modifier = Modifier.width(Dimensions.PADDING_SMALL))
                    labelEndComposable()
                }
            }
        }
    }
}

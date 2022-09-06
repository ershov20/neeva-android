// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.ui.layouts

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.neeva.app.ui.TwoBooleanPreviewContainer
import com.neeva.app.ui.theme.Dimensions

/**
 * Base skeleton for everything that can be displayed as a row in UI, including history items,
 * navigation suggestions, and settings.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BaseRowLayout(
    modifier: Modifier = Modifier,
    onTapRow: (() -> Unit)? = null,
    onDoubleTapRow: (() -> Unit)? = null,
    onLongTap: (() -> Unit)? = null,
    onTapRowContentDescription: String? = null,
    onLongPressContentDescription: String? = null,
    startComposable: @Composable (() -> Unit)? = null,
    endComposable: @Composable (() -> Unit)? = null,
    endComposablePadding: Dp = Dimensions.PADDING_SMALL,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = contentColorFor(backgroundColor),
    applyVerticalPadding: Boolean = true,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    mainContent: @Composable () -> Unit
) {
    Surface(
        color = backgroundColor,
        contentColor = contentColor,
        modifier = modifier
            .then(
                if (onTapRow != null || onDoubleTapRow != null || onLongTap != null) {
                    Modifier.combinedClickable(
                        onClickLabel = onTapRowContentDescription,
                        onClick = onTapRow ?: {},
                        onDoubleClick = onDoubleTapRow,
                        onLongClick = onLongTap,
                        onLongClickLabel = onLongPressContentDescription
                    )
                } else {
                    Modifier
                }
            )
            .defaultMinSize(minHeight = Dimensions.SIZE_TOUCH_TARGET)
            .fillMaxWidth()
    ) {
        Row(
            verticalAlignment = verticalAlignment,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    vertical = if (applyVerticalPadding) {
                        Dimensions.PADDING_SMALL
                    } else {
                        0.dp
                    }
                )
        ) {
            startComposable?.let {
                Box(
                    modifier = Modifier
                        .defaultMinSize(Dimensions.SIZE_TOUCH_TARGET)
                        .padding(start = Dimensions.PADDING_LARGE),
                    contentAlignment = Alignment.Center
                ) {
                    it()
                }
            }

            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .padding(horizontal = Dimensions.PADDING_LARGE)
                    .weight(1.0f)
                    .defaultMinSize(minHeight = Dimensions.SIZE_TOUCH_TARGET)
            ) {
                mainContent()
            }

            endComposable?.let {
                Box(
                    modifier = Modifier
                        .defaultMinSize(Dimensions.SIZE_TOUCH_TARGET)
                        .padding(end = endComposablePadding),
                    contentAlignment = Alignment.Center
                ) {
                    it()
                }
            }
        }
    }
}

@Preview("BaseRowWidget, LTR", locale = "en")
@Preview("BaseRowWidget, RTL", locale = "he")
@Composable
fun BaseRowWidget_Preview() {
    TwoBooleanPreviewContainer { showStartComposable, showEndComposable ->
        val startComposable = @Composable {
            Box(
                modifier = Modifier
                    .background(Color.Red)
                    .size(48.dp)
            )
        }
        val endComposable = @Composable {
            Box(
                modifier = Modifier
                    .background(Color.Blue)
                    .size(48.dp)
            )
        }

        BaseRowLayout(
            onTapRow = {},
            startComposable = startComposable.takeIf { showStartComposable },
            endComposable = endComposable.takeIf { showEndComposable }
        ) {
            Box(
                modifier = Modifier
                    .background(Color.Green)
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
            )
        }
    }
}

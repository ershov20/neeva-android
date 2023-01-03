// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.ui

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.offset
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FractionalThreshold
import androidx.compose.material.rememberSwipeableState
import androidx.compose.material.swipeable
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity

private enum class SwipeDirection {
    Left,
    Initial,
    Right,
}

/**
 * A SnackbarHost that enables swipe-to-clear.
 * This should be a drop-in replacement for the normal SnackbarHost
 * This code was adapted from: https://stackoverflow.com/questions/69236878
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SnackbarHost(hostState: SnackbarHostState) {
    if (hostState.currentSnackbarData == null) {
        return
    }
    var width = LocalContext.current.resources.displayMetrics.widthPixels

    val swipeableState = rememberSwipeableState(SwipeDirection.Initial)

    if (swipeableState.isAnimationRunning) {
        DisposableEffect(Unit) {
            onDispose {
                when (swipeableState.currentValue) {
                    SwipeDirection.Right,
                    SwipeDirection.Left -> {
                        hostState.currentSnackbarData?.dismiss()
                    }
                    SwipeDirection.Initial -> {
                        return@onDispose
                    }
                }
            }
        }
    }
    val offset = with(LocalDensity.current) {
        swipeableState.offset.value.toDp()
    }
    SnackbarHost(
        hostState,
        snackbar = { snackbarData ->
            Snackbar(
                snackbarData,
                modifier = Modifier.offset(x = offset)
            )
        },
        modifier = Modifier
            .swipeable(
                state = swipeableState,
                anchors = mapOf(
                    -width.toFloat() to SwipeDirection.Left,
                    0f to SwipeDirection.Initial,
                    width.toFloat() to SwipeDirection.Right,
                ),
                thresholds = { _, _ -> FractionalThreshold(0.3f) },
                orientation = Orientation.Horizontal
            )
    )
}

// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.Composable

@Composable
fun AnimatedExpandShrink(
    isVisible: Boolean,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = expandVertically(
            animationSpec = tween(AnimationConstants.ANIMATION_DURATION_MS)
        ),
        exit = shrinkVertically(
            animationSpec = tween(AnimationConstants.ANIMATION_DURATION_MS)
        )
    ) {
        content()
    }
}

// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.ui.theme

import androidx.compose.ui.unit.dp

object Dimensions {
    val PADDING_TINY = 4.dp
    val PADDING_SMALL = 8.dp
    val PADDING_MEDIUM = 12.dp
    val PADDING_LARGE = 16.dp
    val PADDING_HUGE = 24.dp

    val RADIUS_TINY = 4.dp
    val RADIUS_SMALL = 8.dp
    val RADIUS_MEDIUM = 12.dp
    val RADIUS_LARGE = 16.dp

    /** Minimum size of an accessible touch target. */
    val SIZE_TOUCH_TARGET = 48.dp

    val SIZE_ICON_SMALL = 16.dp
    val SIZE_ICON_MEDIUM = 24.dp
    val SIZE_ICON_TOOLBAR = 21.dp

    val SCROLL_GRADIENT_INDICATOR = 48.dp

    /** Default size of a Material icon. */
    val SIZE_ICON = 24.dp

    /**
     * Pre-calculated value for full-bleed Composables that need to match the size of an icon
     * wrapped inside of a container with PADDING_SMALL on all sides.
     */
    val SIZE_ICON_INCLUDING_PADDING = SIZE_ICON + (PADDING_SMALL * 2)
}

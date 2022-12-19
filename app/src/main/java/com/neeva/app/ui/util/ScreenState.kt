// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.ui.util

import android.content.Context
import android.content.res.Configuration
import android.graphics.drawable.GradientDrawable.Orientation
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.neeva.app.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScreenState @Inject constructor() {
    var orientation by mutableStateOf(Orientation.Portrait)

    fun configure(context: Context) {
        context.resources.apply {
            val minScreenWidth = getDimensionPixelSize(R.dimen.min_screen_width_for_one_toolbar)
            val isWide = displayMetrics.widthPixels >= minScreenWidth
            val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            orientation = when {
                (isLandscape || isWide) -> Orientation.Landscape
                else -> Orientation.Portrait
            }
        }
    }

    enum class Orientation { Portrait, Landscape }
}

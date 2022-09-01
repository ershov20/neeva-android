// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.firstrun

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

object FirstRunConstants {
    @Composable
    fun getSubtextStyle(color: Color = MaterialTheme.colorScheme.onSurfaceVariant): TextStyle {
        return MaterialTheme.typography.bodyMedium
            .copy(color = color)
    }
}

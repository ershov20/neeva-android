// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.firstrun.widgets.icons

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import com.neeva.app.R

@Composable
fun WordMark(colorFilter: ColorFilter, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = R.drawable.ic_wordmark),
        contentDescription = null,
        colorFilter = colorFilter,
        modifier = modifier
    )
}

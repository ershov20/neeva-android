// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.browsing.toolbar

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.neeva.app.R

/** Icon used to represent the tab switcher or the regular tab grid. */
@Composable
fun TabSwitcherIcon(contentDescription: String = "") {
    Icon(
        painter = painterResource(R.drawable.ic_tabs_24px),
        contentDescription = contentDescription,
        modifier = Modifier.size(24.dp)
    )
}

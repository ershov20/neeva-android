// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.browsing.toolbar

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.StateFlow

@Composable
fun LoadingBar(
    progressFlow: StateFlow<Int>,
    modifier: Modifier = Modifier
) {
    val progress: Int = progressFlow.collectAsState().value
    if (progress != 100) {
        LinearProgressIndicator(
            progress = progress / 100.0f,
            modifier = Modifier
                .height(2.dp)
                .fillMaxWidth()
                .then(modifier),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primaryContainer
        )
    }
}

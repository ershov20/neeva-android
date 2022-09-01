// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.ui.widgets

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import com.neeva.app.ui.theme.Dimensions

@Composable
fun SplitStringRow(
    primary: String,
    secondaryPieces: List<String>,
    separator: String,
    modifier: Modifier = Modifier,
    primaryStyle: TextStyle = MaterialTheme.typography.bodySmall,
    secondaryStyle: TextStyle = MaterialTheme.typography.bodySmall
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Text(
            text = primary,
            overflow = TextOverflow.Ellipsis,
            style = primaryStyle,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )

        secondaryPieces.forEach { secondaryPiece ->
            Text(
                text = separator,
                overflow = TextOverflow.Ellipsis,
                style = secondaryStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = Dimensions.PADDING_TINY)
            )

            Text(
                text = secondaryPiece,
                overflow = TextOverflow.Ellipsis,
                style = secondaryStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.history

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.neeva.app.R
import com.neeva.app.ui.NeevaThemePreviewContainer
import com.neeva.app.ui.PortraitPreviews
import com.neeva.app.ui.PortraitPreviewsDark
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.ui.toLocalDate
import com.neeva.app.ui.widgets.DefaultDivider
import com.neeva.app.ui.widgets.HeavyDivider
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun HistoryHeader(timestamp: Long, useHeavyDivider: Boolean = false) {
    HistoryHeader(localDate = timestamp.toLocalDate(), useHeavyDivider = useHeavyDivider)
}

@Composable
fun HistoryHeader(localDate: LocalDate, useHeavyDivider: Boolean = false) {
    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("EEEE, MMMM d")
    }

    HistoryHeader(
        text = dateFormatter.format(localDate),
        useHeavyDivider = useHeavyDivider
    )
}

@Composable
fun HistoryHeader(text: String, useHeavyDivider: Boolean = false) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (useHeavyDivider) {
            HeavyDivider()
        } else {
            DefaultDivider()
        }

        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = Dimensions.PADDING_LARGE)
        )

        Spacer(modifier = Modifier.height(Dimensions.PADDING_SMALL))
    }
}

@PortraitPreviews
@Composable
fun HistoryHeaderPreview_Light() {
    NeevaThemePreviewContainer(useDarkTheme = false) {
        Surface {
            HistoryHeader(stringResource(id = R.string.debug_long_string_primary))
        }
    }
}

@PortraitPreviewsDark
@Composable
fun HistoryHeaderPreview_Dark() {
    NeevaThemePreviewContainer(useDarkTheme = true) {
        Surface {
            HistoryHeader(stringResource(id = R.string.debug_long_string_primary))
        }
    }
}

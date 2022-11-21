// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.neeva.app.R
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.ui.widgets.DefaultDivider
import com.neeva.app.ui.widgets.HeavyDivider

@Composable
fun SectionHeader(stringId: Int? = null, heavy: Boolean = false) {
    Column(
        modifier = Modifier
            .background(color = MaterialTheme.colorScheme.surface)
    ) {
        if (heavy) {
            HeavyDivider()
        } else {
            DefaultDivider()
        }

        if (stringId != null) {
            Text(
                text = stringResource(stringId),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                modifier = Modifier
                    .padding(horizontal = Dimensions.PADDING_LARGE)
                    .padding(bottom = Dimensions.PADDING_SMALL),
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@PortraitPreviews
@Composable
private fun SectionHeaderPreview() {
    TwoBooleanPreviewContainer { useLongText, heavy ->
        val titleString = if (useLongText) {
            R.string.debug_long_string_primary
        } else {
            R.string.debug_short_action
        }

        SectionHeader(titleString, heavy = heavy)
    }
}

// Separator size should *not* adjust with font size, so both sets of previews should
// be identical.
@NoTextPortraitPreviews
@Composable
private fun NoTextSectionHeaderPreview() {
    OneBooleanPreviewContainer { heavy ->
        SectionHeader(heavy = heavy)
    }
}

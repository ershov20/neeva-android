// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.ui.widgets

import android.net.Uri
import android.webkit.URLUtil
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import com.neeva.app.R
import com.neeva.app.ui.OneBooleanPreviewContainer
import com.neeva.app.ui.PortraitPreviews
import com.neeva.app.ui.theme.getClickableAlpha

@Composable
fun StackedText(
    primaryLabel: String,
    secondaryLabel: String? = null,
    primaryTextStyle: TextStyle = MaterialTheme.typography.titleMedium,
    secondaryTextStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    primaryMaxLines: Int = 1,
    secondaryMaxLines: Int = 1,
    primaryColor: Color? = MaterialTheme.colorScheme.onBackground,
    showActualUrl: Boolean = false,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.alpha(getClickableAlpha(enabled))) {
        Text(
            text = primaryLabel,
            style = primaryTextStyle,
            color = primaryColor ?: MaterialTheme.colorScheme.onBackground,
            maxLines = primaryMaxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )

        secondaryLabel?.let {
            if (!showActualUrl && URLUtil.isValidUrl(secondaryLabel)) {
                UriDisplayView(Uri.parse(secondaryLabel))
            } else {
                Text(
                    text = secondaryLabel,
                    overflow = TextOverflow.Ellipsis,
                    style = secondaryTextStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = secondaryMaxLines,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@PortraitPreviews
@Composable
fun StackedTextPreview() {
    OneBooleanPreviewContainer { hasSecondaryLabel ->
        StackedText(
            primaryLabel = stringResource(R.string.debug_short_string_primary),
            secondaryLabel =
            stringResource(R.string.debug_long_string_secondary).takeIf { hasSecondaryLabel }
        )
    }
}

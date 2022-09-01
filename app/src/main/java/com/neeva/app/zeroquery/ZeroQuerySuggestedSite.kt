// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.zeroquery

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.neeva.app.NeevaConstants
import com.neeva.app.R
import com.neeva.app.publicsuffixlist.DomainProvider
import com.neeva.app.storage.entities.Favicon.Companion.toBitmap
import com.neeva.app.suggestions.toUserVisibleString
import com.neeva.app.ui.OneBooleanPreviewContainer
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.ui.widgets.FaviconView

@Composable
fun ZeroQuerySuggestedSite(
    suggestedSite: SuggestedSite,
    domainProvider: DomainProvider,
    onClick: (Uri) -> Unit
) {
    val siteUri = Uri.parse(suggestedSite.site.siteURL)
    val label = suggestedSite.site.toUserVisibleString(domainProvider)

    ZeroQuerySuggestedSite(
        faviconBitmap = suggestedSite.bitmap,
        overrideDrawableId = suggestedSite.overrideDrawableId,
        label = label,
        onClick = { onClick(siteUri) }
    )
}

@Composable
fun ZeroQuerySuggestedSite(
    faviconBitmap: Bitmap?,
    overrideDrawableId: Int?,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = label) {
                onClick()
            }
            .padding(
                start = Dimensions.PADDING_LARGE,
                end = Dimensions.PADDING_LARGE
            )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            FaviconView(
                bitmap = faviconBitmap,
                overrideDrawableId = overrideDrawableId
            )

            Spacer(modifier = Modifier.height(Dimensions.PADDING_SMALL))

            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview("Small container, LTR, 1x font scale", locale = "en")
@Preview("Small container, LTR, 2x font scale", locale = "en", fontScale = 2.0f)
@Preview("Small container, RTL, 1x font scale", locale = "he")
@Composable
private fun ZeroQuerySuggestedSitePreview_SmallContainer() {
    OneBooleanPreviewContainer { useLongTitle ->
        val label = if (useLongTitle) {
            stringResource(id = R.string.debug_long_string_primary)
        } else {
            "Short"
        }

        val containerSize = 96.dp
        val neevaConstants = NeevaConstants()
        Box(modifier = Modifier.width(containerSize)) {
            ZeroQuerySuggestedSite(
                faviconBitmap = Uri.parse(neevaConstants.appURL).toBitmap(),
                overrideDrawableId = null,
                label = label,
                onClick = {}
            )
        }
    }
}

@Preview("Large container, LTR, 1x font scale", locale = "en")
@Preview("Large container, LTR, 2x font scale", locale = "en", fontScale = 2.0f)
@Preview("Large container, RTL, 1x font scale", locale = "he")
@Composable
private fun ZeroQuerySuggestedSitePreview_LargeContainer() {
    OneBooleanPreviewContainer { useLongTitle ->
        val label = if (useLongTitle) {
            stringResource(id = R.string.debug_long_string_primary)
        } else {
            "Short"
        }

        val containerSize = 300.dp
        val neevaConstants = NeevaConstants()
        Box(modifier = Modifier.width(containerSize)) {
            ZeroQuerySuggestedSite(
                faviconBitmap = Uri.parse(neevaConstants.appURL).toBitmap(),
                overrideDrawableId = null,
                label = label,
                onClick = {}
            )
        }
    }
}

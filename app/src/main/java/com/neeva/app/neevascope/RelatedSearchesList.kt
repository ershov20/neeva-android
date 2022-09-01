// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.neevascope

import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import com.neeva.app.NeevaConstants
import com.neeva.app.R
import com.neeva.app.browsing.toSearchUri
import com.neeva.app.ui.LandscapePreviews
import com.neeva.app.ui.LightDarkPreviewContainer
import com.neeva.app.ui.PortraitPreviews
import com.neeva.app.ui.theme.Dimensions

fun LazyListScope.RelatedSearchesList(
    @StringRes title: Int,
    searches: List<String>,
    openUrl: (Uri) -> Unit,
    neevaConstants: NeevaConstants,
    onDismiss: () -> Unit
) {
    item {
        NeevaScopeSectionHeader(title = title)
        Spacer(modifier = Modifier.padding(Dimensions.PADDING_SMALL))
    }

    items(searches) { search ->
        RelatedSearchRow(
            search,
            onTapRow = {
                openUrl(search.toSearchUri(neevaConstants))
                onDismiss()
            }
        )
    }

    item {
        NeevaScopeDivider()
    }
}

@Composable
fun RelatedSearchRow(
    search: String,
    onTapRow: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Dimensions.PADDING_SMALL),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = Dimensions.SIZE_TOUCH_TARGET)
            .clickable {
                onTapRow()
            }
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_baseline_search_24),
            contentDescription = null,
            modifier = Modifier.size(Dimensions.SIZE_ICON_SMALL)
        )

        Text(
            text = search,
            maxLines = 1,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@PortraitPreviews
@LandscapePreviews
@Composable
fun RelatedSearchRow_Preview() {
    LightDarkPreviewContainer {
        RelatedSearchRow(
            search = "Related search",
            onTapRow = {}
        )
    }
}

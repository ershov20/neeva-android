// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.contentfilter.ui.popover

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.neeva.app.R
import com.neeva.app.contentfilter.TrackersAllowList
import com.neeva.app.contentfilter.TrackingData
import com.neeva.app.contentfilter.TrackingEntity
import com.neeva.app.ui.LandscapePreviews
import com.neeva.app.ui.NeevaThemePreviewContainer
import com.neeva.app.ui.PortraitPreviews
import com.neeva.app.ui.PortraitPreviewsDark
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.ui.widgets.NavigationRow

@Composable
fun ContentFilterPopover(
    contentFilterPopoverModel: ContentFilterPopoverModel,
    modifier: Modifier = Modifier
) {
    // TODO(kobec): the offset does not work in landscape
    val topOffset = with(LocalDensity.current) {
        dimensionResource(id = R.dimen.top_toolbar_height).roundToPx()
    }
    Popup(
        offset = IntOffset(x = 0, y = topOffset),
        onDismissRequest = contentFilterPopoverModel::dismissPopover,
        properties = PopupProperties(focusable = true)
    ) {
        ContentFilterPopoverContent(
            contentFilterPopoverModel = contentFilterPopoverModel,
            modifier = modifier
        )
    }
}

@Composable
private fun ContentFilterPopoverContent(
    contentFilterPopoverModel: ContentFilterPopoverModel,
    modifier: Modifier = Modifier
) {
    val hostFlow by contentFilterPopoverModel.urlFlow.collectAsState()
    val host = hostFlow.host ?: ""

    // Default to saying that trackers are disallowed to avoid the TrackingDataDisplay UI appearing
    // and then disappearing immediately.
    val trackersAllowList = contentFilterPopoverModel.trackersAllowList
    val allowsTrackersFlow by trackersAllowList
        .getHostAllowsTrackersFlow(host)
        .collectAsState(true)
    val isContentFilterEnabled = !allowsTrackersFlow

    ContentFilterPopoverContent(
        host = host,
        isContentFilterEnabled = isContentFilterEnabled,
        trackersAllowList = trackersAllowList,
        contentFilterPopoverModel = contentFilterPopoverModel,
        modifier = modifier
    )
}

@Composable
private fun ContentFilterPopoverContent(
    host: String,
    isContentFilterEnabled: Boolean,
    trackersAllowList: TrackersAllowList,
    contentFilterPopoverModel: ContentFilterPopoverModel,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(Dimensions.RADIUS_SMALL),
        shadowElevation = 2.dp,
        modifier = modifier
            .widthIn(max = 480.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Column(
            modifier = Modifier
                .padding(Dimensions.PADDING_LARGE)
                .fillMaxWidth()
        ) {
            TrackingDataDisplay(
                visible = isContentFilterEnabled,
                contentFilterPopoverModel = contentFilterPopoverModel
            )

            // Cookie Cutter Popover Settings:
            TrackingDataSurface {
                Column {
                    ContentFilterPopoverSwitch(
                        contentFilterEnabled = isContentFilterEnabled,
                        host = host,
                        trackersAllowList = trackersAllowList,
                        onSuccess = { contentFilterPopoverModel.onReloadTab() }
                    )

                    Spacer(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .height(1.dp)
                            .fillMaxWidth()
                    )

                    NavigationRow(
                        primaryLabel = stringResource(R.string.content_filter_settings),
                        onClick = contentFilterPopoverModel::openContentFilterSettings
                    )
                }
            }
        }
    }
}

private val previewContentFilterPopoverModel = PreviewContentFilterPopoverModel(
    trackingData = TrackingData(
        numTrackers = 999,
        trackingEntities = mapOf(
            TrackingEntity.GOOGLE to 500,
            TrackingEntity.AMAZON to 38
        )
    )
)

@PortraitPreviews
@Composable
private fun ContentFilterPopoverContentPreview_Light_Partial() {
    NeevaThemePreviewContainer(useDarkTheme = false) {
        ContentFilterPopoverContent(
            contentFilterPopoverModel = previewContentFilterPopoverModel
        )
    }
}

@PortraitPreviews
@LandscapePreviews
@Composable
private fun ContentFilterPopoverContentPreview_Light_Full() {
    NeevaThemePreviewContainer(useDarkTheme = false) {
        ContentFilterPopoverContent(
            host = "unused",
            isContentFilterEnabled = true,
            trackersAllowList = previewContentFilterPopoverModel.trackersAllowList,
            contentFilterPopoverModel = previewContentFilterPopoverModel
        )
    }
}

@PortraitPreviewsDark
@Composable
private fun ContentFilterPopoverContentPreview_Dark() {
    NeevaThemePreviewContainer(useDarkTheme = true) {
        ContentFilterPopoverContent(
            host = "unused",
            isContentFilterEnabled = true,
            trackersAllowList = previewContentFilterPopoverModel.trackersAllowList,
            contentFilterPopoverModel = previewContentFilterPopoverModel
        )
    }
}

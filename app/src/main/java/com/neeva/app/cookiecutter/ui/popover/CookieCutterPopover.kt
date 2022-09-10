// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.cookiecutter.ui.popover

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
import com.neeva.app.cookiecutter.TrackersAllowList
import com.neeva.app.cookiecutter.TrackingData
import com.neeva.app.cookiecutter.TrackingEntity
import com.neeva.app.ui.LandscapePreviews
import com.neeva.app.ui.NeevaThemePreviewContainer
import com.neeva.app.ui.PortraitPreviews
import com.neeva.app.ui.PortraitPreviewsDark
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.ui.widgets.NavigationRow

@Composable
fun CookieCutterPopover(
    cookieCutterPopoverModel: CookieCutterPopoverModel,
    modifier: Modifier = Modifier
) {
    // TODO(kobec): the offset does not work in landscape
    val topOffset = with(LocalDensity.current) {
        dimensionResource(id = R.dimen.top_toolbar_height).roundToPx()
    }
    Popup(
        offset = IntOffset(x = 0, y = topOffset),
        onDismissRequest = cookieCutterPopoverModel::dismissPopover,
        properties = PopupProperties(focusable = true)
    ) {
        CookieCutterPopoverContent(
            cookieCutterPopoverModel = cookieCutterPopoverModel,
            modifier = modifier
        )
    }
}

@Composable
private fun CookieCutterPopoverContent(
    cookieCutterPopoverModel: CookieCutterPopoverModel,
    modifier: Modifier = Modifier
) {
    val hostFlow by cookieCutterPopoverModel.urlFlow.collectAsState()
    val host = hostFlow.host ?: ""

    // Default to saying that trackers are disallowed to avoid the TrackingDataDisplay UI appearing
    // and then disappearing immediately.
    val trackersAllowList = cookieCutterPopoverModel.trackersAllowList
    val allowsTrackersFlow by trackersAllowList
        .getHostAllowsTrackersFlow(host)
        .collectAsState(true)
    val isCookieCutterEnabled = !allowsTrackersFlow

    CookieCutterPopoverContent(
        host = host,
        isCookieCutterEnabled = isCookieCutterEnabled,
        trackersAllowList = trackersAllowList,
        cookieCutterPopoverModel = cookieCutterPopoverModel,
        modifier = modifier
    )
}

@Composable
private fun CookieCutterPopoverContent(
    host: String,
    isCookieCutterEnabled: Boolean,
    trackersAllowList: TrackersAllowList,
    cookieCutterPopoverModel: CookieCutterPopoverModel,
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
                visible = isCookieCutterEnabled,
                cookieCutterPopoverModel = cookieCutterPopoverModel
            )

            // Cookie Cutter Popover Settings:
            TrackingDataSurface {
                Column {
                    CookieCutterPopoverSwitch(
                        cookieCutterEnabled = isCookieCutterEnabled,
                        host = host,
                        trackersAllowList = trackersAllowList,
                        onSuccess = { cookieCutterPopoverModel.onReloadTab() }
                    )

                    Spacer(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .height(1.dp)
                            .fillMaxWidth()
                    )

                    NavigationRow(
                        primaryLabel = stringResource(R.string.cookie_cutter_settings),
                        onClick = cookieCutterPopoverModel::openCookieCutterSettings
                    )
                }
            }
        }
    }
}

private val previewCookieCutterPopoverModel = PreviewCookieCutterPopoverModel(
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
private fun CookieCutterPopoverContentPreview_Light_Partial() {
    NeevaThemePreviewContainer(useDarkTheme = false) {
        CookieCutterPopoverContent(
            cookieCutterPopoverModel = previewCookieCutterPopoverModel
        )
    }
}

@PortraitPreviews
@LandscapePreviews
@Composable
private fun CookieCutterPopoverContentPreview_Light_Full() {
    NeevaThemePreviewContainer(useDarkTheme = false) {
        CookieCutterPopoverContent(
            host = "unused",
            isCookieCutterEnabled = true,
            trackersAllowList = previewCookieCutterPopoverModel.trackersAllowList,
            cookieCutterPopoverModel = previewCookieCutterPopoverModel
        )
    }
}

@PortraitPreviewsDark
@Composable
private fun CookieCutterPopoverContentPreview_Dark() {
    NeevaThemePreviewContainer(useDarkTheme = true) {
        CookieCutterPopoverContent(
            host = "unused",
            isCookieCutterEnabled = true,
            trackersAllowList = previewCookieCutterPopoverModel.trackersAllowList,
            cookieCutterPopoverModel = previewCookieCutterPopoverModel
        )
    }
}

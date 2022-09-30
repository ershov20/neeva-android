// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.contentfilter.ui.popover

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.neeva.app.appnav.AppNavModel
import com.neeva.app.contentfilter.ContentFilterModel
import com.neeva.app.contentfilter.PreviewTrackersAllowList
import com.neeva.app.contentfilter.TrackersAllowList
import com.neeva.app.contentfilter.TrackingData
import com.neeva.app.contentfilter.TrackingEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Stores all controller and state logic needed in [ContentFilterPopover] UI. */
interface ContentFilterPopoverModel {
    val trackingDataFlow: StateFlow<TrackingData?>
    val cookieNoticeBlocked: StateFlow<Boolean>
    val trackersAllowList: TrackersAllowList
    val popoverVisible: MutableState<Boolean>
    val urlFlow: StateFlow<Uri>

    val onReloadTab: () -> Unit
    fun openContentFilterSettings()
    fun openPopover()
    fun dismissPopover()
}

@Composable
fun rememberContentFilterPopoverModel(
    appNavModel: AppNavModel,
    reloadTab: () -> Unit,
    contentFilterModel: ContentFilterModel,
    urlFlow: StateFlow<Uri>
): ContentFilterPopoverModel {
    val popoverVisible = remember { mutableStateOf(false) }

    return remember(appNavModel, contentFilterModel, popoverVisible, urlFlow) {
        ContentFilterPopoverModelImpl(
            appNavModel = appNavModel,
            popoverVisible = popoverVisible,
            trackingDataFlow = contentFilterModel.trackingDataFlow,
            cookieNoticeBlocked = contentFilterModel.cookieNoticeBlockedFlow,
            trackersAllowList = contentFilterModel.trackersAllowList,
            urlFlow = urlFlow,
            onReloadTab = reloadTab
        )
    }
}

class ContentFilterPopoverModelImpl(
    private val appNavModel: AppNavModel,
    override val popoverVisible: MutableState<Boolean>,
    override val trackingDataFlow: StateFlow<TrackingData?>,
    override val cookieNoticeBlocked: StateFlow<Boolean>,
    override val trackersAllowList: TrackersAllowList,
    override val urlFlow: StateFlow<Uri>,
    override val onReloadTab: () -> Unit
) : ContentFilterPopoverModel {
    override fun openContentFilterSettings() {
        dismissPopover()
        appNavModel.showContentFilterSettings()
    }

    override fun openPopover() {
        popoverVisible.value = true
    }

    override fun dismissPopover() {
        popoverVisible.value = false
    }
}

class PreviewContentFilterPopoverModel(
    trackingData: TrackingData = TrackingData(
        numTrackers = 999,
        trackingEntities = mapOf(
            TrackingEntity.GOOGLE to 500,
            TrackingEntity.AMAZON to 38,
            TrackingEntity.WARNERMEDIA to 4,
            TrackingEntity.CRITEO to 19
        )
    )
) : ContentFilterPopoverModel {
    override val trackingDataFlow: StateFlow<TrackingData?> = MutableStateFlow(trackingData)
    override val cookieNoticeBlocked = MutableStateFlow(true)
    override val trackersAllowList = PreviewTrackersAllowList()
    override val popoverVisible = mutableStateOf(false)
    override val urlFlow: StateFlow<Uri> = MutableStateFlow(Uri.parse("www.neeva.com"))
    override val onReloadTab: () -> Unit = { }

    override fun openContentFilterSettings() {}
    override fun openPopover() {}
    override fun dismissPopover() {}
}

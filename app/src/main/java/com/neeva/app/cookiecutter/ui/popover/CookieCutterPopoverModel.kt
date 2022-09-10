// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.cookiecutter.ui.popover

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.neeva.app.appnav.AppNavModel
import com.neeva.app.cookiecutter.CookieCutterModel
import com.neeva.app.cookiecutter.PreviewTrackersAllowList
import com.neeva.app.cookiecutter.TrackersAllowList
import com.neeva.app.cookiecutter.TrackingData
import com.neeva.app.cookiecutter.TrackingEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Stores all controller and state logic needed in [CookieCutterPopover] UI. */
interface CookieCutterPopoverModel {
    val trackingDataFlow: StateFlow<TrackingData?>
    val cookieNoticeBlocked: StateFlow<Boolean>
    val trackersAllowList: TrackersAllowList
    val popoverVisible: MutableState<Boolean>
    val urlFlow: StateFlow<Uri>

    val onReloadTab: () -> Unit
    fun openCookieCutterSettings()
    fun openPopover()
    fun dismissPopover()
}

@Composable
fun rememberCookieCutterPopoverModel(
    appNavModel: AppNavModel,
    reloadTab: () -> Unit,
    cookieCutterModel: CookieCutterModel,
    urlFlow: StateFlow<Uri>
): CookieCutterPopoverModel {
    val popoverVisible = remember { mutableStateOf(false) }

    return remember(appNavModel, cookieCutterModel, popoverVisible, urlFlow) {
        CookieCutterPopoverModelImpl(
            appNavModel = appNavModel,
            popoverVisible = popoverVisible,
            trackingDataFlow = cookieCutterModel.trackingDataFlow,
            cookieNoticeBlocked = cookieCutterModel.cookieNoticeBlockedFlow,
            trackersAllowList = cookieCutterModel.trackersAllowList,
            urlFlow = urlFlow,
            onReloadTab = reloadTab
        )
    }
}

class CookieCutterPopoverModelImpl(
    private val appNavModel: AppNavModel,
    override val popoverVisible: MutableState<Boolean>,
    override val trackingDataFlow: StateFlow<TrackingData?>,
    override val cookieNoticeBlocked: StateFlow<Boolean>,
    override val trackersAllowList: TrackersAllowList,
    override val urlFlow: StateFlow<Uri>,
    override val onReloadTab: () -> Unit
) : CookieCutterPopoverModel {
    override fun openCookieCutterSettings() {
        dismissPopover()
        appNavModel.showCookieCutterSettings()
    }

    override fun openPopover() {
        popoverVisible.value = true
    }

    override fun dismissPopover() {
        popoverVisible.value = false
    }
}

class PreviewCookieCutterPopoverModel(
    trackingData: TrackingData = TrackingData(
        numTrackers = 999,
        trackingEntities = mapOf(
            TrackingEntity.GOOGLE to 500,
            TrackingEntity.AMAZON to 38,
            TrackingEntity.WARNERMEDIA to 4,
            TrackingEntity.CRITEO to 19
        )
    )
) : CookieCutterPopoverModel {
    override val trackingDataFlow: StateFlow<TrackingData?> = MutableStateFlow(trackingData)
    override val cookieNoticeBlocked = MutableStateFlow(true)
    override val trackersAllowList = PreviewTrackersAllowList()
    override val popoverVisible = mutableStateOf(false)
    override val urlFlow: StateFlow<Uri> = MutableStateFlow(Uri.parse("www.neeva.com"))
    override val onReloadTab: () -> Unit = { }

    override fun openCookieCutterSettings() {}
    override fun openPopover() {}
    override fun dismissPopover() {}
}

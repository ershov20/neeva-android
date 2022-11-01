// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.contentfilter.ui.popover

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.neeva.app.LocalSharedPreferencesModel
import com.neeva.app.appnav.AppNavModel
import com.neeva.app.contentfilter.ContentFilterModel
import com.neeva.app.contentfilter.PreviewTrackersAllowList
import com.neeva.app.contentfilter.TrackersAllowList
import com.neeva.app.contentfilter.TrackingData
import com.neeva.app.contentfilter.TrackingEntity
import com.neeva.app.sharedprefs.SharedPrefFolder
import com.neeva.app.sharedprefs.SharedPreferencesModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Stores all controller and state logic needed in [ContentFilterPopover] UI. */
interface ContentFilterPopoverModel {
    val trackingDataFlow: StateFlow<TrackingData?>
    val cookieNoticeBlocked: StateFlow<Boolean>
    val easyListRuleBlocked: StateFlow<Boolean>
    val trackersAllowList: TrackersAllowList
    val popoverVisible: MutableState<Boolean>
    val shouldShowOnboardingPopover: MutableState<Boolean>
    val urlFlow: StateFlow<Uri>

    val onReloadTab: () -> Unit
    fun openContentFilterSettings()
    fun openPopover()
    fun dismissPopover()
    fun openOnboardingPopover()
    fun dismissOnboardingPopover()
}

@Composable
fun rememberContentFilterPopoverModel(
    appNavModel: AppNavModel,
    reloadTab: () -> Unit,
    contentFilterModel: ContentFilterModel,
    urlFlow: StateFlow<Uri>
): ContentFilterPopoverModel {
    val popoverVisible = remember { mutableStateOf(false) }
    val shouldShowOnboardingPopover = remember { mutableStateOf(false) }

    val sharedPreferenceModel = LocalSharedPreferencesModel.current

    return remember(
        appNavModel,
        contentFilterModel,
        popoverVisible,
        shouldShowOnboardingPopover,
        urlFlow
    ) {
        ContentFilterPopoverModelImpl(
            appNavModel = appNavModel,
            popoverVisible = popoverVisible,
            sharedPreferenceModel = sharedPreferenceModel,
            shouldShowOnboardingPopover = shouldShowOnboardingPopover,
            trackingDataFlow = contentFilterModel.trackingDataFlow,
            cookieNoticeBlocked = contentFilterModel.cookieNoticeBlockedFlow,
            easyListRuleBlocked = contentFilterModel.easyListRuleBlockedFlow,
            trackersAllowList = contentFilterModel.trackersAllowList,
            urlFlow = urlFlow,
            onReloadTab = reloadTab
        )
    }
}

class ContentFilterPopoverModelImpl(
    private val appNavModel: AppNavModel,
    override val popoverVisible: MutableState<Boolean>,
    private val sharedPreferenceModel: SharedPreferencesModel,
    override val shouldShowOnboardingPopover: MutableState<Boolean>,
    override val trackingDataFlow: StateFlow<TrackingData?>,
    override val cookieNoticeBlocked: StateFlow<Boolean>,
    override val easyListRuleBlocked: StateFlow<Boolean>,
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

    override fun openOnboardingPopover() {
        popoverVisible.value = false
        shouldShowOnboardingPopover.value = true
    }

    override fun dismissOnboardingPopover() {
        SharedPrefFolder.FirstRun.didShowAdBlockOnboarding.set(sharedPreferenceModel, true)
        shouldShowOnboardingPopover.value = false
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
    ),
    cookieNoticeBlocked: Boolean = false,
    easyListRuleBlocked: Boolean = false

) : ContentFilterPopoverModel {
    override val trackingDataFlow: StateFlow<TrackingData?> = MutableStateFlow(trackingData)
    override val cookieNoticeBlocked = MutableStateFlow(cookieNoticeBlocked)
    override val easyListRuleBlocked = MutableStateFlow(easyListRuleBlocked)
    override val trackersAllowList = PreviewTrackersAllowList()
    override val popoverVisible = mutableStateOf(false)
    override val shouldShowOnboardingPopover = mutableStateOf(false)
    override val urlFlow: StateFlow<Uri> = MutableStateFlow(Uri.parse("www.neeva.com"))
    override val onReloadTab: () -> Unit = { }

    override fun openContentFilterSettings() {}
    override fun openPopover() {}
    override fun dismissPopover() {}
    override fun openOnboardingPopover() {}
    override fun dismissOnboardingPopover() {}
}

// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.contentfilter

import androidx.annotation.StringRes
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.neeva.app.Dispatchers
import com.neeva.app.R
import com.neeva.app.settings.SettingsDataModel
import com.neeva.app.settings.SettingsToggle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.chromium.weblayer.ContentFilterManager
import org.chromium.weblayer.ContentFilterMode

interface ContentFilterModel {
    val trackersAllowList: TrackersAllowList
    val trackingDataFlow: MutableStateFlow<TrackingData?>
    val cookieNoticeBlockedFlow: MutableStateFlow<Boolean>
    val enableTrackingProtection: MutableState<Boolean>
    val enableAdBlocking: MutableState<Boolean>
    val cookieCuttingPreferences: State<Set<CookieNoticeCookies>>

    fun setUpTrackingProtection(manager: ContentFilterManager)
    fun updateTrackingProtectionConfiguration()

    enum class BlockingStrength(
        @StringRes val description: Int,
        @StringRes val title: Int,
        val mode: Int
    ) {
        TRACKER_COOKIE(
            R.string.blocking_strength_standard_description,
            R.string.blocking_strength_standard_title,
            ContentFilterMode.BLOCK_COOKIES
        ),
        TRACKER_REQUEST(
            R.string.blocking_strength_strict_description,
            R.string.blocking_strength_strict_title,
            ContentFilterMode.BLOCK_REQUESTS
        )
    }

    enum class CookieNoticeSelection(
        @StringRes val title: Int
    ) {
        DECLINE_COOKIES(
            R.string.cookie_notice_decline_title
        ),
        ACCEPT_COOKIES(
            R.string.cookie_notice_accept_title
        )
    }

    enum class CookieNoticeCookies(
        @StringRes val title: Int,
        @StringRes val description: Int
    ) {
        MARKETING(
            R.string.cookie_marketing_title,
            R.string.cookie_marketing_description
        ),
        ANALYTICS(
            R.string.cookie_analytics_title,
            R.string.cookie_analytics_description
        ),
        SOCIAL(
            R.string.cookie_social_title,
            R.string.cookie_social_description
        )
    }

    companion object {
        const val BLOCKING_STRENGTH_SHARED_PREF_KEY = "BLOCKING_STRENGTH"
        const val COOKIE_NOTICE_PREFERENCES_SHARED_PREF_KEY = "COOKIE_NOTICE_PREFERENCES"
    }
}

class ContentFilterModelImpl(
    override val trackersAllowList: TrackersAllowList,
    private val coroutineScope: CoroutineScope,
    private val dispatchers: Dispatchers,
    private val settingsDataModel: SettingsDataModel
) : ContentFilterModel {
    private lateinit var contentFilterManager: ContentFilterManager

    override val trackingDataFlow = MutableStateFlow<TrackingData?>(null)
    override val cookieNoticeBlockedFlow = MutableStateFlow(false)
    override val enableTrackingProtection = settingsDataModel
        .getToggleState(SettingsToggle.TRACKING_PROTECTION)
    override val enableAdBlocking = settingsDataModel
        .getToggleState(SettingsToggle.AD_BLOCKING)

    override val cookieCuttingPreferences = settingsDataModel.cookieNoticePreferences

    override fun setUpTrackingProtection(manager: ContentFilterManager) {
        contentFilterManager = manager

        trackersAllowList.setUpTrackingProtection(
            onAddHostExclusion = contentFilterManager::addHostExclusion,
            onRemoveHostExclusion = contentFilterManager::removeHostExclusion
        )

        updateTrackingProtectionConfiguration()
    }

    override fun updateTrackingProtectionConfiguration() {
        enableTrackingProtection.value =
            settingsDataModel.getSettingsToggleValue(SettingsToggle.TRACKING_PROTECTION)
        enableAdBlocking.value =
            settingsDataModel.getSettingsToggleValue(SettingsToggle.AD_BLOCKING)

        contentFilterManager.disableAllRulesFiles()

        if (enableTrackingProtection.value) {
            val blockingStrength = settingsDataModel.getContentFilterStrength()
            contentFilterManager.setMode(blockingStrength.mode)

            contentFilterManager.enableRulesFile("easyprivacy")
            if (enableAdBlocking.value &&
                blockingStrength == ContentFilterModel.BlockingStrength.TRACKER_REQUEST
            ) {
                contentFilterManager.enableRulesFile("easylist")
            }

            coroutineScope.launch {
                val allTrackingAllowedHosts = withContext(dispatchers.io) {
                    trackersAllowList.getAllHostsInList()
                }
                withContext(dispatchers.main) {
                    allTrackingAllowedHosts.forEach { hostInfo ->
                        contentFilterManager.addHostExclusion(hostInfo.host)
                    }
                    contentFilterManager.startFiltering()
                }
            }
        } else {
            contentFilterManager.stopFiltering()
        }
    }
}

class PreviewContentFilterModel : ContentFilterModel {
    override val trackersAllowList = PreviewTrackersAllowList()
    override val trackingDataFlow: MutableStateFlow<TrackingData?> = MutableStateFlow(null)
    override val cookieNoticeBlockedFlow = MutableStateFlow(false)
    override val enableTrackingProtection: MutableState<Boolean> = mutableStateOf(true)
    override val enableAdBlocking: MutableState<Boolean> = mutableStateOf(false)
    override val cookieCuttingPreferences =
        mutableStateOf(emptySet<ContentFilterModel.CookieNoticeCookies>())
    override fun setUpTrackingProtection(manager: ContentFilterManager) {}
    override fun updateTrackingProtectionConfiguration() {}
}

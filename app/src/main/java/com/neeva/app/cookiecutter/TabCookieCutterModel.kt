// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.cookiecutter

import android.net.Uri
import androidx.compose.runtime.State
import com.neeva.app.browsing.getActiveTabId
import com.neeva.app.cookiecutter.TrackingEntity.Companion.trackingEntityForHost
import com.neeva.app.publicsuffixlist.DomainProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.chromium.weblayer.Browser

data class TrackingData(
    val numTrackers: Int,
    val numDomains: Int,
    val trackingEntities: Map<TrackingEntity, Int>
) {
    companion object {
        fun create(stats: Map<String, Int>?, domainProvider: DomainProvider): TrackingData {
            val numTrackers: Int = stats?.values?.sum() ?: 0
            val numDomains: Int = stats?.keys?.count() ?: 0
            val trackingEntities: Map<TrackingEntity, Int> =
                mutableMapOf<TrackingEntity, Int>().apply {
                    stats?.forEach {
                        val trackingEntity = trackingEntityForHost(
                            domainProvider.getRegisteredDomain(Uri.parse(it.key))
                        )
                        if (trackingEntity != null) {
                            put(trackingEntity, (get(trackingEntity) ?: 0) + it.value)
                        }
                    }
                }

            return TrackingData(
                numTrackers = numTrackers,
                numDomains = numDomains,
                trackingEntities = trackingEntities
            )
        }
    }

    /** Return the top 3 tracking entity (e.g., Google, Facebook...) which has the most trackers */
    fun whoIsTrackingYouHosts(): Map<TrackingEntity, Int> {
        return this.trackingEntities
            .toList()
            .sortedByDescending { (_, value) -> value }
            .take(3)
            .toMap()
    }
}

class TabCookieCutterModel(
    private val browserFlow: StateFlow<Browser?>,
    private val cookieNoticeBlockedFlow: MutableStateFlow<Boolean>,
    private val enableCookieNoticeSuppression: State<Boolean>,
    private val tabId: String,
    private val trackersAllowList: TrackersAllowList,
    private val trackingDataFlow: MutableStateFlow<TrackingData?>,
    val domainProvider: DomainProvider
) {
    /** When true, the tab will be reloaded when it becomes active tab. */
    var reloadUponForeground = false

    var cookieNoticeBlocked = false
        set(value) {
            field = value

            // if we're active, i.e. have the rights to update the main flow directly, then do so
            if (browserFlow.getActiveTabId() == tabId) {
                cookieNoticeBlockedFlow.value = value
            }
        }

    private var stats: Map<String, Int>? = null
        set(value) {
            field = value

            if (browserFlow.getActiveTabId() == tabId) {
                trackingDataFlow.value = TrackingData.create(stats, domainProvider)
            }
        }

    suspend fun shouldInjectCookieEngine(host: String): Boolean {
        val trackersEnabled = trackersAllowList.getHostAllowsTrackers(host)

        // Only allow cookie notice suppression if it is enabled globally, and
        // if trackers are not enabled for that site. For the purposes of this flag,
        // we consider "cookie notices" a tracker.
        return enableCookieNoticeSuppression.value && !trackersEnabled
    }

    fun currentTrackingData(): TrackingData {
        return TrackingData.create(stats, domainProvider)
    }

    fun resetStat() {
        stats = null
        cookieNoticeBlocked = false
    }

    fun updateStats(stats: Map<String, Int>?) {
        this.stats = stats
    }
}

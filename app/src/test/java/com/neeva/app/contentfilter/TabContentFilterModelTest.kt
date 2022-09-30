// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.contentfilter

import androidx.compose.runtime.mutableStateOf
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neeva.app.BaseTest
import com.neeva.app.publicsuffixlist.DomainProviderImpl
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue

/**
 * Tests that the TabContentFilterModel updates the stats properly
 */
@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class TabContentFilterModelTest : BaseTest() {
    private lateinit var model: TabContentFilterModel
    private lateinit var domainProviderImpl: DomainProviderImpl
    private lateinit var cookieNoticeBlockedFlow: MutableStateFlow<Boolean>
    private lateinit var trackersAllowList: TrackersAllowList

    override fun setUp() {
        super.setUp()

        domainProviderImpl = DomainProviderImpl(RuntimeEnvironment.getApplication())
        cookieNoticeBlockedFlow = MutableStateFlow(false)

        trackersAllowList = mockk {
            coEvery { getHostAllowsTrackers(any()) } returns false
            coEvery { getHostAllowsTrackers("example.com") } returns true
        }

        model = TabContentFilterModel(
            browserFlow = MutableStateFlow(null),
            tabId = "tab guid 1",
            trackingDataFlow = MutableStateFlow(null),
            cookieNoticeBlockedFlow = cookieNoticeBlockedFlow,
            enableCookieNoticeSuppression = mutableStateOf(true),
            domainProvider = domainProviderImpl,
            trackersAllowList = trackersAllowList
        )
    }

    override fun tearDown() {
        super.tearDown()
    }

    @Test
    fun testCookieNoticesInModel() {
        // pretend we've blocked a cookie notice
        model.cookieNoticeBlocked = true

        // assert that the model's own state reflects this
        expectThat(model.cookieNoticeBlocked).isEqualTo(true)

        // but we're not the active tab, so make sure the state flow wasn't updated
        expectThat(cookieNoticeBlockedFlow.value).isEqualTo(false)
    }

    @Test
    fun testCookieSuppressionEnabledByHost() {
        runTest {
            // First, try a site that suppression should be enabled on.
            val suppressResult = model.shouldInjectCookieEngine("suppress.com")

            coVerify {
                trackersAllowList.getHostAllowsTrackers("suppress.com")
            }

            expectThat(suppressResult).isTrue()

            // Then, a site it should not be enabled on.
            val exampleResult = model.shouldInjectCookieEngine("example.com")

            coVerify {
                trackersAllowList.getHostAllowsTrackers("example.com")
            }

            expectThat(exampleResult).isFalse()
        }
    }

    @Test
    fun testTrackingStatsInModel() {
        model.updateStats(
            mapOf(
                "1emn.com" to 1,
                "accountkit.com" to 2,
                "ads-twitter.com" to 3
            )
        )
        val trackingData = model.currentTrackingData()
        expectThat(trackingData.numTrackers).isEqualTo(6)
        expectThat(trackingData.trackingEntities.size).isEqualTo(3)

        model.resetStat()
        val emptyTrackingData = model.currentTrackingData()
        expectThat(emptyTrackingData.numTrackers).isEqualTo(0)
        expectThat(emptyTrackingData.trackingEntities.size).isEqualTo(0)
    }
}

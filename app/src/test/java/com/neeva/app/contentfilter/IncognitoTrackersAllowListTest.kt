// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.contentfilter

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neeva.app.BaseTest
import com.neeva.app.CoroutineScopeRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import strikt.api.expectThat
import strikt.assertions.isFalse
import strikt.assertions.isTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class IncognitoTrackersAllowListTest : BaseTest() {
    @Rule
    @JvmField
    val coroutineScopeRule = CoroutineScopeRule()

    private lateinit var trackersAllowList: IncognitoTrackersAllowList

    @Mock private lateinit var onAddHostExclusion: (host: String) -> Unit
    @Mock private lateinit var onRemoveHostExclusion: (host: String) -> Unit
    @Mock private lateinit var onSuccess: () -> Unit

    override fun setUp() {
        super.setUp()
        trackersAllowList = IncognitoTrackersAllowList()
        trackersAllowList.setUpTrackingProtection(
            onAddHostExclusion = onAddHostExclusion,
            onRemoveHostExclusion = onRemoveHostExclusion
        )
    }

    @Test
    fun toggleHostInAllowList() {
        val host = "example.com"

        // The list starts empty, so toggling it should add the host as an exception.
        trackersAllowList.toggleHostInAllowList(host = host, onSuccess = onSuccess)
        verify(onAddHostExclusion).invoke(eq(host))
        verify(onRemoveHostExclusion, never()).invoke(any())
        verify(onSuccess).invoke()

        // Toggling it should turn it off.
        trackersAllowList.toggleHostInAllowList(host = host, onSuccess = onSuccess)
        verify(onAddHostExclusion).invoke(eq(host))
        verify(onRemoveHostExclusion).invoke(eq(host))
        verify(onSuccess, times(2)).invoke()

        // Toggling it should turn it back on.
        trackersAllowList.toggleHostInAllowList(host = host, onSuccess = onSuccess)
        verify(onAddHostExclusion, times(2)).invoke(eq(host))
        verify(onRemoveHostExclusion).invoke(eq(host))
        verify(onSuccess, times(3)).invoke()
    }

    @Test
    fun getAllowsTrackersFlow() {
        val host = "example.com"

        // Keep track of the current state of the Flow, which should trigger whenever the
        // list is updated.
        var isTrackingAllowed: Boolean? = null
        trackersAllowList.getHostAllowsTrackersFlow(host)
            .onEach { isTrackingAllowed = it }
            .launchIn(coroutineScopeRule.scope)

        coroutineScopeRule.scope.advanceUntilIdle()
        expectThat(isTrackingAllowed).isFalse()

        trackersAllowList.toggleHostInAllowList(host = host, onSuccess = onSuccess)
        coroutineScopeRule.scope.advanceUntilIdle()
        expectThat(isTrackingAllowed).isTrue()

        trackersAllowList.toggleHostInAllowList(host = host, onSuccess = onSuccess)
        coroutineScopeRule.scope.advanceUntilIdle()
        expectThat(isTrackingAllowed).isFalse()
    }
}

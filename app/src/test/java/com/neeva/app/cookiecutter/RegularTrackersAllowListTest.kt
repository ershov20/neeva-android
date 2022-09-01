// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.cookiecutter

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neeva.app.BaseTest
import com.neeva.app.CoroutineScopeRule
import com.neeva.app.Dispatchers
import com.neeva.app.storage.daos.HostInfoDao
import com.neeva.app.storage.entities.HostInfo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import strikt.api.expectThat
import strikt.assertions.isFalse
import strikt.assertions.isTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class RegularTrackersAllowListTest : BaseTest() {
    @Rule
    @JvmField
    val coroutineScopeRule = CoroutineScopeRule()

    private lateinit var dispatchers: Dispatchers
    private lateinit var regularTrackersAllowList: RegularTrackersAllowList

    @Mock private lateinit var hostInfoDao: HostInfoDao
    @Mock private lateinit var onAddHostExclusion: (host: String) -> Unit
    @Mock private lateinit var onRemoveHostExclusion: (host: String) -> Unit
    @Mock private lateinit var onSuccess: () -> Unit

    override fun setUp() {
        super.setUp()

        dispatchers = Dispatchers(
            main = StandardTestDispatcher(coroutineScopeRule.scope.testScheduler),
            io = StandardTestDispatcher(coroutineScopeRule.scope.testScheduler),
        )

        regularTrackersAllowList = RegularTrackersAllowList(
            hostInfoDao = hostInfoDao,
            coroutineScope = coroutineScopeRule.scope,
            dispatchers = dispatchers
        )

        regularTrackersAllowList.setUpTrackingProtection(
            onAddHostExclusion = onAddHostExclusion,
            onRemoveHostExclusion = onRemoveHostExclusion
        )
    }

    @Test
    fun toggleHostInAllowList_whenHostNotPresent_addsHost() {
        val host = "example.com"
        runBlocking {
            Mockito.`when`(hostInfoDao.toggleTrackingAllowedForHost(eq(host))).thenReturn(false)
        }

        val wasJobStarted = regularTrackersAllowList.toggleHostInAllowList(
            host = host,
            onSuccess = onSuccess
        )
        coroutineScopeRule.scope.advanceUntilIdle()

        expectThat(wasJobStarted).isTrue()
        verify(onAddHostExclusion).invoke(eq(host))
        verify(onRemoveHostExclusion, never()).invoke(any())
        verify(onSuccess).invoke()
    }

    @Test
    fun toggleHostInAllowList_whenHostPresent_removesHost() {
        val host = "example.com"
        runBlocking {
            Mockito.`when`(hostInfoDao.toggleTrackingAllowedForHost(eq(host))).thenReturn(true)
        }

        val wasJobStarted = regularTrackersAllowList.toggleHostInAllowList(
            host = host,
            onSuccess = onSuccess
        )
        coroutineScopeRule.scope.advanceUntilIdle()

        expectThat(wasJobStarted).isTrue()
        verify(onAddHostExclusion, never()).invoke(eq(host))
        verify(onRemoveHostExclusion).invoke(any())
        verify(onSuccess).invoke()
    }

    @Test
    fun toggleHostInAllowList_whenJobAlreadyRunning_doesNothing() {
        val host = "example.com"
        runBlocking {
            Mockito.`when`(hostInfoDao.toggleTrackingAllowedForHost(eq(host))).thenReturn(true)
        }

        val firstSuccessLambda: () -> Unit = mock()
        val secondSuccessLambda: () -> Unit = mock()
        val wasFirstJobStarted = regularTrackersAllowList.toggleHostInAllowList(
            host = host,
            onSuccess = firstSuccessLambda
        )
        val wasSecondJobStarted = regularTrackersAllowList.toggleHostInAllowList(
            host = host,
            onSuccess = secondSuccessLambda
        )
        coroutineScopeRule.scope.advanceUntilIdle()

        expectThat(wasFirstJobStarted).isTrue()
        expectThat(wasSecondJobStarted).isFalse()
        verify(firstSuccessLambda).invoke()
        verify(secondSuccessLambda, never()).invoke()
    }

    @Test
    fun getAllowsTrackersFlow() {
        val host = "example.com"

        val hostNameFlow = MutableStateFlow<HostInfo?>(null)
        Mockito.`when`(hostInfoDao.getHostInfoByNameFlow(eq(host))).thenReturn(hostNameFlow)

        // Keep track of the current state of the Flow, which should trigger whenever the
        // hostNameFlow updates.
        var isTrackingAllowed: Boolean? = null
        regularTrackersAllowList.getHostAllowsTrackersFlow(host)
            .onEach { isTrackingAllowed = it }
            .launchIn(coroutineScopeRule.scope)

        // Check that a null value defaults to false.
        hostNameFlow.value = null
        coroutineScopeRule.scope.advanceUntilIdle()
        expectThat(isTrackingAllowed).isFalse()

        hostNameFlow.value = HostInfo(host = host, isTrackingAllowed = true)
        coroutineScopeRule.scope.advanceUntilIdle()
        expectThat(isTrackingAllowed).isTrue()

        hostNameFlow.value = HostInfo(host = host, isTrackingAllowed = false)
        coroutineScopeRule.scope.advanceUntilIdle()
        expectThat(isTrackingAllowed).isFalse()
    }
}

// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.billing

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neeva.app.BaseTest
import com.neeva.app.CoroutineScopeRule
import com.neeva.app.apollo.AuthenticatedApolloWrapper
import com.neeva.app.billing.billingclient.BillingClientController
import com.neeva.app.billing.billingclient.BillingClientWrapper
import com.neeva.app.settings.SettingsDataModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class BillingClientControllerTest : BaseTest() {
    @Rule
    @JvmField
    val coroutineScopeRule = CoroutineScopeRule()

    @Mock private lateinit var authenticatedApolloWrapper: AuthenticatedApolloWrapper
    @Mock private lateinit var settingsDataModel: SettingsDataModel

    @MockK private lateinit var billingClientWrapper: BillingClientWrapper

    private class Jobs {
        fun job1() { }
        fun job2() { }
        fun job3() { }
    }
    private lateinit var jobs: Jobs

    private lateinit var billingClientController: BillingClientController

    override fun setUp() {
        super.setUp()
        billingClientWrapper = mockk {
            coEvery { retryConnection() } answers { true }
            coEvery { setUpPurchasesUpdatedListener(any()) } answers { }
        }

        billingClientController = BillingClientController(
            authenticatedApolloWrapper = authenticatedApolloWrapper,
            billingClientWrapper = billingClientWrapper,
            coroutineScope = coroutineScopeRule.scope,
            dispatchers = coroutineScopeRule.dispatchers,
            settingsDataModel = settingsDataModel
        )
        jobs = spyk(Jobs())
    }

    @Test
    fun queueJobWithRetry_multipleJobs_processesThemInQueueOrder() {
        billingClientController.queueJobWithRetry("job 1") {
            jobs.job1()
            true
        }
        billingClientController.queueJobWithRetry("job 2") {
            jobs.job2()
            true
        }
        billingClientController.queueJobWithRetry("job 3") {
            jobs.job3()
            true
        }

        coroutineScopeRule.scope.advanceUntilIdle()

        coVerifyOrder {
            jobs.job1()
            jobs.job2()
            jobs.job3()
        }
        verify(exactly = 1) { jobs.job1() }
        verify(exactly = 1) { jobs.job2() }
        verify(exactly = 1) { jobs.job3() }
    }

    @Test
    fun queueJobWithRetry_oneJobFailsEveryTime_processInOrderAndMovesOn() {
        billingClientController.queueJobWithRetry("job 1") {
            jobs.job1()
            true
        }
        billingClientController.queueJobWithRetry("job 2") {
            jobs.job2()
            false
        }
        billingClientController.queueJobWithRetry("job 3") {
            jobs.job3()
            true
        }

        coroutineScopeRule.scope.advanceUntilIdle()
        coVerifyOrder {
            jobs.job1()
            jobs.job2()
            jobs.job3()
        }
        verify(exactly = 1) { jobs.job1() }
        verify(exactly = billingClientController.MAX_ATTEMPTS) { jobs.job2() }
        coVerify(exactly = billingClientController.MAX_ATTEMPTS - 1) {
            billingClientWrapper.retryConnection()
        }
        verify(exactly = 1) { jobs.job3() }
    }

    @Test
    fun queueJobWithRetry_sameJobNamesQueued_onlyProcessesUniqueJobs() {
        billingClientController.queueJobWithRetry("job 1") {
            jobs.job1()
            true
        }
        billingClientController.queueJobWithRetry("job 2") {
            jobs.job2()
            true
        }
        billingClientController.queueJobWithRetry("job 3") {
            jobs.job3()
            true
        }
        // Queue tasks with the same name
        billingClientController.queueJobWithRetry("job 2") {
            jobs.job2()
            true
        }
        billingClientController.queueJobWithRetry("job 2") {
            jobs.job2()
            true
        }

        coroutineScopeRule.scope.advanceUntilIdle()

        coVerifyOrder {
            jobs.job1()
            jobs.job2()
            jobs.job3()
        }
        verify(exactly = 1) { jobs.job1() }
        verify(exactly = 1) { jobs.job2() }
        verify(exactly = 1) { jobs.job3() }
    }

    @Test
    fun queueJobWithRetry_taskFinishedAndRequeued_isAbleToBeRequeued() {
        billingClientController.queueJobWithRetry("job 1") {
            jobs.job1()
            true
        }

        coroutineScopeRule.scope.advanceUntilIdle()
        verify(exactly = 1) { jobs.job1() }

        billingClientController.queueJobWithRetry("job 1") {
            jobs.job1()
            true
        }

        coroutineScopeRule.scope.advanceUntilIdle()

        verify(exactly = 2) { jobs.job1() }
    }
}

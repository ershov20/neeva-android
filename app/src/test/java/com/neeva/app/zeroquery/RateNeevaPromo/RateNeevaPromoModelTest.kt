// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.zeroquery.RateNeevaPromo

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neeva.app.BaseTest
import com.neeva.app.appnav.AppNavModel
import com.neeva.app.sharedprefs.SharedPrefFolder
import com.neeva.app.sharedprefs.SharedPreferencesModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class RateNeevaPromoModelTest : BaseTest() {

    lateinit var sharedPreferencesModel: SharedPreferencesModel
    lateinit var rateNeevaPromoModel: RateNeevaPromoModel

    override fun setUp() {
        super.setUp()

        val context: Context = ApplicationProvider.getApplicationContext()
        sharedPreferencesModel = SharedPreferencesModel(context)
        rateNeevaPromoModel = RateNeevaPromoModel(sharedPreferencesModel)
    }

    @Test
    fun incrementLaunches_increases_the_launch_count_and_stores_it() {
        val before = SharedPrefFolder.App.PostFirstRunLaunchCount.get(sharedPreferencesModel)

        rateNeevaPromoModel.incrementLaunches()

        val after = SharedPrefFolder.App.PostFirstRunLaunchCount.get(sharedPreferencesModel)

        expectThat(after - before).isEqualTo(1)
    }

    @Test
    fun incrementLaunches_does_not_increment_past_MAX_VALUE() {
        SharedPrefFolder.App.PostFirstRunLaunchCount.set(
            sharedPreferencesModel,
            Int.MAX_VALUE,
            true
        )

        rateNeevaPromoModel.incrementLaunches()

        expectThat(SharedPrefFolder.App.PostFirstRunLaunchCount.get(sharedPreferencesModel))
            .isEqualTo(Int.MAX_VALUE)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun shouldShowRateNeevaPromo_is_true_initially() {
        runTest {
            expectThat(rateNeevaPromoModel.shouldShowRateNeevaPromo.first()).isTrue()
        }
    }

    @Test
    fun dismiss_temporally_sets_shouldShowRateNeevaPromo_to_false() {
        rateNeevaPromoModel.dismiss()
        runTest {
            expectThat(rateNeevaPromoModel.shouldShowRateNeevaPromo.first()).isFalse()
        }
    }

    @Test
    fun after_7_launches_shouldShowRateNeevaPromo_is_false() {
        repeat(7) { rateNeevaPromoModel.incrementLaunches() }
        runTest {
            expectThat(rateNeevaPromoModel.shouldShowRateNeevaPromo.first()).isFalse()
        }
    }

    @Test
    fun suppress_sets_launch_count_to_max_value() {
        val appNav: AppNavModel = mockk()
        every { appNav.showFeedback() } returns Unit

        rateNeevaPromoModel.giveFeedback(appNav)

        expectThat(SharedPrefFolder.App.PostFirstRunLaunchCount.get(sharedPreferencesModel))
            .isEqualTo(Int.MAX_VALUE)
    }
}

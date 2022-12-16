// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.zeroquery.RateNeevaPromo

import android.net.Uri
import com.neeva.app.appnav.AppNavModel
import com.neeva.app.sharedprefs.SharedPrefFolder
import com.neeva.app.sharedprefs.SharedPreferencesModel
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine

@Singleton
class RateNeevaPromoModel @Inject constructor(
    val sharedPreferencesModel: SharedPreferencesModel,
) {
    private val launchCount = SharedPrefFolder.App.PostFirstRunLaunchCount
        .getFlow(sharedPreferencesModel)

    private val dismissed = MutableStateFlow(false)

    val shouldShowRateNeevaPromo = dismissed.combine(launchCount) { dismissed, launchCount ->
        !dismissed && launchCount <= 6
    }

    fun incrementLaunches() {
        launchCount.value.let {
            if (it < Int.MAX_VALUE) {
                SharedPrefFolder.App.PostFirstRunLaunchCount.set(
                    sharedPreferencesModel,
                    it + 1
                )
            }
        }
    }

    fun dismiss() {
        dismissed.value = true
    }

    fun launchAppStore(appNavModel: AppNavModel) {
        val appPackageName = "com.neeva.app"
        suppress()
        appNavModel.openUrlViaIntent(
            Uri.parse("market://details?id=$appPackageName"),
            fallback = Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName")
        )
    }

    fun giveFeedback(appNavModel: AppNavModel) {
        suppress()
        appNavModel.showFeedback()
    }

    private fun suppress() {
        SharedPrefFolder.App.PostFirstRunLaunchCount.set(
            sharedPreferencesModel,
            Int.MAX_VALUE,
            true
        )
    }
}

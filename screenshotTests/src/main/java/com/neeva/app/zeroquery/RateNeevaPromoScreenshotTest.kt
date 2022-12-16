// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.zeroquery

import com.neeva.app.BaseScreenshotTest
import com.neeva.app.zeroquery.RateNeevaPromo.Preview_RateNeevaPromo_GaugeSentiment
import com.neeva.app.zeroquery.RateNeevaPromo.Preview_RateNeevaPromo_feedback
import com.neeva.app.zeroquery.RateNeevaPromo.Preview_RateNeevaPromo_landscape_GaugeSentiment
import com.neeva.app.zeroquery.RateNeevaPromo.Preview_RateNeevaPromo_landscape_feedback
import com.neeva.app.zeroquery.RateNeevaPromo.Preview_RateNeevaPromo_landscape_rate
import com.neeva.app.zeroquery.RateNeevaPromo.Preview_RateNeevaPromo_rate
import org.junit.Test

class RateNeevaPromoScreenshotTest : BaseScreenshotTest() {
    @Test
    fun RateNeevaPromo_StateGagueSentiment() = runScreenshotTest {
        Preview_RateNeevaPromo_GaugeSentiment()
    }

    @Test
    fun RateNeevaPromo_StateFeedback() = runScreenshotTest {
        Preview_RateNeevaPromo_feedback()
    }

    @Test
    fun RateNeevaPromo_StateRate() = runScreenshotTest {
        Preview_RateNeevaPromo_rate()
    }

    @Test
    fun RateNeevaPromo_StateGagueSentiment_Landscape() = runScreenshotTest {
        Preview_RateNeevaPromo_landscape_GaugeSentiment()
    }

    @Test
    fun RateNeevaPromo_StateFeedback_Landscape() = runScreenshotTest {
        Preview_RateNeevaPromo_landscape_feedback()
    }

    @Test
    fun RateNeevaPromo_StateRate_Landscape() = runScreenshotTest {
        Preview_RateNeevaPromo_landscape_rate()
    }
}

// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.zeroquery.RateNeevaPromo

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.neeva.app.LocalAppNavModel
import com.neeva.app.LocalRateNeevaPromoModel
import com.neeva.app.ui.theme.Dimensions

private enum class State {
    GaugeSentiment,
    Feedback,
    Rate,
    Hidden,
}

@Composable
fun RateNeevaPromo() {
    var state by rememberSaveable { mutableStateOf(State.GaugeSentiment) }
    val model = LocalRateNeevaPromoModel.current
    val appNav = LocalAppNavModel.current

    Card(
        modifier = Modifier
            .padding(Dimensions.PADDING_LARGE)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        when (state) {
            State.GaugeSentiment -> GaugeSentimentPrompt(
                onTapPositive = { state = State.Rate },
                onTapNegative = { state = State.Feedback }
            )
            State.Rate -> RatePrompt(
                onTapRate = { model.launchAppStore(appNav) },
                onTapLater = { model.dismiss() }
            )
            State.Feedback -> FeedbackPrompt(
                onTapFeedback = { model.giveFeedback(appNav) },
                onTapLater = { model.dismiss() }
            )
            State.Hidden -> {}
        }
    }
}

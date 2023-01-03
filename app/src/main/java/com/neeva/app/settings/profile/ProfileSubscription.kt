// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.settings.profile

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.neeva.app.type.SubscriptionType
import com.neeva.app.ui.theme.ColorPalette
import com.neeva.app.ui.theme.Dimensions

@Composable
fun ProfileSubscription(subscriptionType: SubscriptionType?) {
    val color = when (subscriptionType) {
        SubscriptionType.Basic -> ColorPalette.Brand.Polar
        else -> ColorPalette.Brand.OffwhiteVariant
    }
    Surface(color = color, shape = RoundedCornerShape(Dimensions.RADIUS_TINY)) {
        Text(
            text = GetSubscriptionString(subscriptionType),
            style = MaterialTheme.typography.titleSmall,
            color = Color.Black,
            modifier = Modifier.padding(
                vertical = Dimensions.PADDING_TINY,
                horizontal = Dimensions.PADDING_MEDIUM
            )
        )
    }
}

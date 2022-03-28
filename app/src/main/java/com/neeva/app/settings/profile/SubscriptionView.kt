package com.neeva.app.settings.profile

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.neeva.app.R
import com.neeva.app.type.SubscriptionType
import com.neeva.app.ui.theme.ColorPalette
import com.neeva.app.ui.theme.Dimensions

@Composable
fun SubscriptionView(subscriptionType: SubscriptionType?) {
    val (subscriptionString, color) = when (subscriptionType) {
        SubscriptionType.Basic ->
            Pair(stringResource(id = R.string.subscription_basic), ColorPalette.Brand.Polar)

        SubscriptionType.Premium ->
            Pair(
                stringResource(id = R.string.subscription_premium),
                ColorPalette.Brand.OffwhiteVariant
            )

        else ->
            Pair(stringResource(id = R.string.subscription_unknown), ColorPalette.Brand.Red)
    }
    if (subscriptionType != null) {
        Surface(
            shape = RoundedCornerShape(Dimensions.RADIUS_TINY),
            color = color
        ) {
            Text(
                text = subscriptionString,
                style = MaterialTheme.typography.titleSmall,
                color = ColorPalette.Brand.Charcoal,
                modifier = Modifier.padding(Dimensions.PADDING_TINY)
            )
        }
    }
}

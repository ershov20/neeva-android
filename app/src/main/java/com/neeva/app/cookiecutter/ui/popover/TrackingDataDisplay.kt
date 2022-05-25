package com.neeva.app.cookiecutter.ui.popover

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandIn
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.neeva.app.R
import com.neeva.app.ui.theme.Dimensions

@Composable
fun TrackingDataDisplay(visible: Boolean, cookieCutterPopoverModel: CookieCutterPopoverModel) {
    val trackingData = cookieCutterPopoverModel.trackingDataFlow?.collectAsState()?.value

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically() + expandVertically(),
        exit = fadeOut() + slideOutVertically() + shrinkVertically()
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row {
                TrackingDataNumberBox(
                    label = stringResource(id = R.string.cookie_cutter_trackers),
                    value = trackingData?.numTrackers ?: 0,
                    modifier = Modifier.weight(1f)
                )

                Spacer(Modifier.size(Dimensions.PADDING_SMALL))

                TrackingDataNumberBox(
                    label = stringResource(id = R.string.cookie_cutter_domains),
                    value = trackingData?.numDomains ?: 0,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.size(Dimensions.PADDING_MEDIUM))

            AnimatedVisibility(
                visible = trackingData?.trackingEntities?.isNotEmpty() == true,
                enter = fadeIn() + expandIn(expandFrom = Alignment.TopCenter),
                exit = fadeOut() + shrinkOut(shrinkTowards = Alignment.TopCenter)
            ) {
                TrackingDataEntityGroup(trackingEntities = trackingData?.trackingEntities)
            }

            Spacer(Modifier.size(Dimensions.PADDING_MEDIUM))
        }
    }
}

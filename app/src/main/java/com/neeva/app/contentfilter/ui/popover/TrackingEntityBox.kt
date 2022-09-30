// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.contentfilter.ui.popover

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.accompanist.flowlayout.FlowCrossAxisAlignment
import com.google.accompanist.flowlayout.FlowRow
import com.google.accompanist.flowlayout.MainAxisAlignment
import com.neeva.app.R
import com.neeva.app.contentfilter.TrackingEntity
import com.neeva.app.ui.LightDarkPreviewContainer
import com.neeva.app.ui.PortraitPreviews
import com.neeva.app.ui.theme.Dimensions

@Composable
internal fun TrackingEntityBox(trackingEntities: Map<TrackingEntity, Int>?) {
    val topTrackingEntities = remember(trackingEntities) {
        derivedStateOf {
            (trackingEntities ?: mapOf())
                .toList()
                .sortedByDescending { (_, trackers) -> trackers }
                .take(3)
        }
    }

    TrackingDataBox(label = stringResource(id = R.string.content_filter_whos_tracking_you)) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Dimensions.PADDING_SMALL),
            mainAxisAlignment = MainAxisAlignment.SpaceBetween,
            mainAxisSpacing = Dimensions.PADDING_LARGE,
            crossAxisAlignment = FlowCrossAxisAlignment.Center,
            crossAxisSpacing = Dimensions.PADDING_SMALL
        ) {
            topTrackingEntities.value.forEach {
                val trackingEntity = it.first
                val numTrackers = it.second

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.height(IntrinsicSize.Min)
                ) {
                    Image(
                        painter = painterResource(id = trackingEntity.imageId),
                        contentDescription = trackingEntity.description,
                        contentScale = ContentScale.Fit
                    )

                    Spacer(Modifier.size(Dimensions.PADDING_SMALL))

                    Text(
                        text = numTrackers.toString(),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }

            // Add a zero-width item to force FlowRow to divide the free space evenly between the
            // real items when using MainAxisAlignment.SpaceBetween.  This isn't equivalent to using
            // a Row with equally-weighted items, but it's a reasonable compromise for ensuring that
            // the text doesn't wrap in a weird way if there isn't enough room to display everything
            // in a single line without having to resort to a custom Composable.
            Spacer(Modifier.size(0.dp))
        }
    }
}

@PortraitPreviews
@Composable
fun TrackingEntityBoxPreview_Partial() {
    LightDarkPreviewContainer {
        TrackingEntityBox(
            trackingEntities = mapOf(
                TrackingEntity.GOOGLE to 5,
                TrackingEntity.FACEBOOK to 500
            )
        )
    }
}

@PortraitPreviews
@Composable
fun TrackingEntityBoxPreview_Full() {
    LightDarkPreviewContainer {
        TrackingEntityBox(
            trackingEntities = mapOf(
                TrackingEntity.GOOGLE to 500,
                TrackingEntity.FACEBOOK to 500,
                TrackingEntity.AMAZON to 500
            )
        )
    }
}

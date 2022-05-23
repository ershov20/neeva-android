package com.neeva.app.cookiecutter.ui.popover

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.neeva.app.R
import com.neeva.app.cookiecutter.TrackingEntity
import com.neeva.app.ui.theme.Dimensions

@Composable
fun TrackingDataNumberBox(
    label: String,
    value: Int,
    modifier: Modifier = Modifier
) {
    TrackingDataBox(label = label, modifier = modifier) {
        TrackingDataNumber(value)
    }
}

@Composable
private fun TrackingDataNumber(value: Int, modifier: Modifier = Modifier) {
    Text(
        text = value.toString(),
        color = MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.titleLarge,
        modifier = modifier
    )
}

@Composable
private fun TrackingDataBox(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    TrackingDataSurface(modifier) {
        Column(
            Modifier.padding(
                horizontal = Dimensions.PADDING_LARGE,
                vertical = Dimensions.PADDING_MEDIUM
            )
        ) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.outline,
                style = MaterialTheme.typography.titleMedium
            )
            content()
        }
    }
}

@Composable
internal fun TrackingDataSurface(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12))
    ) {
        content()
    }
}

@Composable
internal fun TrackingDataEntityGroup(trackingEntities: Map<TrackingEntity, Int>?) {
    val topTrackingEntities = remember(trackingEntities) {
        (trackingEntities ?: mapOf())
            .toList()
            .sortedByDescending { (_, trackers) -> trackers }
            .take(3)
            .toMap()
    }

    TrackingDataBox(label = stringResource(id = R.string.cookie_cutter_whos_tracking_you)) {
        Spacer(Modifier.size(Dimensions.PADDING_SMALL))
        // TODO(kobec): Figure out a way to handle this for when you run out of screen real-estate. See 2x font preview...
        Row {
            topTrackingEntities.forEach { (trackingEntity, trackers) ->
                TrackingDataEntityView(
                    trackingEntity = trackingEntity,
                    trackers = trackers,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun TrackingDataEntityView(
    trackingEntity: TrackingEntity,
    trackers: Int,
    modifier: Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.height(IntrinsicSize.Min)
    ) {
        Image(
            painter = painterResource(id = trackingEntity.imageId),
            contentDescription = trackingEntity.description,
            contentScale = ContentScale.Fit
        )

        Spacer(Modifier.size(Dimensions.PADDING_SMALL))

        TrackingDataNumber(value = trackers)
    }
}

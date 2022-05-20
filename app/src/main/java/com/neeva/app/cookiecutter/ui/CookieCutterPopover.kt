package com.neeva.app.cookiecutter.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.neeva.app.R
import com.neeva.app.cookiecutter.TrackingData
import com.neeva.app.cookiecutter.TrackingEntity
import com.neeva.app.settings.sharedComposables.subcomponents.SettingsNavigationRow
import com.neeva.app.ui.LightDarkPreviewContainer
import com.neeva.app.ui.NeevaSwitch
import com.neeva.app.ui.theme.Dimensions

@Composable
fun CookieCutterPopover(
    trackingData: TrackingData?,
    openCookieCutterSettings: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val topOffset = with(LocalDensity.current) {
        dimensionResource(id = R.dimen.top_toolbar_height).roundToPx()
    }
    // TODO(kobec): the offset does not work in landscape
    Popup(
        offset = IntOffset(x = 0, y = topOffset),
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true)
    ) {
        CookieCutterPopoverSurface(
            trackingData = trackingData,
            openCookieCutterSettings = openCookieCutterSettings,
            onDismissRequest = onDismissRequest,
            modifier = modifier
        )
    }
    BackHandler(onBack = onDismissRequest)
}

@Composable
private fun CookieCutterPopoverSurface(
    trackingData: TrackingData?,
    openCookieCutterSettings: () -> Unit,
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit
) {
    // TODO(kobec/chung): when designs come out: set it to the correct height
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(5)),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(Dimensions.PADDING_LARGE)
        ) {
            IconButton(onClick = onDismissRequest) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = stringResource(id = R.string.cookie_cutter_dismiss_popup)
                )
            }

            CookieCutterPopoverContent(
                trackingData = trackingData,
                openCookieCutterSettings = {
                    onDismissRequest()
                    openCookieCutterSettings()
                }
            )
        }
    }
}

@Composable
private fun CookieCutterPopoverContent(
    trackingData: TrackingData?,
    openCookieCutterSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row {
            TrackingDataBox(
                label = stringResource(id = R.string.cookie_cutter_trackers),
                modifier = Modifier.weight(1f)
            ) {
                TrackingDataContentText("${trackingData?.numTrackers ?: 0}")
            }

            Spacer(Modifier.size(Dimensions.PADDING_SMALL))

            TrackingDataBox(
                label = stringResource(id = R.string.cookie_cutter_domains),
                modifier = Modifier.weight(1f)
            ) {
                // TODO(kobec/chung): implement domains
                TrackingDataContentText("${trackingData?.numDomains ?: 0}")
            }
        }

        Spacer(Modifier.size(Dimensions.PADDING_MEDIUM))

        TrackingDataEntityGroup(trackingEntities = trackingData?.trackingEntities)

        Spacer(Modifier.size(Dimensions.PADDING_MEDIUM))

        TrackingDataSurface {
            Column {
                // TODO(kobec/chung): Map each hostname -> if it is enabled or not
                NeevaSwitch(
                    primaryLabel = stringResource(id = R.string.cookie_cutter),
                    isChecked = false,
                    onCheckedChange = {},
                    enabled = false
                )

                Divider()

                SettingsNavigationRow(
                    primaryLabel = stringResource(R.string.cookie_cutter_settings),
                    onClick = openCookieCutterSettings
                )
            }
        }
    }
}

@Preview("CookieCutterPopover 1x font scale", locale = "en")
@Preview("CookieCutterPopover 2x font scale", locale = "en", fontScale = 2.0f)
@Preview("CookieCutterPopover RTL, 1x font scale", locale = "he")
@Composable
private fun CookieCutterPopoverPreview() {
    LightDarkPreviewContainer {
        val trackingData = TrackingData(
            numTrackers = 114,
            numDomains = 88,
            trackingEntities = mapOf(
                TrackingEntity.GOOGLE to 500,
                TrackingEntity.AMAZON to 38,
                TrackingEntity.WARNERMEDIA to 4,
                TrackingEntity.CRITEO to 19
            )
        )
        CookieCutterPopoverSurface(
            trackingData = trackingData,
            openCookieCutterSettings = {},
            modifier = Modifier.height(400.dp)
        ) {}
    }
}

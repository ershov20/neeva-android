package com.neeva.app.cookiecutter.ui.popover

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.neeva.app.R
import com.neeva.app.settings.sharedComposables.subcomponents.SettingsNavigationRow
import com.neeva.app.ui.LightDarkPreviewContainer
import com.neeva.app.ui.theme.Dimensions

@Composable
fun CookieCutterPopover(
    cookieCutterPopoverModel: CookieCutterPopoverModel,
    modifier: Modifier = Modifier
) {
    // TODO(kobec): the offset does not work in landscape
    val topOffset = with(LocalDensity.current) {
        dimensionResource(id = R.dimen.top_toolbar_height).roundToPx()
    }
    Popup(
        offset = IntOffset(x = 0, y = topOffset),
        onDismissRequest = cookieCutterPopoverModel::dismissPopover,
        properties = PopupProperties(focusable = true)
    ) {
        Surface(
            shape = RoundedCornerShape(
                bottomStart = Dimensions.RADIUS_SMALL,
                bottomEnd = Dimensions.RADIUS_SMALL
            ),
            shadowElevation = 2.dp,
            modifier = modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(Dimensions.PADDING_LARGE)
            ) {
                CookieCutterPopoverContent(
                    cookieCutterPopoverModel = cookieCutterPopoverModel
                )
            }
        }
    }
}

@Composable
private fun CookieCutterPopoverContent(
    cookieCutterPopoverModel: CookieCutterPopoverModel,
    modifier: Modifier = Modifier
) {
    val hostFlow by cookieCutterPopoverModel.urlFlow.collectAsState()
    val host = hostFlow.host ?: ""

    // Default to saying that trackers are disallowed to avoid the TrackingDataDisplay UI appearing
    // and then disappearing immediately.
    val trackersAllowList = cookieCutterPopoverModel.trackersAllowList
    val allowsTrackersFlow by trackersAllowList.getHostAllowsTrackersFlow(host).collectAsState(true)
    val cookieCutterEnabled = !allowsTrackersFlow

    Column(modifier = modifier.fillMaxWidth()) {
        TrackingDataDisplay(
            visible = cookieCutterEnabled,
            cookieCutterPopoverModel = cookieCutterPopoverModel
        )
        // Cookie Cutter Popover Settings:
        TrackingDataSurface {
            Column {
                CookieCutterPopoverSwitch(
                    cookieCutterEnabled = cookieCutterEnabled,
                    host = host,
                    trackersAllowList = trackersAllowList,
                    onSuccess = { cookieCutterPopoverModel.onReloadTab() }
                )

                Divider()

                SettingsNavigationRow(
                    primaryLabel = stringResource(R.string.cookie_cutter_settings),
                    onClick = cookieCutterPopoverModel::openCookieCutterSettings
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
        CookieCutterPopover(
            cookieCutterPopoverModel = PreviewCookieCutterPopoverModel(),
            modifier = Modifier.height(400.dp)
        )
    }
}

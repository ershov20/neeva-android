package com.neeva.app.cookiecutter.ui.popover

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    isIncognito: Boolean,
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
                IconButton(onClick = cookieCutterPopoverModel::dismissPopover) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = stringResource(id = R.string.cookie_cutter_dismiss)
                    )
                }
                CookieCutterPopoverContent(
                    isIncognito = isIncognito,
                    cookieCutterPopoverModel = cookieCutterPopoverModel
                )
            }
        }
    }
}

@Composable
private fun CookieCutterPopoverContent(
    isIncognito: Boolean,
    cookieCutterPopoverModel: CookieCutterPopoverModel,
    modifier: Modifier = Modifier
) {
    val host = cookieCutterPopoverModel.urlFlow.collectAsState().value.host ?: ""
    // Needs to be a flow because when we attach the Browser to the Activity, this is no longer null
    val allowTrackersManager = cookieCutterPopoverModel
        .trackersAllowListFlow?.collectAsState()?.value

    if (allowTrackersManager != null) {
        // TODO(kobec): try to get this state as soon as the host changes
        // A value of null here denotes that we don't know yet if we allow trackers on this host.
        val allowsTrackers by allowTrackersManager.getAllowsTrackersFlow(host)
            .collectAsState(null)

        // Will start as false until proven true.
        // This avoid cases where we show TrackingDataDisplayUI for a split second and then hide it.
        val cookieCutterEnabled = remember(allowsTrackers) {
            mutableStateOf(!(allowsTrackers ?: true))
        }

        Column(modifier = modifier.fillMaxWidth()) {
            TrackingDataDisplay(
                visible = cookieCutterEnabled.value,
                cookieCutterPopoverModel = cookieCutterPopoverModel
            )
            // Cookie Cutter Popover Settings:
            TrackingDataSurface {
                Column {
                    CookieCutterPopoverSwitch(
                        isIncognito = isIncognito,
                        cookieCutterEnabled = cookieCutterEnabled.value,
                        host = host,
                        trackersAllowList = allowTrackersManager,
                        onSuccess = { isCookieCutterEnabled ->
                            cookieCutterEnabled.value = isCookieCutterEnabled
                            cookieCutterPopoverModel.onReloadTab()
                        }
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
}

@Preview("CookieCutterPopover 1x font scale", locale = "en")
@Preview("CookieCutterPopover 2x font scale", locale = "en", fontScale = 2.0f)
@Preview("CookieCutterPopover RTL, 1x font scale", locale = "he")
@Composable
private fun CookieCutterPopoverPreview() {
    LightDarkPreviewContainer {
        CookieCutterPopover(
            isIncognito = false,
            cookieCutterPopoverModel = PreviewCookieCutterPopoverModel(),
            modifier = Modifier.height(400.dp)
        )
    }
}

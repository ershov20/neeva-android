// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.contentfilter.ui.popover

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.neeva.app.R
import com.neeva.app.contentfilter.TrackersAllowList
import com.neeva.app.contentfilter.TrackingData
import com.neeva.app.contentfilter.TrackingEntity
import com.neeva.app.contentfilter.ui.icon.BadgeSize
import com.neeva.app.contentfilter.ui.icon.NumberBadge
import com.neeva.app.ui.LandscapePreviews
import com.neeva.app.ui.NeevaThemePreviewContainer
import com.neeva.app.ui.PortraitPreviews
import com.neeva.app.ui.PortraitPreviewsDark
import com.neeva.app.ui.theme.Dimensions

@Composable
fun ContentFilterOnboardingContent(
    contentFilterPopoverModel: ContentFilterPopoverModel,
    modifier: Modifier = Modifier
) {
    val hostFlow by contentFilterPopoverModel.urlFlow.collectAsState()
    val host = hostFlow.host ?: ""

    // Default to saying that trackers are disallowed to avoid the TrackingDataDisplay UI appearing
    // and then disappearing immediately.
    val trackersAllowList = contentFilterPopoverModel.trackersAllowList
    val allowsTrackersFlow by trackersAllowList
        .getHostAllowsTrackersFlow(host)
        .collectAsState(true)
    val isContentFilterEnabled = !allowsTrackersFlow

    ContentFilterOnboardingPopoverContent(
        host = host,
        isContentFilterEnabled = isContentFilterEnabled,
        trackersAllowList = trackersAllowList,
        contentFilterPopoverModel = contentFilterPopoverModel,
        modifier = modifier
    )
}

@Composable
private fun IconBadge(
    image: ImageVector,
    imageDescription: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface),
        modifier = modifier
            .widthIn(min = 14.dp)
    ) {
        Icon(
            imageVector = image,
            contentDescription = stringResource(imageDescription),
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
private fun ContentFilterOnboardingPopoverContent(
    host: String,
    isContentFilterEnabled: Boolean,
    trackersAllowList: TrackersAllowList,
    contentFilterPopoverModel: ContentFilterPopoverModel,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(Dimensions.RADIUS_SMALL),
        shadowElevation = 2.dp,
        modifier = modifier
            .widthIn(max = 480.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Column(
            modifier = Modifier
                .padding(Dimensions.PADDING_LARGE)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ConstraintLayout {
                val (trackingProtectionIcon, badge) = createRefs()
                val verticalMiddleGuideline = createGuidelineFromBottom(0.4f)

                Icon(
                    painter = painterResource(R.drawable.ic_shield),
                    contentDescription = stringResource(
                        R.string.content_filter_content_description
                    ),
                    modifier = Modifier
                        .size(55.dp)
                        .constrainAs(trackingProtectionIcon) {
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                            bottom.linkTo(parent.bottom)
                            top.linkTo(parent.top)
                        }
                )

                val horizontalMiddleGuideline = createGuidelineFromStart(0.72f)
                val constraintModifier = Modifier.constrainAs(badge) {
                    bottom.linkTo(verticalMiddleGuideline)
                    centerAround(horizontalMiddleGuideline)
                }
                if (!isContentFilterEnabled) {
                    IconBadge(
                        image = Icons.Filled.Remove,
                        imageDescription = R.string.first_run_ad_block_remove_badge,
                        modifier = constraintModifier
                    )
                } else if (contentFilterPopoverModel.easyListRuleBlocked.collectAsState().value) {
                    val trackingDataState = contentFilterPopoverModel
                        .trackingDataFlow.collectAsState()
                    NumberBadge(
                        number = trackingDataState.value?.numTrackers ?: 0,
                        badgeSize = BadgeSize.LARGE,
                        modifier = constraintModifier
                    )
                } else if (contentFilterPopoverModel.cookieNoticeBlocked.collectAsState().value) {
                    IconBadge(
                        image = Icons.Filled.Check,
                        imageDescription = R.string.first_run_ad_block_checkmark_badge,
                        modifier = constraintModifier
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimensions.PADDING_LARGE))

            val text = when {
                !isContentFilterEnabled -> R.string.content_filter_onboarding_disabled
                contentFilterPopoverModel.easyListRuleBlocked.collectAsState().value ->
                    R.string.first_ad_blocked
                contentFilterPopoverModel.cookieNoticeBlocked.collectAsState().value ->
                    R.string.first_cookie_notice_blocked
                else -> R.string.first_ad_blocked
            }

            Text(
                text = stringResource(id = text),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(Dimensions.PADDING_LARGE))

            ContentFilterPopoverSwitch(
                contentFilterEnabled = isContentFilterEnabled,
                host = host,
                subtitle = R.string.first_run_ad_block_switch_subtitle,
                trackersAllowList = trackersAllowList,
                onSuccess = {
                    contentFilterPopoverModel.onReloadTab()
                }
            )

            Spacer(modifier = Modifier.height(Dimensions.PADDING_LARGE))

            Button(
                onClick = { contentFilterPopoverModel.dismissOnboardingPopover() },
                modifier = Modifier
                    .defaultMinSize(minHeight = dimensionResource(R.dimen.min_touch_target_size))
                    .fillMaxWidth()
            ) {
                Text(
                    text = stringResource(id = R.string.first_run_ad_block_dismiss),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

private val previewContentFilterCookieNoticeOnboardingPopoverModel by lazy {
    PreviewContentFilterPopoverModel(
        trackingData = TrackingData(
            numTrackers = 999,
            trackingEntities = mapOf(
                TrackingEntity.GOOGLE to 500,
                TrackingEntity.AMAZON to 38
            )
        ),
        easyListRuleBlocked = false,
        cookieNoticeBlocked = true
    )
}

private val previewContentFilterAdBlockOnboardingPopoverModel by lazy {
    PreviewContentFilterPopoverModel(
        trackingData = TrackingData(
            numTrackers = 999,
            trackingEntities = mapOf(
                TrackingEntity.GOOGLE to 500,
                TrackingEntity.AMAZON to 38
            )
        ),
        easyListRuleBlocked = true,
        cookieNoticeBlocked = false
    )
}

@PortraitPreviews
@Composable
fun ContentFilterDisableOnboardingContentPreview_Light_Partial() {
    NeevaThemePreviewContainer(useDarkTheme = false) {
        ContentFilterOnboardingContent(
            contentFilterPopoverModel = previewContentFilterCookieNoticeOnboardingPopoverModel
        )
    }
}

@PortraitPreviews
@LandscapePreviews
@Composable
fun ContentFilterCookieNoticeOnboardingPopoverContentPreview_Light_Full() {
    NeevaThemePreviewContainer(useDarkTheme = false) {
        ContentFilterOnboardingPopoverContent(
            host = "unused",
            isContentFilterEnabled = true,
            trackersAllowList =
            previewContentFilterCookieNoticeOnboardingPopoverModel.trackersAllowList,
            contentFilterPopoverModel = previewContentFilterCookieNoticeOnboardingPopoverModel
        )
    }
}

@PortraitPreviewsDark
@Composable
fun ContentFilterCookieNoticeOnboardingPopoverContentPreview_Dark() {
    NeevaThemePreviewContainer(useDarkTheme = true) {
        ContentFilterOnboardingPopoverContent(
            host = "unused",
            isContentFilterEnabled = true,
            trackersAllowList =
            previewContentFilterCookieNoticeOnboardingPopoverModel.trackersAllowList,
            contentFilterPopoverModel = previewContentFilterCookieNoticeOnboardingPopoverModel
        )
    }
}

@PortraitPreviews
@LandscapePreviews
@Composable
fun ContentFilterAdBlockOnboardingPopoverContentPreview_Light_Full() {
    NeevaThemePreviewContainer(useDarkTheme = false) {
        ContentFilterOnboardingPopoverContent(
            host = "unused",
            isContentFilterEnabled = true,
            trackersAllowList = previewContentFilterAdBlockOnboardingPopoverModel.trackersAllowList,
            contentFilterPopoverModel = previewContentFilterAdBlockOnboardingPopoverModel
        )
    }
}

@PortraitPreviewsDark
@Composable
fun ContentFilterAdBlockOnboardingPopoverContentPreview_Dark() {
    NeevaThemePreviewContainer(useDarkTheme = true) {
        ContentFilterOnboardingPopoverContent(
            host = "unused",
            isContentFilterEnabled = true,
            trackersAllowList = previewContentFilterAdBlockOnboardingPopoverModel.trackersAllowList,
            contentFilterPopoverModel = previewContentFilterAdBlockOnboardingPopoverModel
        )
    }
}

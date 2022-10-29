// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.neevascope

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.neeva.app.R
import com.neeva.app.ui.LightDarkPreviewContainer
import com.neeva.app.ui.PortraitPreviews
import com.neeva.app.ui.theme.Dimensions
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay

private val TIP_SIZE = 8.dp

@Composable
fun NeevaScopeTooltip(
    neevaScopeModel: NeevaScopeModel,
    showRedditDot: MutableState<Boolean>,
    isLandscape: Boolean = false
) {
    val neevaScopeTooltipType by neevaScopeModel.neevaScopeTooltipTypeFlow.collectAsState(null)

    when (neevaScopeTooltipType) {
        NeevaScopePromoType.TRY_NEEVASCOPE -> {
            TryNeevaScopeTooltip(
                isLandscape = isLandscape,
                performPromoTransition = {
                    neevaScopeModel.performRedditPromoTransition(
                        NeevaScopeModel.PromoTransition.DISMISS_TOOLTIP
                    )
                }
            )
        }

        NeevaScopePromoType.TRY_UGC -> {
            NeevaScopeRedditTooltip(
                isLandscape = isLandscape,
                performPromoTransition = {
                    neevaScopeModel.performRedditPromoTransition(
                        NeevaScopeModel.PromoTransition.DISMISS_TOOLTIP
                    )
                    showRedditDot.value = neevaScopeModel.showRedditDot
                }
            )
        }
        else -> {}
    }
}

@Composable
fun TryNeevaScopeTooltip(
    isLandscape: Boolean = false,
    performPromoTransition: () -> Unit
) {
    Tooltip(
        showTooltip = remember { mutableStateOf(true) },
        isLandscape = isLandscape,
        performPromoTransition = performPromoTransition,
        horizontalPadding = Dimensions.PADDING_LARGE,
        verticalPadding = Dimensions.PADDING_LARGE,
        maxWidth = 220.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Dimensions.PADDING_TINY)) {
            Text(
                text = stringResource(id = R.string.neevascope_tooltip),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = stringResource(id = R.string.neevascope_intro_title),
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun NeevaScopeRedditTooltip(
    isLandscape: Boolean = false,
    performPromoTransition: () -> Unit
) {
    Tooltip(
        showTooltip = remember { mutableStateOf(true) },
        isLandscape = isLandscape,
        isRedditTooltip = true,
        performPromoTransition = performPromoTransition,
        horizontalPadding = Dimensions.PADDING_MEDIUM,
        verticalPadding = Dimensions.PADDING_SMALL,
        maxWidth = 240.dp
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Dimensions.PADDING_MEDIUM)) {
            Image(
                painter = painterResource(id = R.drawable.reddit),
                contentDescription = null,
                modifier = Modifier
                    .size(32.dp)
                    .align(CenterVertically)
            )

            Text(
                text = stringResource(id = R.string.neevascope_reddit_tooltip),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
fun Tooltip(
    showTooltip: MutableState<Boolean>,
    isLandscape: Boolean = false,
    isRedditTooltip: Boolean = false,
    performPromoTransition: (() -> Unit)? = null,
    horizontalPadding: Dp,
    verticalPadding: Dp,
    maxWidth: Dp,
    offset: IntOffset = IntOffset(0, 0),
    properties: PopupProperties = PopupProperties(focusable = true),
    content: @Composable ColumnScope.() -> Unit
) {
    val tooltipOffset = with(LocalDensity.current) {
        TIP_SIZE.roundToPx()
    }
    val endOffset = with(LocalDensity.current) {
        Dimensions.SIZE_ICON_TOOLBAR.roundToPx() * 2 + Dimensions.PADDING_SMALL.roundToPx() * 3
    }

    val popupPositionProvider = object : PopupPositionProvider {
        override fun calculatePosition(
            anchorBounds: IntRect,
            windowSize: IntSize,
            layoutDirection: LayoutDirection,
            popupContentSize: IntSize
        ): IntOffset {
            return offset.copy(
                x = if (isLandscape) {
                    anchorBounds.right - popupContentSize.width - endOffset
                } else {
                    (anchorBounds.width - popupContentSize.width) / 2
                },
                y = if (isLandscape) {
                    anchorBounds.bottom - tooltipOffset
                } else {
                    anchorBounds.top - popupContentSize.height + tooltipOffset / 2
                }
            )
        }
    }

    if (showTooltip.value) {
        val dismissLambda = {
            showTooltip.value = false
            if (performPromoTransition != null) performPromoTransition()
        }

        if (isRedditTooltip) {
            LaunchedEffect(showTooltip) {
                delay(TimeUnit.SECONDS.toMillis(4))
                dismissLambda()
            }
        }

        Popup(
            popupPositionProvider = popupPositionProvider,
            onDismissRequest = dismissLambda,
            properties = properties
        ) {
            TooltipContent(isLandscape, horizontalPadding, verticalPadding, maxWidth, content)
        }
    }
}

@Composable
fun TooltipContent(
    isLandscape: Boolean = false,
    horizontalPadding: Dp,
    verticalPadding: Dp,
    maxWidth: Dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isLandscape) {
            Column(
                modifier = Modifier
                    .background(
                        color = Color.Blue,
                        shape = GenericShape { size, _ ->
                            moveTo(size.width / 2, 0f)
                            lineTo(0f, size.height)
                            lineTo(size.width, size.height)
                        }
                    )
                    .size(
                        width = Dimensions.NEEVASCOPE_TIP_SIZE * 2,
                        height = Dimensions.NEEVASCOPE_TIP_SIZE
                    )
            ) {}
        }

        Column(
            modifier = Modifier
                .background(
                    // Always keep light mode color
                    color = Color.Blue,
                    shape = RoundedCornerShape(Dimensions.RADIUS_LARGE)
                )
                .padding(horizontal = horizontalPadding, vertical = verticalPadding)
                .widthIn(max = maxWidth),
            content = content
        )

        if (!isLandscape) {
            Column(
                modifier = Modifier
                    .background(
                        color = Color.Blue,
                        shape = GenericShape { size, _ ->
                            moveTo(size.width / 2, size.height)
                            lineTo(0f, 0f)
                            lineTo(size.width, 0f)
                        }
                    )
                    .size(
                        width = Dimensions.NEEVASCOPE_TIP_SIZE * 2,
                        height = Dimensions.NEEVASCOPE_TIP_SIZE
                    )
            ) {}
        }
    }
}

@PortraitPreviews
@Composable
fun TooltipContent_Preview() {
    LightDarkPreviewContainer {
        TryNeevaScopeTooltip(
            isLandscape = false,
            performPromoTransition = {}
        )
    }
}

@PortraitPreviews
@Composable
fun TooltipRedditContent_Preview() {
    LightDarkPreviewContainer {
        NeevaScopeRedditTooltip(
            isLandscape = false,
            performPromoTransition = {}
        )
    }
}

package com.neeva.app.neevascope

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
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

private val TIP_SIZE = 8.dp

@Composable
fun NeevaScopeTooltip(
    isLandscape: Boolean = false
) {
    Tooltip(
        showTooltip = remember { mutableStateOf(true) },
        isLandscape = isLandscape
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
fun Tooltip(
    showTooltip: MutableState<Boolean>,
    isLandscape: Boolean = false,
    offset: IntOffset = IntOffset(0, 0),
    properties: PopupProperties = PopupProperties(focusable = true),
    content: @Composable ColumnScope.() -> Unit
) {
    val bottomOffset = with(LocalDensity.current) {
        dimensionResource(id = R.dimen.bottom_toolbar_height).roundToPx() - TIP_SIZE.roundToPx() / 2
    }
    val topOffset = with(LocalDensity.current) {
        dimensionResource(id = R.dimen.top_toolbar_height).roundToPx() - TIP_SIZE.roundToPx()
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
                    windowSize.width - popupContentSize.width - endOffset
                } else {
                    (windowSize.width - popupContentSize.width) / 2
                },
                y = if (isLandscape) {
                    popupContentSize.height - topOffset
                } else {
                    windowSize.height - popupContentSize.height / 2 - bottomOffset
                }
            )
        }
    }

    if (showTooltip.value) {
        Popup(
            popupPositionProvider = popupPositionProvider,
            onDismissRequest = { showTooltip.value = false },
            properties = properties
        ) {
            TooltipContent(isLandscape, content)
        }
    }
}

@Composable
fun TooltipContent(
    isLandscape: Boolean = false,
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
                    .size(width = TIP_SIZE * 2, height = TIP_SIZE),
            ) {}
        }

        Column(
            modifier = Modifier
                .background(
                    // Always keep light mode color
                    color = Color.Blue,
                    shape = RoundedCornerShape(Dimensions.RADIUS_LARGE)
                )
                .padding(Dimensions.PADDING_LARGE)
                .widthIn(max = 220.dp),
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
                    .size(width = TIP_SIZE * 2, height = TIP_SIZE),
            ) {}
        }
    }
}

@PortraitPreviews
@Composable
fun TooltipContent_Preview() {
    LightDarkPreviewContainer {
        TooltipContent {
            Column(verticalArrangement = Arrangement.spacedBy(Dimensions.PADDING_TINY)) {
                Text(
                    text = stringResource(id = R.string.neevascope_tooltip),
                    // Always keep light mode color
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = stringResource(id = R.string.neevascope_intro_title),
                    // Always keep light mode color
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

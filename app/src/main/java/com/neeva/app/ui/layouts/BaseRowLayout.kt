package com.neeva.app.ui.layouts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.annotation.ExperimentalCoilApi
import com.neeva.app.ui.TwoBooleanPreviewContainer
import com.neeva.app.ui.theme.Dimensions

/**
 * Base skeleton for everything that can be displayed as a row in UI, including history items,
 * navigation suggestions, and settings.
 */
@OptIn(ExperimentalCoilApi::class)
@Composable
fun BaseRowLayout(
    modifier: Modifier = Modifier,
    onTapRow: (() -> Unit)? = null,
    onDoubleTapRow: (() -> Unit)? = null,
    onTapRowContentDescription: String? = null,
    startComposable: @Composable (() -> Unit)? = null,
    endComposable: @Composable (() -> Unit)? = null,
    endComposablePadding: Dp = Dimensions.PADDING_SMALL,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = contentColorFor(backgroundColor),
    applyVerticalPadding: Boolean = true,
    mainContent: @Composable () -> Unit
) {
    Surface(
        color = backgroundColor,
        contentColor = contentColor,
        modifier = modifier
            .then(
                when {
                    onTapRow == null && onDoubleTapRow == null -> {
                        Modifier
                    }

                    onTapRow != null && onDoubleTapRow == null -> {
                        Modifier.clickable(onClickLabel = onTapRowContentDescription) {
                            onTapRow.invoke()
                        }
                    }

                    else -> {
                        // It's not clear how to set the content description when you're using the
                        // pointerInput modifier because we'd want to describe each action
                        // individually.
                        Modifier.pointerInput(Unit) {
                            detectTapGestures(
                                onTap = onTapRow?.let { { it() } },
                                onDoubleTap = onDoubleTapRow?.let { { it() } }
                            )
                        }
                    }
                }
            )
            .defaultMinSize(minHeight = Dimensions.SIZE_TOUCH_TARGET)
            .fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = if (applyVerticalPadding) Dimensions.PADDING_SMALL else 0.dp)
        ) {
            startComposable?.let {
                Box(
                    modifier = Modifier
                        .defaultMinSize(Dimensions.SIZE_TOUCH_TARGET)
                        .padding(start = Dimensions.PADDING_LARGE),
                    contentAlignment = Alignment.Center
                ) {
                    it()
                }
            }

            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .padding(horizontal = Dimensions.PADDING_LARGE)
                    .weight(1.0f)
                    .defaultMinSize(minHeight = Dimensions.SIZE_TOUCH_TARGET)
            ) {
                mainContent()
            }

            endComposable?.let {
                Box(
                    modifier = Modifier
                        .defaultMinSize(Dimensions.SIZE_TOUCH_TARGET)
                        .padding(end = endComposablePadding),
                    contentAlignment = Alignment.Center
                ) {
                    it()
                }
            }
        }
    }
}

@Preview("BaseRowWidget, LTR", locale = "en")
@Preview("BaseRowWidget, RTL", locale = "he")
@Composable
fun BaseRowWidget_Preview() {
    TwoBooleanPreviewContainer { showStartComposable, showEndComposable ->
        val startComposable = @Composable {
            Box(
                modifier = Modifier
                    .background(Color.Red)
                    .size(48.dp)
            )
        }
        val endComposable = @Composable {
            Box(
                modifier = Modifier
                    .background(Color.Blue)
                    .size(48.dp)
            )
        }

        BaseRowLayout(
            onTapRow = {},
            startComposable = startComposable.takeIf { showStartComposable },
            endComposable = endComposable.takeIf { showEndComposable }
        ) {
            Box(
                modifier = Modifier
                    .background(Color.Green)
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
            )
        }
    }
}

package com.neeva.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.ui.theme.NeevaTheme

/**
 * Show a bunch of previews for the same Composable in a Column to reduce the number of previews has
 * to try rendering.
 *
 * Doesn't work well if the items are too tall because Android Studio previews have a height limit
 * and doesn't draw things that fall outside of that range.
 */
@Composable
fun PreviewContainer(
    numBools: Int,
    content: @Composable (BooleanArray) -> Unit
) {
    val values: Sequence<BooleanArray> = sequence {
        val setSize = 1 shl numBools
        for (bits in 0 until setSize) {
            val currentArray = BooleanArray(numBools)
            for (j in 0 until numBools) {
                currentArray[j] = bits and (1 shl j) != 0
            }
            yield(currentArray)
        }
    }

    Column(
        modifier = Modifier.wrapContentHeight(unbounded = true)
    ) {
        values.forEachIndexed { index, params ->
            // Alternate the backgrounds to make it easier to see how things are grouped together.
            val backgroundColor = if (index % 2 == 0) {
                Color(red = 255, green = 0, blue = 255)
            } else {
                Color(red = 225, green = 0, blue = 225)
            }

            Column(
                modifier = Modifier
                    .background(backgroundColor)
                    .wrapContentHeight(unbounded = true)
            ) {
                NeevaThemePreviewContainer(useDarkTheme = false) { content(params) }
                NeevaThemePreviewContainer(useDarkTheme = true) { content(params) }
            }
        }
    }
}

@Composable
fun OneBooleanPreviewContainer(content: @Composable (Boolean) -> Unit) {
    PreviewContainer(numBools = 1) { content(it[0]) }
}

@Composable
fun TwoBooleanPreviewContainer(content: @Composable (Boolean, Boolean) -> Unit) {
    PreviewContainer(numBools = 2) { content(it[0], it[1]) }
}

@Composable
fun LightDarkPreviewContainer(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .background(Color.Magenta)
            .wrapContentHeight(unbounded = true)
    ) {
        NeevaThemePreviewContainer(useDarkTheme = false) { content() }
        NeevaThemePreviewContainer(useDarkTheme = true) { content() }
    }
}

@Composable
fun NeevaThemePreviewContainer(useDarkTheme: Boolean, content: @Composable () -> Unit) {
    NeevaTheme(useDarkTheme = useDarkTheme) {
        Box(modifier = Modifier.padding(Dimensions.PADDING_SMALL)) {
            content()
        }
    }
}

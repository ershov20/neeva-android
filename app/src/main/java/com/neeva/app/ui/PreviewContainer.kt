package com.neeva.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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
    useDarkTheme: Boolean? = null,
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
        values.forEach { params ->
            Column(modifier = Modifier.wrapContentHeight(unbounded = true)) {
                if (useDarkTheme == null) {
                    NeevaThemePreviewContainer(useDarkTheme = false) { content(params) }
                    NeevaThemePreviewContainer(useDarkTheme = true) { content(params) }
                } else {
                    NeevaThemePreviewContainer(useDarkTheme = useDarkTheme) { content(params) }
                }
            }

            // Separate out the different Preview groups.
            Spacer(
                modifier = Modifier
                    .height(1.dp)
                    .fillMaxWidth()
                    .background(Color.White)
            )
        }
    }
}

@Composable
fun OneBooleanPreviewContainer(
    useDarkTheme: Boolean? = null,
    content: @Composable (Boolean) -> Unit
) {
    PreviewContainer(useDarkTheme = useDarkTheme, numBools = 1) { content(it[0]) }
}

@Composable
fun TwoBooleanPreviewContainer(
    useDarkTheme: Boolean? = null,
    content: @Composable (Boolean, Boolean) -> Unit
) {
    PreviewContainer(useDarkTheme = useDarkTheme, numBools = 2) { content(it[0], it[1]) }
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
        Box(
            modifier = Modifier
                .background(Color.Magenta)
                .padding(Dimensions.PADDING_SMALL)
                .background(MaterialTheme.colorScheme.background)
        ) {
            content()
        }
    }
}

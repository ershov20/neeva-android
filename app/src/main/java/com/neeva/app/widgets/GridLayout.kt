package com.neeva.app.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.neeva.app.ui.OneBooleanPreviewContainer
import com.neeva.app.ui.theme.Dimensions

@Composable
fun <T : Any> GridLayout(
    numColumns: Int,
    items: List<T>,
    itemContent: @Composable (T) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        for (rowStartIndex in items.indices step numColumns) {
            Row(
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier.padding(vertical = Dimensions.PADDING_LARGE)
            ) {
                for (itemIndex in rowStartIndex until rowStartIndex + numColumns) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.weight(1.0f)
                    ) {
                        items.getOrNull(itemIndex)?.let { itemContent(it) }
                    }
                }
            }
        }
    }
}

class GridLayoutPreviews {
    @Preview("LTR", locale = "en")
    @Preview("RTL", locale = "he")
    @Composable
    fun DefaultPreview() {
        OneBooleanPreviewContainer { showFullRows ->
            val numItems = if (showFullRows) {
                8
            } else {
                6
            }

            val items = mutableListOf(
                Color.Red,
                Color.Yellow,
                Color.Green,
                Color.Blue,
                Color.Magenta,
                Color.DarkGray,
                Color.Cyan,
                Color.LightGray
            )

            Surface {
                GridLayout(numColumns = 4, items = items.take(numItems)) {
                    Box(
                        modifier = Modifier
                            .background(color = it)
                            .fillMaxWidth()
                            .height(64.dp)
                    )
                }
            }
        }
    }
}

package com.neeva.app.cardgrid

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.round

internal val MINIMUM_CARD_WIDTH = 200.dp
internal val MINIMUM_CARD_CONTENT_HEIGHT = 200.dp

@Composable
fun <T> CardGrid(
    gridState: LazyGridState,
    allItems: List<T>,
    emptyComposable: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    minimumCardWidth: Dp = MINIMUM_CARD_WIDTH,
    itemComposable: @Composable (T) -> Unit
) {
    BoxWithConstraints(modifier = modifier) {
        val numCells = round(maxWidth / minimumCardWidth).toInt().coerceAtLeast(2)

        val contentModifier = Modifier.fillMaxSize()
        if (allItems.isEmpty()) {
            emptyComposable()
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(numCells),
                state = gridState,
                modifier = contentModifier
            ) {
                items(allItems) {
                    itemComposable(it)
                }
            }
        }
    }
}

package com.neeva.app.cardgrid

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.round

internal val MINIMUM_CARD_WIDTH = 200.dp
internal val MINIMUM_CARD_CONTENT_HEIGHT = 200.dp

@Composable
fun <T> CardGrid(
    items: List<T>,
    emptyComposable: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    computeFirstVisibleItemIndex: (numCellsPerRow: Int) -> Int = { 0 },
    minimumCardWidth: Dp = MINIMUM_CARD_WIDTH,
    content: LazyGridScope.(numCellsPerRow: Int, listItems: List<T>) -> Unit
) {
    BoxWithConstraints(modifier = modifier) {
        if (items.isEmpty()) {
            emptyComposable()
        } else {
            val numCells = round(maxWidth / minimumCardWidth).toInt().coerceAtLeast(2)
            val gridState = rememberLazyGridState(
                initialFirstVisibleItemIndex = computeFirstVisibleItemIndex(numCells)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(numCells),
                state = gridState,
                modifier = Modifier.fillMaxSize()
            ) {
                content(numCells, items)
            }
        }
    }
}

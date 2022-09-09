// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.cardgrid

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.neeva.app.ui.layouts.GridContainer

internal val MINIMUM_CARD_WIDTH = 200.dp
internal val MINIMUM_CARD_CONTENT_HEIGHT = 200.dp

@Composable
fun <T> CardGrid(
    items: List<T>,
    emptyComposable: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    computeFirstVisibleItemIndex: (numCellsPerRow: Int) -> Int = { 0 },
    content: LazyGridScope.(numCellsPerRow: Int, listItems: List<T>) -> Unit
) {
    GridContainer(
        minItemWidth = MINIMUM_CARD_WIDTH,
        modifier = modifier
    ) { numCells ->
        if (items.isEmpty()) {
            emptyComposable()
        } else {
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

@Composable
fun CardGrid(
    modifier: Modifier = Modifier,
    computeFirstVisibleItemIndex: (numCellsPerRow: Int) -> Int = { 0 },
    content: LazyGridScope.(numCellsPerRow: Int) -> Unit
) {
    GridContainer(
        minItemWidth = MINIMUM_CARD_WIDTH,
        modifier = modifier
    ) { numCells ->
        val gridState = rememberLazyGridState(
            initialFirstVisibleItemIndex = computeFirstVisibleItemIndex(numCells)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(numCells),
            state = gridState,
            modifier = Modifier.fillMaxSize()
        ) {
            content(numCells)
        }
    }
}

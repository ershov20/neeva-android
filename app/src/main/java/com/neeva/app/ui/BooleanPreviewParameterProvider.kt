package com.neeva.app.ui

import androidx.compose.ui.tooling.preview.PreviewParameterProvider

/** Helper class that can create @PreviewParameter annotated classes for Composables. */
abstract class BooleanPreviewParameterProvider<T>(numBools: Int) : PreviewParameterProvider<T> {
    override val values: Sequence<T> = sequence {
        val setSize = 1 shl numBools
        for (bits in 0 until setSize) {
            val currentArray = BooleanArray(numBools)
            for (j in 0 until numBools) {
                currentArray[j] = bits and (1 shl j) != 0
            }
            yield(createParams(currentArray))
        }
    }

    abstract fun createParams(booleanArray: BooleanArray): T
}

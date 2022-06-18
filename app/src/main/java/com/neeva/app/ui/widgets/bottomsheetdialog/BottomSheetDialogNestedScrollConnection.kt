package com.neeva.app.ui.widgets.bottomsheetdialog

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeableState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity

/**
 * Forked from Material's internal PreUpPostDownNestedScrollConnection, which we can't access.
 *
 * The main change is in [onPreFling], which assumes that the minimum offset is 0.0 (because we
 * can't access the minBound stored by the [SwipeableState]).  0.0 implies that the bottom sheet has
 * been dragged all the way to the top of the screen.
 */
@OptIn(ExperimentalMaterialApi::class)
class BottomSheetDialogNestedScrollConnection(
    private val state: SwipeableState<BottomSheetDialogStates>
) : NestedScrollConnection {
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        val delta = available.toFloat()
        return if (delta < 0 && source == NestedScrollSource.Drag) {
            state.performDrag(delta).toOffset()
        } else {
            Offset.Zero
        }
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset {
        return if (source == NestedScrollSource.Drag) {
            state.performDrag(available.toFloat()).toOffset()
        } else {
            Offset.Zero
        }
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        val toFling = Offset(available.x, available.y).toFloat()
        return if (toFling < 0 && state.offset.value > 0.0f) {
            state.performFling(velocity = toFling)
            available
        } else {
            Velocity.Zero
        }
    }

    override suspend fun onPostFling(
        consumed: Velocity,
        available: Velocity
    ): Velocity {
        state.performFling(velocity = Offset(available.x, available.y).toFloat())
        return available
    }

    private fun Float.toOffset(): Offset = Offset(0f, this)

    private fun Offset.toFloat(): Float = this.y
}

package com.neeva.app.widgets.collapsible

import androidx.annotation.StringRes
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.res.stringResource

/** Collapsible section of a [LazyList] that allows callers to display the items in three states. */
fun LazyListScope.collapsibleThreeStateSection(
    @StringRes label: Int,
    collapsingSectionState: State<CollapsingSectionState>,
    updateCollapsingHeaderState: () -> Unit,
    expandedContent: @Composable () -> Unit,
    compactContent: @Composable () -> Unit
) {
    item {
        CollapsibleThreeStateHeader(
            label = stringResource(label),
            state = collapsingSectionState,
            onClick = updateCollapsingHeaderState
        )
    }

    item {
        when (collapsingSectionState.value) {
            CollapsingSectionState.EXPANDED -> expandedContent()
            CollapsingSectionState.COMPACT -> compactContent()
            CollapsingSectionState.COLLAPSED -> {}
        }
    }
}

/** Collapsible section of a [LazyList] that only shows items when the section is Expanded. */
fun LazyListScope.collapsibleSection(
    @StringRes label: Int,
    collapsingSectionState: State<CollapsingSectionState>,
    updateCollapsingHeaderState: () -> Unit,
    expandedContent: (LazyListScope.() -> Unit)
) {
    item {
        CollapsibleTwoStateHeader(
            label = stringResource(label),
            state = collapsingSectionState,
            onClick = updateCollapsingHeaderState
        )
    }

    if (collapsingSectionState.value == CollapsingSectionState.EXPANDED) {
        expandedContent()
    }
}

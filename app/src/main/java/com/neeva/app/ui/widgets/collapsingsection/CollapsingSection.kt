package com.neeva.app.ui.widgets.collapsingsection

import androidx.annotation.StringRes
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/** Collapsible section of a [LazyList] that allows callers to display the items in three states. */
fun LazyListScope.collapsingThreeStateSection(
    @StringRes label: Int,
    collapsingSectionState: CollapsingSectionState,
    onUpdateCollapsingSectionState: () -> Unit,
    expandedContent: @Composable () -> Unit,
    compactContent: @Composable () -> Unit
) {
    item {
        CollapsingThreeStateHeader(
            label = stringResource(label),
            state = collapsingSectionState,
            onClick = onUpdateCollapsingSectionState
        )
    }

    item {
        when (collapsingSectionState) {
            CollapsingSectionState.EXPANDED -> expandedContent()
            CollapsingSectionState.COMPACT -> compactContent()
            CollapsingSectionState.COLLAPSED -> {}
        }
    }
}

/** Collapsible section of a [LazyList] that only shows items when the section is Expanded. */
fun LazyListScope.collapsingSection(
    @StringRes label: Int,
    collapsingSectionState: CollapsingSectionState,
    onUpdateCollapsingSectionState: () -> Unit,
    expandedContent: (LazyListScope.() -> Unit)
) {
    item {
        CollapsingTwoStateHeader(
            label = stringResource(label),
            state = collapsingSectionState,
            onClick = onUpdateCollapsingSectionState
        )
    }

    if (collapsingSectionState == CollapsingSectionState.EXPANDED) {
        expandedContent()
    }
}

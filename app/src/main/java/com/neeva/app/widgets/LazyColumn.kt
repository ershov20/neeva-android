package com.neeva.app.widgets

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.MutableLiveData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.items
import com.neeva.app.R
import com.neeva.app.ui.theme.ColorPalette

fun <T : Any> LazyListScope.collapsibleSection(
    @StringRes label: Int,
    displayedItems: LazyPagingItems<T>,
    isExpanded: MutableState<Boolean>,
    itemContent: @Composable() (LazyItemScope.(T?) -> Unit)
) {
    item {
        CollapsingHeader(
            label = stringResource(label),
            isExpanded = isExpanded.value
        ) {
            isExpanded.value = !isExpanded.value
        }
    }
    if (isExpanded.value) {
        items(displayedItems) {
            itemContent(it)
        }
    }
}

@Composable
fun CollapsingHeader(
    label: String,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier
                .weight(1.0f)
                .padding(horizontal = 16.dp),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Image(
            painter = painterResource(
                if (isExpanded) {
                    R.drawable.ic_baseline_keyboard_arrow_down_24
                } else {
                    R.drawable.ic_baseline_keyboard_arrow_up_24
                }
            ),
            contentDescription = null,
            modifier = Modifier
                .padding(8.dp)
                .size(32.dp),
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground)
        )
    }
}

fun <T> LazyListScope.collapsibleHeaderItems(
    label: String,
    startingState: CollapsingState = CollapsingState.HIDDEN,
    key: ((T) -> Any)? = null,
    items: List<T>,
    itemContent: @Composable() (LazyItemScope.(T) -> Unit)
) {
    val state = MutableLiveData(startingState)

    item {
        CollapsibleHeader(label = label, startingState = startingState, state = state)
    }

    items(items, key = key) { item ->
        val itemsState: CollapsingState by state.observeAsState(CollapsingState.HIDDEN)
        if (itemsState != CollapsingState.HIDDEN) {
            itemContent(item)
        }
    }
}

fun LazyListScope.collapsibleHeaderItem(
    label: String,
    startingState: CollapsingState = CollapsingState.HIDDEN,
    itemContent: @Composable() (LazyItemScope.() -> Unit)
) {
    val state = MutableLiveData(startingState)

    item {
        CollapsibleHeader(label = label, startingState = startingState, state = state)
    }

    item {
        val itemsState: CollapsingState by state.observeAsState(CollapsingState.HIDDEN)
        if (itemsState != CollapsingState.HIDDEN) {
            itemContent()
        }
    }
}

@Composable
fun CollapsibleHeader(
    label: String,
    startingState: CollapsingState,
    state: MutableLiveData<CollapsingState>
) {
    val headerState: CollapsingState by state.observeAsState(startingState)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.outline,
            maxLines = 1,
        )
        Spacer(modifier = Modifier.weight(1f))
        Image(
            painter = painterResource(
                id = when (headerState.next()) {
                    CollapsingState.HIDDEN -> R.drawable.ic_baseline_keyboard_arrow_up_24
                    CollapsingState.SHOW_COMPACT -> R.drawable.ic_baseline_keyboard_arrow_down_24
                    CollapsingState.SHOW_ALL -> R.drawable.ic_baseline_keyboard_double_arrow_down_24
                }
            ),
            contentDescription = "$label section",
            contentScale = ContentScale.Inside,
            modifier = Modifier
                .padding(8.dp)
                .size(32.dp, 32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable {
                    state.value = headerState.next()
                },
            colorFilter = ColorFilter.tint(ColorPalette.Brand.BlueUICandidate)
        )
    }
}

enum class CollapsibleType {
    TWO_STATES, THREE_STATES
}

enum class CollapsingState(val type: CollapsibleType = CollapsibleType.TWO_STATES) {
    HIDDEN {
        override fun next() = SHOW_COMPACT
    },

    SHOW_COMPACT {
        override fun next() = if (type == CollapsibleType.TWO_STATES) HIDDEN else SHOW_ALL
    },

    SHOW_ALL {
        override fun next() = HIDDEN
    };

    abstract fun next(): CollapsingState
}

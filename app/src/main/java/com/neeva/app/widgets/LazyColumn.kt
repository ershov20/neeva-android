package com.neeva.app.widgets

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.items
import com.neeva.app.R
import com.neeva.app.ui.theme.Dimensions

fun <T : Any> LazyListScope.collapsibleSection(
    @StringRes label: Int,
    displayedItems: List<T>,
    isExpanded: MutableState<Boolean>,
    isDisplayedAsRow: Boolean = false,
    itemContent: @Composable() (LazyItemScope.(T) -> Unit)
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
        if (isDisplayedAsRow) {
            item {
                LazyRow(
                    modifier = Modifier.padding(vertical = Dimensions.PADDING_MEDIUM)
                ) {
                    items(displayedItems) {
                        itemContent(it)
                    }
                }
            }
        } else {
            items(displayedItems) {
                itemContent(it)
            }
        }
    }
}

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

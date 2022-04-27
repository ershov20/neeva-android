package com.neeva.app.overflowmenu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.neeva.app.R
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.ui.theme.NeevaTheme

@Composable
fun OverflowMenuIcon(
    itemData: OverflowMenuItem,
    modifier: Modifier = Modifier
) {
    val label = itemData.labelId?.let { stringResource(it) } ?: ""

    if (itemData.icon != null) {
        Icon(
            modifier = modifier,
            imageVector = itemData.icon,
            contentDescription = label
        )
    } else if (itemData.imageResourceID != null) {
        Icon(
            modifier = modifier,
            painter = painterResource(id = itemData.imageResourceID),
            contentDescription = label
        )
    }
}

@Composable
fun OverflowMenuRow(
    itemData: OverflowMenuItem,
    onMenuItem: (OverflowMenuItemId) -> Unit
) {
    val label = itemData.labelId?.let { stringResource(it) } ?: ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = label) {
                onMenuItem(itemData.id)
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier
                .weight(1.0f)
                .padding(Dimensions.PADDING_LARGE),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )

        OverflowMenuIcon(
            itemData = itemData,
            modifier = Modifier.size(Dimensions.SIZE_TOUCH_TARGET)
        )
    }
}

@Preview(name = "Short text, 1x font size", locale = "en")
@Preview(name = "Short text, 2x font size", locale = "en", fontScale = 2.0f)
@Composable
fun NeevaMenuRow_Preview() {
    NeevaTheme {
        OverflowMenuRow(
            itemData = OverflowMenuItem(
                id = OverflowMenuItemId.HISTORY,
                labelId = R.string.history,
                imageResourceID = R.drawable.ic_baseline_history_24
            ),
            onMenuItem = {}
        )
    }
}

@Preview(name = "Long text, 1x font size", locale = "en")
@Preview(name = "Long text, 2x font size", locale = "en", fontScale = 2.0f)
@Preview(name = "Long text, RTL, 1x font size", locale = "he")
@Preview(name = "Long text, RTL, 2x font size", locale = "he", fontScale = 2.0f)
@Composable
fun NeevaMenuRow_PreviewLongText() {
    NeevaTheme {
        OverflowMenuRow(
            itemData = OverflowMenuItem(
                id = OverflowMenuItemId.HISTORY,
                labelId = R.string.debug_long_string_primary,
                imageResourceID = R.drawable.ic_baseline_history_24
            ),
            onMenuItem = {}
        )
    }
}

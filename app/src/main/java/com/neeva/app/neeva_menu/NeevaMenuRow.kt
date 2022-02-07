package com.neeva.app.neeva_menu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.neeva.app.R
import com.neeva.app.ui.theme.NeevaTheme

@Composable
fun NeevaMenuIcon(
    itemData: NeevaMenuItemData,
    modifier: Modifier = Modifier
) {
    if (itemData.icon != null) {
        Icon(
            modifier = modifier,
            imageVector = itemData.icon,
            contentDescription = stringResource(id = itemData.labelId),
            tint = MaterialTheme.colorScheme.onSurface
        )
    } else if (itemData.imageResourceID != null) {
        Icon(
            modifier = modifier,
            painter = painterResource(id = itemData.imageResourceID),
            contentDescription = stringResource(id = itemData.labelId),
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun NeevaMenuRow(
    itemData: NeevaMenuItemData,
    onMenuItem: (NeevaMenuItemId) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = stringResource(itemData.labelId)) {
                onMenuItem(itemData.id)
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(itemData.labelId),
            modifier = Modifier
                .weight(1.0f)
                .padding(16.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )

        NeevaMenuIcon(
            itemData = itemData,
            modifier = Modifier.width(48.dp)
        )
    }
}

@Preview(name = "Short text, 1x font size", locale = "en")
@Preview(name = "Short text, 2x font size", locale = "en", fontScale = 2.0f)
@Composable
fun NeevaMenuRow_Preview() {
    NeevaTheme {
        NeevaMenuRow(
            itemData = NeevaMenuItemData(
                id = NeevaMenuItemId.HISTORY,
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
        NeevaMenuRow(
            itemData = NeevaMenuItemData(
                id = NeevaMenuItemId.HISTORY,
                labelId = R.string.debug_long_string_primary,
                imageResourceID = R.drawable.ic_baseline_history_24
            ),
            onMenuItem = {}
        )
    }
}

package com.neeva.app.neeva_menu

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.neeva.app.R
import com.neeva.app.ui.theme.NeevaTheme

@Composable
fun NeevaMenuTileItem(
    itemData: NeevaMenuItemData,
    onMenuItem: (NeevaMenuItemId) -> Unit
) {
    Column(
        modifier = Modifier
            .padding(8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colors.primary)
            .clickable(onClickLabel = stringResource(itemData.labelId)) {
                onMenuItem(itemData.id)
            }
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(itemData.labelId),
            style = MaterialTheme.typography.body1,
            color = MaterialTheme.colors.onPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Image(
            painter = painterResource(id = itemData.imageResourceID),
            contentDescription = null,
            contentScale = ContentScale.Inside,
            modifier = Modifier.size(48.dp, 48.dp),
            colorFilter = ColorFilter.tint(MaterialTheme.colors.onPrimary)
        )
    }
}

@Preview(name = "Short text, 1x font size", locale = "en")
@Preview(name = "Short text, 2x font size", locale = "en", fontScale = 2.0f)
@Composable
fun NeevaMenuRectangleItem_PreviewShortText() {
    NeevaTheme {
        NeevaMenuTileItem(
            itemData = NeevaMenuItemData(
                id = NeevaMenuItemId.HOME,
                labelId = R.string.home,
                imageResourceID = R.drawable.ic_baseline_home_24
            ),
            onMenuItem = {}
        )
    }
}

@Preview(name = "Long text, 1x font size", locale = "en")
@Preview(name = "Long text, 2x font size", locale = "en", fontScale = 2.0f)
@Composable
fun NeevaMenuRectangleItem_PreviewLongText() {
    NeevaTheme {
        NeevaMenuTileItem(
            itemData = NeevaMenuItemData(
                id = NeevaMenuItemId.HOME,
                labelId = R.string.debug_long_string_primary,
                imageResourceID = R.drawable.ic_baseline_home_24
            ),
            onMenuItem = {}
        )
    }
}

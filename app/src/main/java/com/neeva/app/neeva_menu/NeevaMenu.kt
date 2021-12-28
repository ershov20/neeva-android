package com.neeva.app.neeva_menu

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.neeva.app.AppNavModel
import com.neeva.app.AppNavState
import com.neeva.app.widgets.OverlaySheet
import com.neeva.app.widgets.OverlaySheetHeightConfig

@Composable
fun NeevaMenuSheet(appNavModel: AppNavModel) {
    OverlaySheet(
        appNavModel = appNavModel,
        visibleState = AppNavState.NEEVA_MENU,
        config = OverlaySheetHeightConfig.WRAP_CONTENT
    ) {
        NeevaMenuContent(onMenuItem = appNavModel::onMenuItem)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NeevaMenuContent(onMenuItem: (NeevaMenuItemId) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colors.background)
    ) {
        LazyVerticalGrid(
            cells = GridCells.Fixed(2),
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
        ) {
            items(NeevaMenuData.data.subList(0, 4)) { itemData ->
                NeevaMenuRectangleItem(
                    itemData = itemData,
                    onMenuItem = onMenuItem
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colors.primary)
        ) {
            items(NeevaMenuData.data.subList(4, NeevaMenuData.data.size)) { itemData ->
                NeevaMenuRow(
                    itemData = itemData,
                    onMenuItem = onMenuItem
                )
            }
        }
    }
}

@Composable
fun NeevaMenuRectangleItem(
    itemData: NeevaMenuItemData,
    onMenuItem: (NeevaMenuItemId) -> Unit
) {
    Column(
        modifier = Modifier
            .padding(8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colors.primary)
            .clickable { onMenuItem(itemData.id) }
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = itemData.label,
            style = MaterialTheme.typography.body1,
            color = MaterialTheme.colors.onPrimary,
            maxLines = 1,
        )
        Image(
            imageVector = ImageVector.vectorResource(id = itemData.imageResourceID),
            contentDescription = itemData.contentDescription,
            contentScale = ContentScale.Inside,
            modifier = Modifier.size(48.dp, 48.dp),
            colorFilter = ColorFilter.tint(MaterialTheme.colors.onPrimary)
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
            .clickable { onMenuItem(itemData.id) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = itemData.label,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.body1,
            color = MaterialTheme.colors.onPrimary,
            maxLines = 1,
        )
        Spacer(modifier = Modifier.weight(1f))
        Image(
            imageVector = ImageVector.vectorResource(id = itemData.imageResourceID),
            contentDescription = itemData.contentDescription,
            contentScale = ContentScale.Inside,
            modifier = Modifier.size(48.dp, 48.dp),
            colorFilter = ColorFilter.tint(MaterialTheme.colors.onPrimary)
        )
    }
}
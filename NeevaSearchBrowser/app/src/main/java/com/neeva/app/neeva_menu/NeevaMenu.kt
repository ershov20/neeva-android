package com.neeva.app.neeva_menu

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.neeva.app.AppNavModel
import com.neeva.app.AppNavState
import com.neeva.app.widgets.OverlaySheet
import com.neeva.app.widgets.OverlaySheetHeightConfig

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun NeevaMenuSheet(appNavModel: AppNavModel) {
    OverlaySheet(
        appNavModel = appNavModel,
        visibleState = AppNavState.NEEVA_MENU,
        config = OverlaySheetHeightConfig.WRAP_CONTENT) {
        NeevaMenuContent()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NeevaMenuContent() {
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
                NeevaMenuRectangleItem(itemData = itemData)
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
                NeevaMenuRow(itemData = itemData)
            }
        }
    }
}

@Composable
fun NeevaMenuRectangleItem(itemData: NeevaMenuItemData) {
    Column(
        modifier = Modifier
            .padding(8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colors.primary)
            .clickable { itemData.onClick() }
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
fun NeevaMenuRow(itemData: NeevaMenuItemData) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { itemData.onClick() },
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
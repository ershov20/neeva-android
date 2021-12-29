package com.neeva.app.neeva_menu

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.neeva.app.AppNavModel
import com.neeva.app.AppNavState
import com.neeva.app.ui.theme.NeevaTheme
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
            items(NeevaMenuData.tiles) { itemData ->
                NeevaMenuTileItem(
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
            items(NeevaMenuData.rows) { itemData ->
                NeevaMenuRow(
                    itemData = itemData,
                    onMenuItem = onMenuItem
                )
            }
        }
    }
}

@Preview(name = "1x font size", locale = "en")
@Preview(name = "2x font size", locale = "en", fontScale = 2.0f)
@Preview(name = "RTL, 1x font size", locale = "he")
@Preview(name = "RTL, 2x font size", locale = "he", fontScale = 2.0f)
@Composable
fun NeevaMenuContent_Preview() {
    NeevaTheme {
        NeevaMenuContent {}
    }
}

package com.neeva.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.neeva.app.web.WebLayerModel


class TabToolbarModel(
    val onNeevaMenu: () -> Unit,
    val onAddToSpace: () -> Unit,
    val onTabSwitcher: () -> Unit,
)

@Composable
fun TabToolbar(model: TabToolbarModel, webLayerModel: WebLayerModel) {
    val canGoBack: Boolean? by webLayerModel.canGoBack.observeAsState()
    val canGoForward: Boolean? by webLayerModel.canGoForward.observeAsState()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(MaterialTheme.colors.primary),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        TabToolbarButton(
            enabled = canGoBack ?: false,
            resID = R.drawable.ic_baseline_arrow_back_24,
            contentDescription = "back button",
            onClick = { webLayerModel.goBack() }
        )
        TabToolbarButton(
            enabled = canGoForward ?: false,
            resID = R.drawable.ic_baseline_arrow_forward_24,
            contentDescription = "forward button"
        ) { webLayerModel.goForward() }
        NeevaMenuButton(onClick = model.onNeevaMenu)
        TabToolbarButton(
            true,
            resID = R.drawable.ic_baseline_bookmark_border_24,
            contentDescription = "save to space",
            onClick = model.onAddToSpace
        )
        TabToolbarButton(
            true,
            resID = R.drawable.ic_baseline_grid_view_24,
            contentDescription = "switch tabs",
            onClick = model.onTabSwitcher
        )
    }
}

@Composable
fun TabToolbarButton(
    enabled: Boolean,
    resID: Int,
    contentDescription: String,
    onClick: () -> Unit
) {
    Image(
        imageVector = ImageVector.vectorResource(id = resID),
        contentDescription = contentDescription,
        contentScale = ContentScale.Inside,
        modifier = Modifier
            .size(48.dp, 48.dp)
            .clickable(enabled) { onClick() },
        colorFilter = ColorFilter.tint(if (enabled) MaterialTheme.colors.onPrimary else Color.LightGray)
    )
}

@Composable
fun NeevaMenuButton(onClick: () -> Unit) {
    Image(
        imageVector = ImageVector.vectorResource(id = R.drawable.ic_neeva_logo),
        contentDescription = "neeva menu",
        contentScale = ContentScale.Inside,
        modifier = Modifier
            .size(22.dp, 20.dp)
            .clickable { onClick() }
            .size(48.dp, 48.dp),
    )
}
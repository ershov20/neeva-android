package com.neeva.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.neeva.app.browsing.ActiveTabModel
import com.neeva.app.widgets.Button

class TabToolbarModel(
    val onNeevaMenu: () -> Unit,
    val onAddToSpace: () -> Unit,
    val onTabSwitcher: () -> Unit,
)

@Composable
fun TabToolbar(model: TabToolbarModel, activeTabModel: ActiveTabModel) {
    val navigationInfo by activeTabModel.navigationInfoFlow.collectAsState()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(id = R.dimen.bottom_toolbar_height))
            .background(MaterialTheme.colors.primary),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Button(
            enabled = navigationInfo.canGoBackward,
            resID = R.drawable.ic_baseline_arrow_back_24,
            contentDescription = stringResource(id = R.string.toolbar_go_back),
            onClick = activeTabModel::goBack
        )
        Button(
            enabled = navigationInfo.canGoForward,
            resID = R.drawable.ic_baseline_arrow_forward_24,
            contentDescription = stringResource(id = R.string.toolbar_go_forward),
            onClick = activeTabModel::goForward
        )
        NeevaMenuButton(onClick = model.onNeevaMenu)
        Button(
            true,
            resID = R.drawable.ic_baseline_bookmark_border_24,
            contentDescription = stringResource(R.string.toolbar_save_to_space),
            onClick = model.onAddToSpace
        )
        Button(
            true,
            resID = R.drawable.ic_baseline_grid_view_24,
            contentDescription = stringResource(R.string.toolbar_tab_switcher),
            onClick = model.onTabSwitcher
        )
    }
}

@Composable
fun NeevaMenuButton(onClick: () -> Unit) {
    Image(
        painter = painterResource(R.drawable.ic_neeva_logo_vector),
        contentDescription = stringResource(id = R.string.toolbar_neeva_menu),
        contentScale = ContentScale.Inside,
        modifier = Modifier
            .clickable { onClick() }
            .size(48.dp, 48.dp)
            .padding(12.dp),
    )
}

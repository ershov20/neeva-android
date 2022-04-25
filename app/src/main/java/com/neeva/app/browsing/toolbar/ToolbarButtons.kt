package com.neeva.app.browsing.toolbar

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.neeva.app.LocalBrowserToolbarModel
import com.neeva.app.R

@Composable
fun BackButton(
    color: Color,
    modifier: Modifier = Modifier
) {
    val browserToolbarModel = LocalBrowserToolbarModel.current
    val navigationInfo by browserToolbarModel.navigationInfoFlow.collectAsState()

    IconButton(
        enabled = navigationInfo.canGoBackward,
        onClick = browserToolbarModel::goBack,
        modifier = modifier
    ) {
        // Material3 chooses "onSurface" as the disabled color and doesn't seem to allow you
        // to change it.  Because we use a similar color for Incognito backgrounds,
        // manually set the color to what we wanted with the alpha they pick to avoid making
        // the button invisible.
        Icon(
            Icons.Default.ArrowBack,
            contentDescription = stringResource(id = R.string.toolbar_go_back),
            tint = color.copy(
                alpha = LocalContentColor.current.alpha
            )
        )
    }
}

@Composable
fun ForwardButton(
    color: Color,
    modifier: Modifier = Modifier
) {
    val browserToolbarModel = LocalBrowserToolbarModel.current
    val navigationInfo by browserToolbarModel.navigationInfoFlow.collectAsState()

    IconButton(
        enabled = navigationInfo.canGoForward,
        onClick = browserToolbarModel::goForward,
        modifier = modifier
    ) {
        // Material3 chooses "onSurface" as the disabled color and doesn't seem to allow you
        // to change it.  Because we use a similar color for Incognito backgrounds,
        // manually set the color to what we wanted with the alpha they pick to avoid making
        // the button invisible.
        Icon(
            Icons.Default.ArrowForward,
            contentDescription = stringResource(id = R.string.toolbar_go_forward),
            tint = color.copy(
                alpha = LocalContentColor.current.alpha
            )
        )
    }
}

@Composable
fun RefreshButton(modifier: Modifier = Modifier) {
    val browserToolbarModel = LocalBrowserToolbarModel.current

    IconButton(
        onClick = browserToolbarModel::reload,
        modifier = modifier
    ) {
        Icon(
            Icons.Default.Refresh,
            contentDescription = stringResource(id = R.string.reload)
        )
    }
}

@Composable
fun ShareButton(modifier: Modifier = Modifier) {
    val browserToolbarModel = LocalBrowserToolbarModel.current

    IconButton(
        onClick = browserToolbarModel::share,
        modifier = modifier
    ) {
        Icon(
            Icons.Default.Share,
            contentDescription = stringResource(id = R.string.share),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun AddToSpaceButton(modifier: Modifier = Modifier) {
    val browserToolbarModel = LocalBrowserToolbarModel.current
    val spaceStoreHasUrl by browserToolbarModel.spaceStoreHasUrlFlow.collectAsState()

    IconButton(
        onClick = browserToolbarModel::onAddToSpace,
        modifier = modifier
    ) {
        Icon(
            painter = painterResource(
                if (spaceStoreHasUrl) {
                    R.drawable.ic_baseline_bookmark_24
                } else {
                    R.drawable.ic_baseline_bookmark_border_24
                }
            ),
            contentDescription = stringResource(R.string.toolbar_save_to_space)
        )
    }
}

@Composable
fun TabSwitcherButton(modifier: Modifier = Modifier) {
    val browserToolbarModel = LocalBrowserToolbarModel.current

    IconButton(
        onClick = browserToolbarModel::onTabSwitcher,
        modifier = modifier
    ) {
        TabSwitcherIcon(contentDescription = stringResource(R.string.toolbar_tab_switcher))
    }
}

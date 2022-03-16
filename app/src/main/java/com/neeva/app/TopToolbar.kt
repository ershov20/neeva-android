package com.neeva.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import com.neeva.app.urlbar.FindInPageToolbar
import com.neeva.app.urlbar.URLBar
import kotlinx.coroutines.flow.StateFlow

@Composable
fun TopToolbar(
    topControlOffset: StateFlow<Float>
) {
    // Top controls: URL bar, Suggestions, Zero Query, ...
    val topOffset by topControlOffset.collectAsState()
    val topOffsetDp = with(LocalDensity.current) { topOffset.toDp() }
    TopToolbar(
        modifier = Modifier
            .offset(y = topOffsetDp)
            .background(MaterialTheme.colorScheme.background)
    )
}

@Composable
fun TopToolbar(modifier: Modifier) {
    val browserWrapper = LocalBrowserWrapper.current
    val findInPageModel = browserWrapper.findInPageModel
    val findInPageInfo by findInPageModel.findInPageInfo.collectAsState()

    Box(modifier = modifier) {
        if (findInPageInfo.text != null) {
            FindInPageToolbar(
                findInPageInfo = findInPageInfo,
                onUpdateQuery = { browserWrapper.updateFindInPageQuery(it) },
                onScrollToResult = { forward -> browserWrapper.scrollToFindInPageResult(forward) }
            )
        } else {
            URLBar()
        }
    }
}

package com.neeva.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
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
    val activeTabModel = browserWrapper.activeTabModel

    val progress: Int by activeTabModel.progressFlow.collectAsState()
    val findInPageInfo by activeTabModel.findInPageInfo.collectAsState()

    Column(modifier = modifier) {
        if (findInPageInfo.text != null) {
            FindInPageToolbar(
                findInPageInfo = findInPageInfo,
                findInPageText = findInPageInfo.text!!
            ) { text: String?, forward: Boolean ->
                browserWrapper.activeTabModel.findInPage(text, forward)
            }
        } else {
            URLBar()
        }

        if (progress != 100) {
            LinearProgressIndicator(
                progress = progress / 100.0f,
                modifier = Modifier
                    .height(2.dp)
                    .fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                backgroundColor = MaterialTheme.colorScheme.primaryContainer
            )
        }
    }
}

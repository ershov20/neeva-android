package com.neeva.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.neeva.app.suggestions.SuggestionPane
import com.neeva.app.ui.theme.SelectionHighlight
import com.neeva.app.urlbar.FindInPageToolbar
import com.neeva.app.urlbar.URLBar

@Composable
fun BrowserUI(modifier: Modifier) {
    val browserWrapper = LocalBrowserWrapper.current
    val urlBarModel = browserWrapper.urlBarModel
    val activeTabModel = browserWrapper.activeTabModel

    val isEditing: Boolean by urlBarModel.isEditing.collectAsState(false)
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
                color = SelectionHighlight,
                backgroundColor = Color.LightGray
            )
        }

        if (isEditing) {
            Surface(modifier = Modifier.weight(1.0f)) {
                SuggestionPane()
            }
        }
    }
}

package com.neeva.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.neeva.app.storage.DomainViewModel
import com.neeva.app.suggestions.SuggestionList
import com.neeva.app.suggestions.SuggestionsViewModel
import com.neeva.app.urlbar.URLBar
import com.neeva.app.urlbar.URLBarModel
import com.neeva.app.web.WebPanel
import com.neeva.app.web.WebViewModel

@Composable
fun BrowsingUI(urlBarModel: URLBarModel,
               suggestionsViewModel: SuggestionsViewModel,
               webViewModel: WebViewModel,
               domainViewModel: DomainViewModel,
) {
    val isEditing: Boolean? by urlBarModel.isEditing.observeAsState()
    Column {
        URLBar(urlBarModel = urlBarModel, webViewModel, domainViewModel)
        Box(Modifier.height(1.dp).fillMaxWidth().background(MaterialTheme.colors.background))
        Box(modifier = Modifier.weight(1.0f)) {
            Column {
                Box(modifier = Modifier.weight(1.0f)) {
                    WebPanel(webViewModel)
                    ProgressBar(webViewModel = webViewModel)
                }
                TabToolbar(
                    TabToolbarModel(
                        {},
                        {},
                        {}
                    ),
                    webViewModel
                )
            }
            if (isEditing != false) {
                SuggestionList(suggestionsViewModel, urlBarModel, webViewModel, domainViewModel)
            }
        }
    }
}

@Composable
fun ProgressBar(webViewModel: WebViewModel) {
    val progress: Int by webViewModel.progress.observeAsState(0)

    if (progress != 100) {
        LinearProgressIndicator(
            progress = progress / 100.0f,
            Modifier
                .height(2.dp)
                .fillMaxWidth(),
            color = Color(R.color.selection_highlight),
            backgroundColor = Color.LightGray
        )
    }
}
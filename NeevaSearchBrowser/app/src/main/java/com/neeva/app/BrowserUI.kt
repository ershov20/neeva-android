package com.neeva.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.neeva.app.history.HistoryViewModel
import com.neeva.app.storage.DomainViewModel
import com.neeva.app.suggestions.SuggestionList
import com.neeva.app.suggestions.SuggestionsViewModel
import com.neeva.app.urlbar.URLBarModel
import com.neeva.app.browsing.SelectedTabModel
import com.neeva.app.zeroQuery.ZeroQueryViewModel

@Composable
fun BrowserUI(urlBarModel: URLBarModel,
              suggestionsViewModel: SuggestionsViewModel,
              selectedTabModel: SelectedTabModel,
              domainViewModel: DomainViewModel,
              historyViewModel: HistoryViewModel,
              zeroQueryViewModel: ZeroQueryViewModel
) {
    val isEditing: Boolean? by urlBarModel.isEditing.observeAsState()
    val progress: Int by selectedTabModel.progress.observeAsState(0)
    Column {
        Box {
            Box(Modifier.height(1.dp).fillMaxWidth().background(MaterialTheme.colors.background))
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
        if (isEditing != false) {
            Box(modifier = Modifier.weight(1.0f)) {
                SuggestionList(suggestionsViewModel, urlBarModel, selectedTabModel,
                    domainViewModel, historyViewModel, zeroQueryViewModel)
            }
        }
    }
}
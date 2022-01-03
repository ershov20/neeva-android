package com.neeva.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.neeva.app.browsing.ActiveTabModel
import com.neeva.app.history.HistoryViewModel
import com.neeva.app.suggestions.SuggestionPane
import com.neeva.app.suggestions.SuggestionsModel
import com.neeva.app.urlbar.URLBar
import com.neeva.app.urlbar.URLBarModel

@Composable
fun BrowserUI(
    urlBarModel: URLBarModel,
    suggestionsModel: SuggestionsModel,
    activeTabModel: ActiveTabModel,
    historyViewModel: HistoryViewModel
) {
    val isEditing: Boolean by urlBarModel.isEditing.collectAsState()
    val progress: Int by activeTabModel.progressFlow.collectAsState()

    Column {
        URLBar(suggestionsModel, urlBarModel, historyViewModel)
        Box {
            Box(
                Modifier
                    .height(1.dp)
                    .fillMaxWidth()
                    .background(MaterialTheme.colors.background))
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

        if (isEditing) {
            Box(modifier = Modifier.weight(1.0f)) {
                SuggestionPane(suggestionsModel, urlBarModel, activeTabModel, historyViewModel)
            }
        }
    }
}

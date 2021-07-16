package com.neeva.app

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
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


@Composable
fun SuggestionList(suggestionsViewModel: SuggestionsViewModel,
                   urlBarModel: URLBarModel,
                   webViewModel: WebViewModel) {
    val queryChipSuggestions by suggestionsViewModel.queryChipSuggestions.observeAsState()
    val showSuggestionList: Boolean? by suggestionsViewModel.shouldShowSuggestions.observeAsState()
    val currentURL: String by webViewModel.currentUrl.observeAsState("")
    val currentTitle: String by webViewModel.currentTitle.observeAsState("")

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colors.background)
    ) {
        if  (showSuggestionList != false) {
            items(queryChipSuggestions!!) {
                SuggestionRow(query = it.suggestedQuery)
            }
        } else {
            item {
                CurrentPageRow(url = currentURL, title = currentTitle) {
                    urlBarModel.onLocationBarTextChanged(currentURL)
                    urlBarModel.onRequestFocus()
                }
            }
        }
    }
}

@Composable
fun CurrentPageRow(url: String, title: String, onEditPressed: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
    ) {
        Image(
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_baseline_search_24),
            contentDescription = "query icon",
            modifier = Modifier
                .padding(start = 12.dp),
            colorFilter = ColorFilter.tint(Color.LightGray)
        )
        Column(
            modifier = Modifier.padding(horizontal = 8.dp).weight(1.0f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.body1,
                maxLines = 1,
            )
            Text(
                text = Uri.parse(url).authority ?: url,
                style = MaterialTheme.typography.body2,
                color = Color.DarkGray,
                maxLines = 1,
            )
        }
        Image(
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_baseline_edit_24),
            contentDescription = "query icon",
            contentScale = ContentScale.Inside,
            modifier = Modifier
                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                .requiredSize(48.dp, 48.dp)
                .clickable { onEditPressed() },
            colorFilter = ColorFilter.tint(Color.Black)
        )
    }

}

@Composable
fun SuggestionRow(query: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
    ) {
        Image(
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_baseline_search_24),
            contentDescription = "query icon",
            modifier = Modifier
                .padding(start = 12.dp)
                .wrapContentHeight(Alignment.CenterVertically),
            colorFilter = ColorFilter.tint(Color.LightGray)
        )
        Text(
            text = query,
            style = MaterialTheme.typography.body1,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .wrapContentSize(Alignment.CenterStart)
        )
    }

}
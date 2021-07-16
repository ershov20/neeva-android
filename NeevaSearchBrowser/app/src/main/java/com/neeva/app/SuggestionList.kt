package com.neeva.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp


@Composable
fun SuggestionList(suggestionsViewModel: SuggestionsViewModel) {
    val queryChipSuggestions by suggestionsViewModel.queryChipSuggestions.observeAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
    ) {
        items(queryChipSuggestions!!) {
            SuggestionRow(query = it.suggestedQuery)
        }
    }
}


@Composable
fun SuggestionRow(query: String) {
    Row(
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
package com.neeva.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp


@Composable
fun URLBar(searchTextModel: SearchTextModel) {
    val text: String by searchTextModel.text.observeAsState("")
    val isEditing: Boolean by searchTextModel.isEditing.observeAsState(false)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .padding(vertical = 10.dp)
            .fillMaxWidth()
            .background(MaterialTheme.colors.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color.LightGray)
                .height(42.dp)
                .padding(horizontal = 8.dp)
        ) {
            BasicTextField(
                text,
                onValueChange = { searchTextModel.onSearchTextChanged(it) },
                modifier = Modifier
                    .matchParentSize()
                    .onFocusChanged(searchTextModel::onFocusChanged)
                    .focusRequester(searchTextModel.focusRequester)
                    .wrapContentSize(if (isEditing) Alignment.CenterStart else Alignment.Center),
                singleLine = true,
                textStyle = TextStyle(
                    color = if (text.isEmpty()) Color.LightGray else Color.Black,
                    fontSize = MaterialTheme.typography.body1.fontSize
                ),
            )
            if (!isEditing) {
                Text(
                    text = text.ifEmpty { "Search or enter address" },
                    modifier = Modifier
                        .matchParentSize()
                        .clickable {
                            searchTextModel.onRequestFocus()
                        }
                        .wrapContentSize(if (isEditing) Alignment.CenterStart else Alignment.Center),
                    style = MaterialTheme.typography.body1,
                    maxLines = 1,
                    color = if (text.isEmpty()) Color.DarkGray else Color.Black
                )
            }
        }
    }
}

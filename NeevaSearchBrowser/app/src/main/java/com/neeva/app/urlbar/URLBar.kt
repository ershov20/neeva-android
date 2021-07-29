package com.neeva.app.urlbar

import androidx.compose.foundation.Image
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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.neeva.app.R
import com.neeva.app.TabToolbarButton
import com.neeva.app.storage.DomainViewModel
import com.neeva.app.web.WebViewModel
import com.neeva.app.widgets.FaviconView

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun URLBar(urlBarModel: URLBarModel, webViewModel: WebViewModel, domainViewModel: DomainViewModel) {
    val value: TextFieldValue by urlBarModel.text.observeAsState(TextFieldValue("", TextRange.Zero))

    val isEditing: Boolean by urlBarModel.isEditing.observeAsState(false)
    val showLock: Boolean by urlBarModel.showLock.observeAsState(false)
    val currentUrl:String by webViewModel.currentUrl.observeAsState("")
    val autocompletedSuggestion by domainViewModel.autocompletedSuggestion.observeAsState(null)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colors.primary)
            .padding(horizontal = 8.dp)
            .padding(vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .clickable {
                    if (isEditing && !autocompletedSuggestion?.secondaryLabel.isNullOrEmpty()) {
                        val completed = autocompletedSuggestion!!.secondaryLabel
                        urlBarModel.onLocationBarTextChanged(
                            value.copy(
                                completed,
                                TextRange(completed.length, completed.length),
                                TextRange(completed.length, completed.length)
                            )
                        )
                    } else {
                        urlBarModel.onRequestFocus()
                    }
                }
                .background(MaterialTheme.colors.primaryVariant)
                .height(40.dp)
                .padding(horizontal = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .matchParentSize()
            ) {
                FaviconView(
                    domainViewModel = domainViewModel,
                    url = autocompletedSuggestion?.url ?: currentUrl,
                    bordered = false)
                BasicTextField(
                    value,
                    onValueChange = { inside: TextFieldValue ->
                        urlBarModel.onLocationBarTextChanged(inside)
                    },
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .wrapContentSize(if (isEditing) Alignment.CenterStart else Alignment.Center)
                        .width(IntrinsicSize.Min)
                        .onFocusChanged(urlBarModel::onFocusChanged)
                        .focusRequester(urlBarModel.focusRequester),
                    maxLines = 1,
                    textStyle = TextStyle(
                        color = if (value.text.isEmpty()) MaterialTheme.colors.onSecondary
                        else MaterialTheme.colors.onPrimary,
                        fontSize = MaterialTheme.typography.body1.fontSize
                    ),
                )
                Text(
                    text = if (!isEditing || value.text.isEmpty()
                        || autocompletedSuggestion?.secondaryLabel.isNullOrEmpty()
                        || !autocompletedSuggestion!!.secondaryLabel!!.startsWith(value.text))  "" else {
                            autocompletedSuggestion!!.secondaryLabel.substring(value.text.length)
                    },
                    modifier = Modifier
                        .background(Color(R.color.selection_highlight)),
                    style = MaterialTheme.typography.body1,
                    maxLines = 1,
                    color = MaterialTheme.colors.onPrimary,
                    textAlign = TextAlign.Start
                )
            }
            if (!isEditing) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(MaterialTheme.colors.primaryVariant)
                        .matchParentSize()
                        .wrapContentSize(Alignment.Center)
                ) {
                    Spacer(modifier = Modifier.weight(1.0f))
                    if (showLock) {
                        Image(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_baseline_lock_18),
                            contentDescription = "query icon",
                            modifier = Modifier
                                .padding(8.dp)
                                .size(14.dp),
                            colorFilter = ColorFilter.tint(MaterialTheme.colors.onPrimary),
                            contentScale = ContentScale.Fit
                        )
                    }
                    Text(
                        text = value.text.ifEmpty { "Search or enter address" },
                        style = MaterialTheme.typography.body1,
                        maxLines = 1,
                        color = if (value.text.isEmpty()) MaterialTheme.colors.onSecondary
                            else MaterialTheme.colors.onPrimary
                    )
                    Spacer(modifier = Modifier.weight(1.0f))
                    TabToolbarButton(enabled = true,
                        resID = R.drawable.ic_baseline_refresh_24,
                        contentDescription = "refresh button") {
                        webViewModel.reload()
                    }
                }

            }
        }
    }
}

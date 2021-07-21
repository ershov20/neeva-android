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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.neeva.app.R
import com.neeva.app.storage.DomainViewModel
import com.neeva.app.web.WebViewModel
import com.neeva.app.widgets.FaviconView

@Composable
fun URLBar(urlBarModel: URLBarModel, webViewModel: WebViewModel, domainViewModel: DomainViewModel) {
    val text: String by urlBarModel.text.observeAsState("")
    val isEditing: Boolean by urlBarModel.isEditing.observeAsState(false)
    val showLock: Boolean by urlBarModel.showLock.observeAsState(false)
    val currentUrl:String by webViewModel.currentUrl.observeAsState("")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .padding(vertical = 10.dp)
            .background(MaterialTheme.colors.primary)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colors.primaryVariant)
                .height(42.dp)
                .padding(horizontal = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .matchParentSize()
            ) {
                FaviconView(domainViewModel = domainViewModel, url = currentUrl, false)
                BasicTextField(
                    text,
                    onValueChange = { urlBarModel.onLocationBarTextChanged(it) },
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .onFocusChanged(urlBarModel::onFocusChanged)
                        .focusRequester(urlBarModel.focusRequester)
                        .wrapContentSize(if (isEditing) Alignment.CenterStart else Alignment.Center),
                    singleLine = true,
                    textStyle = TextStyle(
                        color = if (text.isNullOrEmpty()) MaterialTheme.colors.onSecondary
                        else MaterialTheme.colors.onPrimary,
                        fontSize = MaterialTheme.typography.body1.fontSize
                    ),
                )
            }
            if (!isEditing) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable {
                            urlBarModel.onRequestFocus()
                        }
                        .background(MaterialTheme.colors.primaryVariant)
                        .matchParentSize()
                        .wrapContentSize(Alignment.Center)
                ) {
                    if (showLock) {
                        Image(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_baseline_lock_18),
                            contentDescription = "query icon",
                            modifier = Modifier
                                .padding(8.dp)
                                .size(14.dp, 14.dp),
                            colorFilter = ColorFilter.tint(MaterialTheme.colors.onPrimary),
                            contentScale = ContentScale.Fit
                        )
                    }
                    Text(
                        text = text.ifEmpty { "Search or enter address" },
                        style = MaterialTheme.typography.body1,
                        maxLines = 1,
                        color = if (text.isEmpty()) MaterialTheme.colors.onSecondary
                            else MaterialTheme.colors.onPrimary
                    )
                }

            }
        }
    }
}

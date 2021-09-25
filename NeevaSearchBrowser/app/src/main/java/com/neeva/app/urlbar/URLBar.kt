package com.neeva.app.urlbar

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.map
import com.neeva.app.R
import com.neeva.app.storage.DomainViewModel
import com.neeva.app.widgets.Button

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun URLBar(urlBarModel: URLBarModel, domainViewModel: DomainViewModel) {
    val isEditing: Boolean by urlBarModel.isEditing.observeAsState(false)
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
                .background(MaterialTheme.colors.primaryVariant)
                .height(40.dp)
                .padding(horizontal = 8.dp)
        ) {
            AutocompleteTextField(urlBarModel, domainViewModel::getFaviconFor)
            if (!isEditing) {
                LocationLabel(urlBarModel, domainViewModel)
            }
        }
    }
}

@Composable
fun LocationLabel(urlBarModel: URLBarModel, domainViewModel: DomainViewModel) {
    val showLock: Boolean by urlBarModel.showLock.observeAsState(false)
    val value: TextFieldValue by urlBarModel.text.observeAsState(TextFieldValue("", TextRange.Zero))
    val isEditing: Boolean by urlBarModel.isEditing.observeAsState(false)
    val autocompletedSuggestion by domainViewModel.autocompletedSuggestion.observeAsState(null)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(MaterialTheme.colors.primaryVariant)
            .wrapContentSize(Alignment.Center)
            .height(40.dp)
            .fillMaxWidth()
            .clickable(!isEditing || autocompletedSuggestion?.secondaryLabel.isNullOrEmpty()) {
                urlBarModel.onRequestFocus()
            }
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
        Button(enabled = true,
            resID = R.drawable.ic_baseline_refresh_24,
            contentDescription = "refresh button",
            onClick = { urlBarModel.onReload() }
        )
    }
}

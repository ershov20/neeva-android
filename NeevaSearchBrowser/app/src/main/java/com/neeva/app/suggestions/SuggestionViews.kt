package com.neeva.app.suggestions

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.neeva.app.R
import com.neeva.app.storage.DomainViewModel
import com.neeva.app.widgets.FaviconView


@Composable
fun NavSuggestView(domainViewModel: DomainViewModel,
                   onOpenUrl: (String) -> Unit,
                   navSuggestion: NavSuggestion) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable { onOpenUrl(navSuggestion.url) }
            .fillMaxWidth()
            .height(58.dp)
            .padding(start = 12.dp)
    ) {
        FaviconView(domainViewModel = domainViewModel, url = navSuggestion.url)
        Column(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .weight(1.0f)
        ) {
            Text(
                text = navSuggestion.label,
                style = MaterialTheme.typography.body1,
                color = MaterialTheme.colors.onPrimary,
                maxLines = 1,
            )
            Text(
                text = navSuggestion.secondaryLabel,
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSecondary,
                maxLines = 1,
            )
        }
    }
}

@Composable
fun QuerySuggestion(query: String,
                    drawableID: Int = R.drawable.ic_baseline_search_24,
                    chip: Boolean = false,
                    onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(8.dp)
            .then(
                if (chip)
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.dp, Color.LightGray, RoundedCornerShape(20.dp))
                        .padding(horizontal = 4.dp)
                else
                    Modifier.fillMaxWidth()
            )
            .clickable { onClick() }
    ) {
        Image(
            imageVector = ImageVector.vectorResource(id = drawableID),
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
                .padding(vertical = 12.dp)
                .wrapContentSize(Alignment.CenterStart),
            color = MaterialTheme.colors.onPrimary,
        )
    }
}
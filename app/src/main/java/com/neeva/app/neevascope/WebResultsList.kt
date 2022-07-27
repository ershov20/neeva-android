package com.neeva.app.neevascope

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.neeva.app.R
import com.neeva.app.ui.LandscapePreviews
import com.neeva.app.ui.LightDarkPreviewContainer
import com.neeva.app.ui.PortraitPreviews
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.ui.widgets.UriDisplayView

fun LazyListScope.WebResultsList(
    webResults: List<NeevascopeWebResult>,
    openUrl: (Uri) -> Unit,
    onDismiss: () -> Unit
) {
    item {
        WebResultsListHeader()
        Spacer(modifier = Modifier.padding(Dimensions.PADDING_MEDIUM))
    }

    items(webResults) { result ->
        WebResultRow(result = result, openUrl = openUrl, onDismiss = onDismiss)
        Spacer(modifier = Modifier.padding(Dimensions.PADDING_MEDIUM))
    }
}

@Composable
fun WebResultsListHeader() {
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(id = R.string.neeva_search),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun WebResultRow(
    result: NeevascopeWebResult,
    openUrl: (Uri) -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                openUrl(result.actionURL)
                onDismiss()
            },
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        WebResultHeader(result = result)

        if (result.snippet != null) {
            Text(
                text = result.snippet,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                maxLines = 3
            )
        }
    }
}

@Composable
fun WebResultHeader(
    result: NeevascopeWebResult
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimensions.PADDING_SMALL)
        ) {
            AsyncImage(
                model = result.faviconURL,
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .size(Dimensions.SIZE_ICON_SMALL)
                    .align(CenterVertically)
            )

            Row {
                // TODO: scrollable row
                UriDisplayView(hostname = result.displayURLHost, pathParts = result.displayURLPath)
            }
        }

        Text(
            text = result.title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1
        )
    }
}

@PortraitPreviews
@LandscapePreviews
@Composable
fun WebResultsListHeader_Preview() {
    LightDarkPreviewContainer {
        WebResultsListHeader()
    }
}

@PortraitPreviews
@LandscapePreviews
@Composable
fun WebResultRow_Preview() {
    LightDarkPreviewContainer {
        WebResultRow(
            result = NeevascopeWebResult(
                faviconURL = "www.reddit.com",
                displayURLHost = "www.reddit.com",
                displayURLPath = listOf("reddit", "path"),
                actionURL = Uri.parse("www.reddit.com"),
                title = "Reddit",
                snippet = "Reddit is an American social news aggregation, content rating, " +
                    "and discussion website. Registered users submit content to the site " +
                    "such as links, text posts, images, and videos, which are then voted up " +
                    "or down by other members."
            ),
            onDismiss = {},
            openUrl = {}
        )
    }
}

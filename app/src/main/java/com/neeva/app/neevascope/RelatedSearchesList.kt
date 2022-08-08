package com.neeva.app.neevascope

import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.neeva.app.NeevaConstants
import com.neeva.app.R
import com.neeva.app.browsing.toSearchUri
import com.neeva.app.ui.LandscapePreviews
import com.neeva.app.ui.LightDarkPreviewContainer
import com.neeva.app.ui.PortraitPreviews
import com.neeva.app.ui.layouts.BaseRowLayout
import com.neeva.app.ui.theme.Dimensions

fun LazyListScope.RelatedSearchesList(
    @StringRes title: Int,
    searches: List<String>,
    openUrl: (Uri) -> Unit,
    neevaConstants: NeevaConstants,
    onDismiss: () -> Unit
) {
    item {
        Text(
            text = stringResource(id = title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
    }

    items(searches) { search ->
        RelatedSearchRow(
            search,
            onTapRow = {
                openUrl(search.toSearchUri(neevaConstants))
                onDismiss()
            }
        )
    }

    item {
        Divider(
            modifier = Modifier.padding(Dimensions.PADDING_MEDIUM),
            color = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
fun RelatedSearchRow(
    search: String,
    onTapRow: () -> Unit
) {
    BaseRowLayout(
        onTapRow = onTapRow,
        startComposable = {
            Icon(
                painter = painterResource(id = R.drawable.ic_baseline_search_24),
                contentDescription = null
            )
        },
        applyVerticalPadding = false
    ) {
        Text(
            text = search,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

@PortraitPreviews
@LandscapePreviews
@Composable
fun RelatedSearchRow_Preview() {
    LightDarkPreviewContainer {
        RelatedSearchRow(
            search = "Related search",
            onTapRow = {}
        )
    }
}

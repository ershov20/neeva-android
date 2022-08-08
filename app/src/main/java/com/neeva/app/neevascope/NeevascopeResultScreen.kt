package com.neeva.app.neevascope

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import com.neeva.app.LocalAppNavModel
import com.neeva.app.LocalNeevaConstants
import com.neeva.app.NeevaConstants
import com.neeva.app.R
import com.neeva.app.ui.LandscapePreviews
import com.neeva.app.ui.LightDarkPreviewContainer
import com.neeva.app.ui.PortraitPreviews
import com.neeva.app.ui.theme.Dimensions

private const val NUMBER_COLLAPSED = 3

@Composable
fun NeevascopeResultScreen(
    neevascopeModel: NeevascopeModel,
    onDismiss: () -> Unit
) {
    val appNavModel = LocalAppNavModel.current
    val neevaConstants = LocalNeevaConstants.current
    val searches by neevascopeModel.searchFlow.collectAsState(initial = null)

    NeevascopeResultScreen(
        openUrl = appNavModel::openUrl,
        showFeedback = appNavModel::showFeedback,
        onDismiss = onDismiss,
        searches = searches,
        neevaConstants = neevaConstants
    )
}

@Composable
fun NeevascopeResultScreen(
    openUrl: (Uri) -> Unit,
    showFeedback: () -> Unit,
    onDismiss: () -> Unit,
    searches: NeevascopeResult?,
    neevaConstants: NeevaConstants
) {
    val showAllDiscussions = remember { mutableStateOf(true) }
    searches?.redditDiscussions?.takeIf { it.isNotEmpty() }?.let {
        showAllDiscussions.value = it.count() <= NUMBER_COLLAPSED
    }

    val showAllSearches = remember { mutableStateOf(true) }
    searches?.webSearches?.takeIf { it.isNotEmpty() }?.let {
        showAllSearches.value = it.count() <= NUMBER_COLLAPSED
    }

    Surface {
        LazyColumn(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .fillMaxWidth()
                .padding(horizontal = Dimensions.PADDING_LARGE)
        ) {
            searches?.redditDiscussions?.takeIf { it.isNotEmpty() }?.let {
                RedditDiscussionsList(
                    discussions = it,
                    showAllDiscussions = showAllDiscussions,
                    openUrl = openUrl,
                    onDismiss = onDismiss
                )
            }

            searches?.webSearches?.takeIf { it.isNotEmpty() }?.let {
                WebResultsList(
                    webResults = it,
                    showAllSearches = showAllSearches,
                    openUrl = openUrl,
                    onDismiss = onDismiss
                )
            }

            searches?.relatedSearches?.takeIf { it.isNotEmpty() }?.let {
                RelatedSearchesList(
                    title = R.string.neevascope_related_search,
                    searches = it,
                    openUrl = openUrl,
                    neevaConstants = neevaConstants,
                    onDismiss = onDismiss
                )
            }

            searches?.memorizedSearches?.takeIf { it.isNotEmpty() }?.let {
                RelatedSearchesList(
                    title = R.string.neevascope_memorized_query,
                    searches = it,
                    openUrl = openUrl,
                    neevaConstants = neevaConstants,
                    onDismiss = onDismiss
                )
            }

            item {
                SupportSection(showFeedback = showFeedback, onDismiss = onDismiss)
            }
        }
    }
}

@Composable
fun ShowMoreButton(showAll: MutableState<Boolean>) {
    FilledTonalButton(
        onClick = { showAll.value = !showAll.value },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = stringResource(id = R.string.neevascope_show_more))

        Spacer(modifier = Modifier.padding(Dimensions.PADDING_SMALL))
        Icon(
            Icons.Outlined.KeyboardArrowDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(Dimensions.SIZE_ICON_SMALL)
        )
    }
}

@Composable
fun SupportSection(
    showFeedback: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimensions.PADDING_SMALL)
    ) {
        Text(
            text = stringResource(id = R.string.neevascope_support_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        Column {
            Text(
                text = stringResource(id = R.string.neevascope_support_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = stringResource(id = R.string.neevascope_contact_us),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable {
                    showFeedback()
                    onDismiss()
                }
            )
        }
    }
}

@PortraitPreviews
@LandscapePreviews
@Composable
fun SupportSection_Preview() {
    LightDarkPreviewContainer {
        SupportSection(showFeedback = {}, onDismiss = {})
    }
}

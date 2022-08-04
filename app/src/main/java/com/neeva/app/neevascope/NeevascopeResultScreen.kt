package com.neeva.app.neevascope

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
                    openUrl = openUrl,
                    onDismiss = onDismiss
                )
            }

            searches?.webSearches?.takeIf { it.isNotEmpty() }?.let {
                WebResultsList(
                    webResults = it,
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
fun SupportSection(
    showFeedback: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimensions.PADDING_SMALL)
    ) {
        Divider()

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
            // TODO: Add chat bubble icon
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

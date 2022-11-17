// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.neevascope

import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Divider
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.neeva.app.LocalAppNavModel
import com.neeva.app.LocalNeevaConstants
import com.neeva.app.NeevaConstants
import com.neeva.app.R
import com.neeva.app.storage.favicons.FaviconCache
import com.neeva.app.ui.LandscapePreviews
import com.neeva.app.ui.LightDarkPreviewContainer
import com.neeva.app.ui.PortraitPreviews
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.ui.theme.NeevaScopeTheme

private const val NUMBER_COLLAPSED = 3
private val NORESULT_IMAGE_WIDTH = 244.dp
private val NORESULT_IMAGE_HEIGHT = 132.dp

@Composable
fun NeevaScopeResultScreen(
    neevascopeModel: NeevaScopeModel,
    onDismiss: () -> Unit,
    faviconCache: FaviconCache?,
    currentUrl: Uri?
) {
    val appNavModel = LocalAppNavModel.current
    val neevaConstants = LocalNeevaConstants.current
    val searches by neevascopeModel.searchFlow.collectAsState()

    if (searches != null) {
        NeevaScopeResultScreen(
            openUrl = appNavModel::openUrlInNewTab,
            showFeedback = appNavModel::showFeedback,
            onDismiss = onDismiss,
            searches = searches,
            faviconCache = faviconCache,
            currentUrl = currentUrl,
            neevaConstants = neevaConstants
        )
    } else {
        NeevaScopeNoResultScreen(appNavModel::showFeedback, onDismiss)
    }
}

@Composable
fun NeevaScopeResultScreen(
    openUrl: (Uri) -> Unit,
    showFeedback: () -> Unit,
    onDismiss: () -> Unit,
    searches: NeevaScopeResult?,
    faviconCache: FaviconCache?,
    currentUrl: Uri?,
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

    val showFullRecipe = remember { mutableStateOf(false) }

    CompositionLocalProvider(LocalTextStyle.provides(MaterialTheme.typography.bodyMedium)) {
        Surface {
            LazyColumn(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .fillMaxWidth()
                    .padding(horizontal = Dimensions.PADDING_LARGE)
            ) {
                searches?.recipe?.takeIf { it.title != "" }?.let {
                    RecipeList(
                        recipe = it,
                        faviconCache = faviconCache,
                        currentUrl = currentUrl,
                        showFullRecipe = showFullRecipe
                    )
                }

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
}

@Composable
fun NeevaScopeNoResultScreen(
    showFeedback: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface {
        BoxWithConstraints {
            if (constraints.maxWidth > constraints.maxHeight) {
                // Landscape
                Row(modifier = Modifier.fillMaxSize().padding(Dimensions.PADDING_LARGE)) {
                    Column(
                        modifier = Modifier.weight(1.0f).fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Top
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.neevascope_noresult),
                            contentDescription = null,
                            modifier = Modifier.size(NORESULT_IMAGE_WIDTH, NORESULT_IMAGE_HEIGHT)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1.0f)
                            .padding(Dimensions.PADDING_LARGE)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Top
                    ) {
                        Text(
                            text = stringResource(id = R.string.neevascope_no_result),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        SupportButton(showFeedback, onDismiss)
                    }
                }
            } else {
                // Portrait
                Column(
                    modifier = Modifier.padding(top = Dimensions.PADDING_MEDIUM),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(40.dp),
                ) {
                    Text(
                        text = stringResource(id = R.string.neevascope_no_result),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(horizontal = Dimensions.PADDING_HUGE)
                    )

                    Image(
                        painter = painterResource(id = R.drawable.neevascope_noresult),
                        contentDescription = null,
                        modifier = Modifier.size(NORESULT_IMAGE_WIDTH, NORESULT_IMAGE_HEIGHT)
                    )

                    SupportButton(showFeedback, onDismiss)
                }
            }
        }
    }
}

@Composable
fun NeevaScopeSectionHeader(
    @StringRes title: Int,
    content: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        if (content != null) {
            Text(
                text = stringResource(id = title, content),
                color = MaterialTheme.colorScheme.onSurface,
                style = NeevaScopeTheme.headerStyle
            )
        } else {
            Text(
                text = stringResource(id = title),
                color = MaterialTheme.colorScheme.onSurface,
                style = NeevaScopeTheme.headerStyle
            )
        }
    }
}

@Composable
fun NeevaScopeDivider() {
    Divider(
        modifier = Modifier.padding(vertical = Dimensions.PADDING_MEDIUM),
        color = MaterialTheme.colorScheme.surfaceVariant
    )
}

@Composable
fun ShowMoreButton(
    @StringRes text: Int,
    showAll: MutableState<Boolean>
) {
    FilledTonalButton(
        onClick = { showAll.value = !showAll.value },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = stringResource(id = text))

        Spacer(modifier = Modifier.padding(Dimensions.PADDING_TINY))

        Icon(
            if (showAll.value) {
                Icons.Outlined.KeyboardArrowUp
            } else {
                Icons.Outlined.KeyboardArrowDown
            },
            contentDescription = null,
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
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(id = R.string.neevascope_support_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        SupportButton(showFeedback, onDismiss)
    }
}

@Composable
fun SupportButton(
    showFeedback: () -> Unit,
    onDismiss: () -> Unit
) {
    TextButton(
        onClick = {
            showFeedback()
            onDismiss()
        }
    ) {
        Icon(
            Icons.Outlined.HelpOutline,
            contentDescription = null,
            modifier = Modifier.size(Dimensions.SIZE_ICON_SMALL)
        )

        Spacer(modifier = Modifier.padding(Dimensions.PADDING_TINY))

        Text(text = stringResource(id = R.string.neevascope_support))
    }
}

@PortraitPreviews
@LandscapePreviews
@Composable
fun NeevaScopeSectionHeader_Preview() {
    LightDarkPreviewContainer {
        NeevaScopeSectionHeader(title = R.string.neevascope_related_search)
    }
}

@PortraitPreviews
@LandscapePreviews
@Composable
fun ShowMoreButton_Preview() {
    LightDarkPreviewContainer {
        ShowMoreButton(
            text = R.string.neevascope_discussion_show_more,
            showAll = remember { mutableStateOf(false) }
        )
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

@PortraitPreviews
@LandscapePreviews
@Composable
fun NeevaScopeNoResultScreen_Preview() {
    LightDarkPreviewContainer {
        NeevaScopeNoResultScreen(showFeedback = {}, onDismiss = {})
    }
}

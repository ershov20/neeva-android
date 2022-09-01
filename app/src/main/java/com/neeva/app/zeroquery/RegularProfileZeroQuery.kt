// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.zeroquery

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp
import com.neeva.app.LocalAppNavModel
import com.neeva.app.LocalBrowserWrapper
import com.neeva.app.LocalDomainProvider
import com.neeva.app.LocalNeevaUser
import com.neeva.app.LocalRegularProfileZeroQueryViewModel
import com.neeva.app.NeevaConstants
import com.neeva.app.R
import com.neeva.app.browsing.toSearchUri
import com.neeva.app.browsing.urlbar.URLBarModel
import com.neeva.app.spaces.SpaceRow
import com.neeva.app.storage.entities.Site
import com.neeva.app.suggestions.QueryRowSuggestion
import com.neeva.app.suggestions.QuerySuggestionRow
import com.neeva.app.ui.layouts.GridLayout
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.ui.widgets.collapsingsection.collapsingSection
import com.neeva.app.ui.widgets.collapsingsection.collapsingThreeStateSection

data class SuggestedSite(
    val site: Site,
    val bitmap: Bitmap? = null,
    val overrideDrawableId: Int? = null
)

@Composable
fun RegularProfileZeroQuery(
    urlBarModel: URLBarModel,
    isFirstRun: Boolean,
    topContent: @Composable () -> Unit = {},
) {
    val browserWrapper = LocalBrowserWrapper.current
    val domainProvider = LocalDomainProvider.current
    val appNavModel = LocalAppNavModel.current
    val neevaUser = LocalNeevaUser.current

    val zeroQueryModel = LocalRegularProfileZeroQueryViewModel.current
    val suggestedSearchesWithDefaults by zeroQueryModel.suggestedSearches.collectAsState()
    val suggestedSitesPlusHome by zeroQueryModel.suggestedSites.collectAsState()

    val isSuggestedSitesExpanded by zeroQueryModel.isSuggestedSitesExpanded.collectAsState()
    val isSuggestedQueriesExpanded by zeroQueryModel.isSuggestedQueriesExpanded.collectAsState()
    val isCommunitySpacesExpanded by zeroQueryModel.isCommunitySpacesExpanded.collectAsState()
    val isSpacesExpanded by zeroQueryModel.isSpacesExpanded.collectAsState()

    val spaces: List<SpacePlusBitmap> by zeroQueryModel.spaces.collectAsState()
    val communitySpaces: List<SpaceRowPlusBitmap> by zeroQueryModel.communitySpaces.collectAsState()

    LazyColumn(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .semantics { testTag = "RegularProfileZeroQuery" }
    ) {
        item {
            topContent()
        }

        if (!isFirstRun) {
            collapsingThreeStateSection(
                label = R.string.suggested_sites,
                collapsingSectionState = isSuggestedSitesExpanded,
                onUpdateCollapsingSectionState = {
                    zeroQueryModel.advanceState(ZeroQueryPrefs.SuggestedSitesState)
                },
                expandedContent = {
                    // Draw everything as a 4x2 grid with the width evenly divided.
                    GridLayout(4, suggestedSitesPlusHome) { suggestedSite ->
                        ZeroQuerySuggestedSite(
                            suggestedSite = suggestedSite,
                            domainProvider = domainProvider,
                            onClick = browserWrapper::loadUrl
                        )
                    }
                },
                compactContent = {
                    // Draw everything as a scrollable Row in the main list.  Keep all of the icons the
                    // same width (64.dp + large padding + large padding).
                    Row(
                        modifier = Modifier
                            .padding(vertical = Dimensions.PADDING_LARGE)
                            .horizontalScroll(rememberScrollState())
                    ) {
                        suggestedSitesPlusHome.forEach { suggestedSite ->
                            Box(modifier = Modifier.width(64.dp + Dimensions.PADDING_LARGE * 2)) {
                                ZeroQuerySuggestedSite(
                                    suggestedSite = suggestedSite,
                                    domainProvider = domainProvider,
                                    onClick = browserWrapper::loadUrl
                                )
                            }
                        }
                    }
                }
            )
        }

        if (suggestedSearchesWithDefaults.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(Dimensions.PADDING_SMALL))
            }

            collapsingSection(
                label = R.string.searches,
                collapsingSectionState = isSuggestedQueriesExpanded,
                onUpdateCollapsingSectionState = {
                    zeroQueryModel.advanceState(ZeroQueryPrefs.SuggestedQueriesState)
                }
            ) {
                items(suggestedSearchesWithDefaults) { search ->
                    QuerySuggestionRow(
                        suggestion = search,
                        onLoadUrl = browserWrapper::loadUrl,
                        onEditUrl = { urlBarModel.replaceLocationBarText(search.query) }
                    )
                }
            }
        }

        if (neevaUser.isSignedOut()) {
            if (communitySpaces.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(Dimensions.PADDING_SMALL))
                }

                collapsingSection(
                    label = R.string.community_spaces,
                    collapsingSectionState = isCommunitySpacesExpanded,
                    onUpdateCollapsingSectionState = {
                        zeroQueryModel.advanceState(ZeroQueryPrefs.CommunitySpacesState)
                    }
                ) {
                    items(
                        if (isFirstRun) {
                            communitySpaces.take(3)
                        } else {
                            communitySpaces
                        },
                        key = { it.spaceRowData.id }
                    ) { data ->
                        val spaceRowData = data.spaceRowData
                        val thumbnail = data.bitmap
                        SpaceRow(
                            spaceName = spaceRowData.name,
                            isSpacePublic = spaceRowData.isPublic,
                            thumbnail = thumbnail?.asImageBitmap(),
                            isCurrentUrlInSpace = null
                        ) {
                            appNavModel.showSpaceDetail(spaceRowData.id)
                        }
                    }
                }
            }
        } else {
            if (spaces.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(Dimensions.PADDING_SMALL))
                }

                collapsingSection(
                    label = R.string.spaces,
                    collapsingSectionState = isSpacesExpanded,
                    onUpdateCollapsingSectionState = {
                        zeroQueryModel.advanceState(ZeroQueryPrefs.SpacesState)
                    }
                ) {
                    items(spaces, key = { it.space.id }) { spacePlusThumbnail ->
                        SpaceRow(
                            space = spacePlusThumbnail.space,
                            thumbnail = spacePlusThumbnail.bitmap?.asImageBitmap(),
                            isCurrentUrlInSpace = null
                        ) {
                            appNavModel.showSpaceDetail(spacePlusThumbnail.space.id)
                        }
                    }
                }
            }
        }

        if (isFirstRun) {
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Spacer(modifier = Modifier.height(Dimensions.PADDING_LARGE))

                    Image(
                        painter = painterResource(id = R.drawable.neeva_letter_logo),
                        contentDescription = null
                    )

                    Spacer(modifier = Modifier.height(Dimensions.PADDING_SMALL))

                    Text(
                        text = stringResource(id = R.string.first_run_ad_free),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

fun String.toSearchSuggest(neevaConstants: NeevaConstants): QueryRowSuggestion = QueryRowSuggestion(
    url = this.toSearchUri(neevaConstants),
    query = this,
    drawableID = R.drawable.ic_baseline_history_24
)

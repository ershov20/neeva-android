package com.neeva.app.zeroquery

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import com.neeva.app.LocalAppNavModel
import com.neeva.app.LocalBrowserWrapper
import com.neeva.app.LocalEnvironment
import com.neeva.app.LocalRegularProfileZeroQueryViewModel
import com.neeva.app.LocalSettingsController
import com.neeva.app.NeevaConstants
import com.neeva.app.R
import com.neeva.app.browsing.toSearchUri
import com.neeva.app.browsing.urlbar.URLBarModel
import com.neeva.app.settings.SettingsToggle
import com.neeva.app.spaces.SpaceRow
import com.neeva.app.spaces.SpaceRowData
import com.neeva.app.spaces.getThumbnailAsync
import com.neeva.app.storage.entities.Site
import com.neeva.app.storage.entities.Space
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
    neevaConstants: NeevaConstants,
    topContent: @Composable () -> Unit = {},
) {
    val browserWrapper = LocalBrowserWrapper.current
    val domainProvider = LocalEnvironment.current.domainProvider
    val appNavModel = LocalAppNavModel.current
    val spaceStore = LocalEnvironment.current.spaceStore
    val settingsController = LocalSettingsController.current
    val neevaUser = LocalEnvironment.current.neevaUser

    val zeroQueryModel = LocalRegularProfileZeroQueryViewModel.current
    val suggestedSearchesWithDefaults by zeroQueryModel.suggestedSearches.collectAsState()
    val suggestedSitesPlusHome by zeroQueryModel.suggestedSites.collectAsState()
    val isSuggestedSitesExpanded = zeroQueryModel.getState(ZeroQueryPrefs.SuggestedSitesState)
    val isSuggestedQueriesExpanded = zeroQueryModel.getState(ZeroQueryPrefs.SuggestedQueriesState)
    val isCommunitySpacesExpanded = zeroQueryModel.getState(ZeroQueryPrefs.CommunitySpacesState)
    val isSpacesExpanded = zeroQueryModel.getState(ZeroQueryPrefs.SpacesState)
    val isNativeSpacesEnabled =
        settingsController.getToggleState(SettingsToggle.DEBUG_NATIVE_SPACES)

    val spaces: List<Space> by spaceStore.allSpacesFlow.collectAsState()
    val communitySpaces: List<SpaceRowData>
        by spaceStore.spacesFromCommunityFlow.collectAsState(emptyList())

    LazyColumn(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
        item {
            topContent()
        }

        collapsingThreeStateSection(
            label = R.string.suggested_sites,
            collapsingSectionState = isSuggestedSitesExpanded.value,
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

        if (suggestedSearchesWithDefaults.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(Dimensions.PADDING_SMALL))
            }

            collapsingSection(
                label = R.string.searches,
                collapsingSectionState = isSuggestedQueriesExpanded.value,
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

        if (neevaUser.isSignedOut() && communitySpaces.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(Dimensions.PADDING_SMALL))
            }

            collapsingSection(
                label = R.string.community_spaces,
                collapsingSectionState = isCommunitySpacesExpanded.value,
                onUpdateCollapsingSectionState = {
                    zeroQueryModel.advanceState(ZeroQueryPrefs.CommunitySpacesState)
                }
            ) {
                items(communitySpaces.take(5), key = { it.id }) {
                    val thumbnail: ImageBitmap? by getThumbnailAsync(uri = it.thumbnail)
                    SpaceRow(
                        spaceName = it.name,
                        isSpacePublic = it.isPublic,
                        thumbnail = thumbnail,
                        isCurrentUrlInSpace = null
                    ) {
                        if (isNativeSpacesEnabled.value) {
                            appNavModel.showSpaceDetail(it.id)
                        } else {
                            appNavModel.openUrl(it.url())
                        }
                    }
                }
            }
        }

        if (!neevaUser.isSignedOut() && spaces.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(Dimensions.PADDING_SMALL))
            }

            collapsingSection(
                label = R.string.spaces,
                collapsingSectionState = isSpacesExpanded.value,
                onUpdateCollapsingSectionState = {
                    zeroQueryModel.advanceState(ZeroQueryPrefs.SpacesState)
                }
            ) {
                items(spaces.subList(0, minOf(3, spaces.size))) { space ->
                    SpaceRow(space = space, isCurrentUrlInSpace = null) {
                        if (isNativeSpacesEnabled.value) {
                            appNavModel.showSpaceDetail(space.id)
                        } else {
                            appNavModel.openUrl(space.url(neevaConstants))
                        }
                    }
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

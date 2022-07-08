package com.neeva.app.zeroquery

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import coil.request.ImageRequest
import com.neeva.app.Dispatchers
import com.neeva.app.NeevaConstants
import com.neeva.app.R
import com.neeva.app.history.HistoryManager
import com.neeva.app.publicsuffixlist.DomainProvider
import com.neeva.app.sharedprefs.SharedPrefFolder
import com.neeva.app.sharedprefs.SharedPreferencesModel
import com.neeva.app.spaces.SpaceRowData
import com.neeva.app.spaces.SpaceStore
import com.neeva.app.storage.BitmapIO
import com.neeva.app.storage.entities.Site
import com.neeva.app.storage.entities.Space
import com.neeva.app.storage.favicons.RegularFaviconCache
import com.neeva.app.suggestions.QueryRowSuggestion
import com.neeva.app.ui.widgets.collapsingsection.CollapsingSectionStateModel
import com.neeva.app.ui.widgets.collapsingsection.CollapsingSectionStateSharedPref
import com.neeva.app.userdata.NeevaUser
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn

/** Data required to present the user with a Zero Query page for their regular profile. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RegularProfileZeroQueryViewModel @Inject constructor(
    @ApplicationContext appContext: Context,
    dispatchers: Dispatchers,
    domainProvider: DomainProvider,
    historyManager: HistoryManager,
    neevaConstants: NeevaConstants,
    neevaUser: NeevaUser,
    regularFaviconCache: RegularFaviconCache,
    sharedPreferencesModel: SharedPreferencesModel,
    spaceStore: SpaceStore
) : ViewModel() {
    companion object {
        private const val NUM_SUGGESTED_SITES = 8
        private const val NUM_SUGGESTED_QUERIES = 3

        private const val NUM_SPACES = 3
        private const val NUM_COMMUNITY_SPACES = 5
    }

    private val homeLabel = appContext.resources.getString(R.string.home)

    private val collapsingSectionStateModel =
        CollapsingSectionStateModel(sharedPreferencesModel, SharedPrefFolder.App)

    fun getState(preferenceKey: CollapsingSectionStateSharedPref) =
        collapsingSectionStateModel.getState(preferenceKey)

    fun advanceState(preferenceKey: CollapsingSectionStateSharedPref) =
        collapsingSectionStateModel.advanceState(preferenceKey)

    val suggestedSites: StateFlow<List<SuggestedSite>> =
        historyManager.suggestedSites
            .mapLatest { suggestedSites ->
                // Produce the list of sites that the user has visited the most.
                val updatedList = suggestedSites.map { SuggestedSite(it) }.toMutableList()

                // The first suggested item should always send the user Home.
                // TODO(dan.alcantara): The user can sign out in the middle of a session, but that's
                //                      not an observable event.
                if (!neevaUser.isSignedOut()) {
                    updatedList.add(
                        index = 0,
                        element = SuggestedSite(
                            site = Site(
                                siteURL = neevaConstants.appURL,
                                title = homeLabel,
                                largestFavicon = null
                            ),
                            overrideDrawableId = R.drawable.ic_house
                        )
                    )
                }

                // Pad out the list of suggested sites with default ones.
                if (updatedList.size < NUM_SUGGESTED_SITES) {
                    // Make sure that the ones that are tacked on don't match any place the user is
                    // already being shown.
                    val domainList = updatedList.map {
                        domainProvider.getRegisteredDomain(Uri.parse(it.site.siteURL))
                    }

                    DefaultSuggestions.DEFAULT_SITE_SUGGESTIONS.forEach { site ->
                        if (updatedList.size >= NUM_SUGGESTED_SITES) return@forEach

                        val siteDomain = domainProvider.getRegisteredDomain(Uri.parse(site.siteURL))
                        if (!domainList.contains(siteDomain)) {
                            updatedList.add(SuggestedSite(site))
                        }
                    }
                }
                updatedList.take(NUM_SUGGESTED_SITES)
            }
            .distinctUntilChanged()
            .mapLatest { updatedList ->
                // Grab all the favicons required to display them whenever the list changes.
                updatedList.map { suggestedSite ->
                    // Check if we've cached the image in the cache.
                    val siteUri = Uri.parse(suggestedSite.site.siteURL)
                    regularFaviconCache.getCachedFavicon(siteUri)?.let {
                        return@map suggestedSite.copy(bitmap = it)
                    }

                    // Try to load the image from over the network.
                    val faviconUrl = suggestedSite.site.largestFavicon?.faviconURL ?: ""
                    val faviconUri = Uri.parse(faviconUrl)
                    if (faviconUri.scheme == "https") {
                        ImageLoader(appContext)
                            .execute(ImageRequest.Builder(appContext).data(faviconUrl).build())
                            .drawable
                            ?.toBitmap()
                            ?.let { return@map suggestedSite.copy(bitmap = it) }
                    }

                    // We can't get an image; just generate one.
                    suggestedSite.copy(bitmap = regularFaviconCache.generateFavicon(siteUri))
                }
            }
            .distinctUntilChanged()
            .flowOn(dispatchers.io)
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val suggestedSearches: StateFlow<List<QueryRowSuggestion>> =
        historyManager.suggestedQueries
            .mapLatest { originalList ->
                if (originalList.size >= NUM_SUGGESTED_QUERIES) return@mapLatest originalList

                // Add some default search suggestions if the user hasn't performed enough to
                // fill out the section.
                val updatedList = originalList.toMutableList()
                DefaultSuggestions.DEFAULT_SEARCH_SUGGESTIONS.forEach { query ->
                    if (updatedList.size >= NUM_SUGGESTED_QUERIES) return@forEach

                    val suggestion = query.toSearchSuggest(neevaConstants)
                    if (updatedList.none { it.query == suggestion.query }) {
                        updatedList.add(suggestion)
                    }
                }

                return@mapLatest updatedList
            }
            .distinctUntilChanged()
            .flowOn(dispatchers.io)
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val spaces: StateFlow<List<SpacePlusBitmap>> =
        spaceStore.allSpacesFlow
            .mapLatest { it.take(NUM_SPACES) }
            .distinctUntilChanged()
            .mapLatest { spaces ->
                spaces.map { space ->
                    SpacePlusBitmap(space, getSpaceBitmap(appContext, space.thumbnail))
                }
            }
            .distinctUntilChanged()
            .flowOn(dispatchers.io)
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val communitySpaces: StateFlow<List<SpaceRowPlusBitmap>> =
        spaceStore.spacesFromCommunityFlow
            .mapLatest { it.take(NUM_COMMUNITY_SPACES) }
            .distinctUntilChanged()
            .mapLatest { spaces ->
                spaces.map { spaceRowData ->
                    SpaceRowPlusBitmap(
                        spaceRowData,
                        getSpaceBitmap(appContext, spaceRowData.thumbnail)
                    )
                }
            }
            .distinctUntilChanged()
            .flowOn(dispatchers.io)
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private suspend fun getSpaceBitmap(context: Context, uri: Uri?): Bitmap? {
        return BitmapIO.loadBitmap(context, uri)
    }
}

data class SpacePlusBitmap(
    val space: Space,
    val bitmap: Bitmap?
)

data class SpaceRowPlusBitmap(
    val spaceRowData: SpaceRowData,
    val bitmap: Bitmap?
)

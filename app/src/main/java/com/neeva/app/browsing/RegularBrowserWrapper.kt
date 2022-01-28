package com.neeva.app.browsing

import android.content.Context
import android.net.Uri
import androidx.fragment.app.Fragment
import com.apollographql.apollo3.ApolloClient
import com.neeva.app.Dispatchers
import com.neeva.app.NeevaConstants
import com.neeva.app.NeevaConstants.browserTypeCookie
import com.neeva.app.NeevaConstants.browserVersionCookie
import com.neeva.app.NeevaUserToken
import com.neeva.app.history.HistoryManager
import com.neeva.app.publicsuffixlist.DomainProvider
import com.neeva.app.saveLoginCookieFrom
import com.neeva.app.spaces.SpaceStore
import com.neeva.app.storage.RegularTabScreenshotManager
import com.neeva.app.storage.favicons.RegularFaviconCache
import com.neeva.app.suggestions.SuggestionsModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.chromium.weblayer.CookieChangedCallback
import org.chromium.weblayer.WebLayer

/**
 * Encapsulates logic for a regular browser profile, which tracks history and automatically fetches
 * suggestions from the backend as the user types out a query.
 */
class RegularBrowserWrapper(
    appContext: Context,
    coroutineScope: CoroutineScope,
    dispatchers: Dispatchers,
    activityCallbackProvider: () -> ActivityCallbacks?,
    domainProvider: DomainProvider,
    apolloClient: ApolloClient,
    override val historyManager: HistoryManager,
    spaceStore: SpaceStore,
    val neevaUserToken: NeevaUserToken
) : BrowserWrapper(
    isIncognito = false,
    appContext = appContext,
    coroutineScope = coroutineScope,
    dispatchers = dispatchers,
    activityCallbackProvider = activityCallbackProvider,
    suggestionsModel = SuggestionsModel(
        coroutineScope,
        historyManager,
        apolloClient,
        domainProvider,
        dispatchers
    ),
    faviconCache = RegularFaviconCache(
        filesDir = appContext.cacheDir,
        domainProvider = domainProvider,
        historyManager = historyManager,
        dispatchers = dispatchers
    )
) {
    companion object {
        private const val NON_INCOGNITO_PROFILE_NAME = "DefaultProfile"
        private const val PERSISTENCE_ID = "Neeva_Browser"
    }

    init {
        urlBarModel.queryTextFlow
            .onEach { queryText ->
                // Pull new suggestions from the database.
                coroutineScope.launch(dispatchers.io) {
                    historyManager.updateSuggestionQuery(queryText)
                }

                // Ask the backend for suggestions appropriate for the currently typed in text.
                coroutineScope.launch(dispatchers.io) {
                    suggestionsModel?.getSuggestionsFromBackend(queryText)
                }
            }
            .launchIn(coroutineScope)

        urlBarModel.isEditing
            .onEach { isEditing -> if (isEditing) spaceStore.refresh() }
            .flowOn(dispatchers.io)
            .launchIn(coroutineScope)
    }

    override fun createBrowserFragment(): Fragment {
        return WebLayer.createBrowserFragment(NON_INCOGNITO_PROFILE_NAME, PERSISTENCE_ID)
    }

    override fun createTabScreenshotManager() = RegularTabScreenshotManager(appContext.cacheDir)

    override fun registerBrowserCallbacks(): Boolean {
        val wasRegistered = super.registerBrowserCallbacks()
        if (!wasRegistered) return false

        browser?.profile?.cookieManager?.apply {
            getCookie(Uri.parse(NeevaConstants.appURL)) {
                it?.split("; ")?.forEach { cookie ->
                    saveLoginCookieFrom(neevaUserToken, cookie)
                }
            }

            addCookieChangedCallback(
                Uri.parse(NeevaConstants.appURL),
                NeevaConstants.loginCookie,
                object : CookieChangedCallback() {
                    override fun onCookieChanged(cookie: String, cause: Int) {
                        saveLoginCookieFrom(neevaUserToken, cookie)
                    }
                }
            )
            setCookie(
                Uri.parse(NeevaConstants.appURL),
                browserTypeCookie.toString() + browserVersionCookie.toString()
            ) {}

            if (!neevaUserToken.getToken().isNullOrEmpty()) {
                setCookie(
                    Uri.parse(NeevaConstants.appURL),
                    neevaUserToken.loginCookieString()
                ) {}
            }
        }

        return true
    }

    override fun onAuthTokenUpdated() {
        browser?.profile?.cookieManager?.setCookie(
            Uri.parse(NeevaConstants.appURL),
            neevaUserToken.loginCookieString()
        ) { success ->
            if (success && activeTabModel.urlFlow.value.toString() == NeevaConstants.appURL) {
                activeTabModel.reload()
            }
        }
    }
}

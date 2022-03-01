package com.neeva.app.browsing

import android.content.Context
import android.net.Uri
import com.neeva.app.ApolloWrapper
import com.neeva.app.Dispatchers
import com.neeva.app.NeevaConstants
import com.neeva.app.NeevaConstants.browserTypeCookie
import com.neeva.app.NeevaConstants.browserVersionCookie
import com.neeva.app.history.HistoryManager
import com.neeva.app.logging.ClientLogger
import com.neeva.app.publicsuffixlist.DomainProvider
import com.neeva.app.saveLoginCookieFrom
import com.neeva.app.spaces.SpaceStore
import com.neeva.app.storage.RegularTabScreenshotManager
import com.neeva.app.storage.favicons.RegularFaviconCache
import com.neeva.app.suggestions.SuggestionsModel
import com.neeva.app.userdata.NeevaUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.chromium.weblayer.Browser
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
    private val apolloWrapper: ApolloWrapper,
    override val historyManager: HistoryManager,
    spaceStore: SpaceStore,
    private val neevaUser: NeevaUser,
    clientLogger: ClientLogger
) : BrowserWrapper(
    isIncognito = false,
    appContext = appContext,
    coroutineScope = coroutineScope,
    dispatchers = dispatchers,
    activityCallbackProvider = activityCallbackProvider,
    suggestionsModel = SuggestionsModel(
        coroutineScope,
        historyManager,
        apolloWrapper,
        dispatchers,
        clientLogger
    ),
    faviconCache = RegularFaviconCache(
        filesDir = appContext.cacheDir,
        domainProvider = domainProvider,
        historyManager = historyManager,
        dispatchers = dispatchers
    ),
    spaceStore = spaceStore
) {
    companion object {
        private const val NON_INCOGNITO_PROFILE_NAME = "DefaultProfile"
        private const val PERSISTENCE_ID = "Neeva_Browser"

        /** Asks WebLayer to get the regular user profile. The Browser does not need to be alive. */
        fun getProfile(webLayer: WebLayer) = webLayer.getProfile(NON_INCOGNITO_PROFILE_NAME)
    }

    init {
        coroutineScope.launch(dispatchers.io) {
            urlBarModel.queryTextFlow.collectLatest { queryText ->
                // Pull new suggestions from the database.
                historyManager.updateSuggestionQuery(queryText)

                // Ask the backend for suggestions appropriate for the currently typed in text.
                suggestionsModel?.getSuggestionsFromBackend(queryText)
            }
        }
    }

    override fun createBrowserFragment() =
        WebLayer.createBrowserFragment(NON_INCOGNITO_PROFILE_NAME, PERSISTENCE_ID)

    override fun createTabScreenshotManager() = RegularTabScreenshotManager(appContext.cacheDir)

    override fun registerBrowserCallbacks(browser: Browser): Boolean {
        val wasRegistered = super.registerBrowserCallbacks(browser)
        if (!wasRegistered) return false

        browser.profile.cookieManager.apply {
            getCookie(Uri.parse(NeevaConstants.appURL)) {
                it?.split(";")?.forEach { cookie ->
                    saveLoginCookieFrom(neevaUser.neevaUserToken, cookie)
                }
            }

            addCookieChangedCallback(
                Uri.parse(NeevaConstants.appURL),
                NeevaConstants.loginCookie,
                object : CookieChangedCallback() {
                    override fun onCookieChanged(cookie: String, cause: Int) {
                        saveLoginCookieFrom(neevaUser.neevaUserToken, cookie)
                        coroutineScope.launch(dispatchers.io) {
                            neevaUser.fetch(apolloWrapper)
                        }
                    }
                }
            )
            setCookie(
                Uri.parse(NeevaConstants.appURL),
                browserTypeCookie.toString() + browserVersionCookie.toString()
            ) {}

            if (neevaUser.neevaUserToken.getToken().isNotEmpty()) {
                setCookie(
                    Uri.parse(NeevaConstants.appURL),
                    neevaUser.neevaUserToken.loginCookieString()
                ) {}
            }
        }

        return true
    }
}

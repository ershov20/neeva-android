package com.neeva.app.browsing

import android.content.Context
import android.net.Uri
import com.neeva.app.ApolloWrapper
import com.neeva.app.Dispatchers
import com.neeva.app.NeevaConstants
import com.neeva.app.cookiecutter.RegularTrackersAllowList
import com.neeva.app.history.HistoryManager
import com.neeva.app.logging.ClientLogger
import com.neeva.app.publicsuffixlist.DomainProvider
import com.neeva.app.settings.SettingsDataModel
import com.neeva.app.spaces.SpaceStore
import com.neeva.app.storage.RegularTabScreenshotManager
import com.neeva.app.storage.daos.HostInfoDao
import com.neeva.app.storage.favicons.RegularFaviconCache
import com.neeva.app.suggestions.SuggestionsModel
import com.neeva.app.userdata.NeevaUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.chromium.weblayer.Browser
import org.chromium.weblayer.CookieChangedCallback
import org.chromium.weblayer.Tab
import org.chromium.weblayer.WebLayer

/**
 * Encapsulates logic for a regular browser profile, which tracks history and automatically fetches
 * suggestions from the backend as the user types out a query.
 */
class RegularBrowserWrapper(
    appContext: Context,
    activityCallbackProvider: ActivityCallbackProvider,
    private val apolloWrapper: ApolloWrapper,
    clientLogger: ClientLogger,
    coroutineScope: CoroutineScope,
    dispatchers: Dispatchers,
    domainProvider: DomainProvider,
    historyManager: HistoryManager,
    hostInfoDao: HostInfoDao,
    neevaConstants: NeevaConstants,
    private val neevaUser: NeevaUser,
    regularFaviconCache: RegularFaviconCache,
    settingsDataModel: SettingsDataModel,
    spaceStore: SpaceStore
) : BaseBrowserWrapper(
    isIncognito = false,
    appContext = appContext,
    coroutineScope = coroutineScope,
    dispatchers = dispatchers,
    activityCallbackProvider = activityCallbackProvider,
    suggestionsModel = SuggestionsModel(
        coroutineScope = coroutineScope,
        historyManager = historyManager,
        settingsDataModel = settingsDataModel,
        apolloWrapper = apolloWrapper,
        dispatchers = dispatchers,
        neevaConstants = neevaConstants,
        clientLogger = clientLogger
    ),
    faviconCache = regularFaviconCache,
    spaceStore = spaceStore,
    historyManager = historyManager,
    tabScreenshotManager = RegularTabScreenshotManager(
        filesDir = appContext.cacheDir,
        coroutineScope = coroutineScope,
        dispatchers = dispatchers
    ),
    domainProvider = domainProvider,
    neevaConstants = neevaConstants,
    settingsDataModel = settingsDataModel,
    trackerAllowList = RegularTrackersAllowList(
        hostInfoDao = hostInfoDao,
        coroutineScope = coroutineScope,
        dispatchers = dispatchers
    )
) {
    companion object {
        /**
         * NEVER change this value or you will cause WebLayer to use a different Profile and
         * (effectively) make all of their previous tabs inaccessible.
         */
        internal const val NON_INCOGNITO_PROFILE_NAME = "DefaultProfile"
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

    override fun registerBrowserCallbacks(browser: Browser): Boolean {
        val wasRegistered = super.registerBrowserCallbacks(browser)
        if (!wasRegistered) return false

        // Keep track of the user's login cookie, which we need for various operations.  These
        // actions should never be performed on the Incognito profile to avoid contaminating the
        // user's data with their Incognito history.
        browser.profile.cookieManager.apply {
            // Asynchronously parse out a pre-existing login cookie and store it locally.
            getCookiePairs(Uri.parse(neevaConstants.appURL)) { cookies ->
                cookies
                    .filter { it.key == neevaConstants.loginCookie }
                    .forEach { neevaUser.neevaUserToken.setToken(it.value) }
            }

            // If Neeva's login cookie changes, we need to save it and refetch the user's data.
            addCookieChangedCallback(
                Uri.parse(neevaConstants.appURL),
                neevaConstants.loginCookie,
                object : CookieChangedCallback() {
                    override fun onCookieChanged(cookie: String, cause: Int) {
                        val key = cookie.trim().substringBefore('=')
                        if (key != neevaConstants.loginCookie || !cookie.contains('=')) return

                        val value = cookie.trim().substringAfter('=')
                        neevaUser.neevaUserToken.setToken(value)
                        coroutineScope.launch(dispatchers.io) {
                            neevaUser.fetch(apolloWrapper)
                        }
                    }
                }
            )

            // If we have a cookie saved in the app, set the cookie in the Profile.  This handles
            // scenarios where the user logs via the app.
            if (neevaUser.neevaUserToken.getToken().isNotEmpty()) {
                setCookie(
                    Uri.parse(neevaConstants.appURL),
                    neevaUser.neevaUserToken.loginCookieString()
                ) {}
            }
        }

        return true
    }

    override fun onBlankTabCreated(tab: Tab) {
        // Direct the tab to go to the home page instead of leaving it on `about:blank`.
        tab.navigationController.navigate(Uri.parse(neevaConstants.homepageURL))
    }
}

package com.neeva.app.browsing

import android.content.Context
import android.net.Uri
import androidx.annotation.NonNull
import com.apollographql.apollo3.ApolloClient
import com.neeva.app.Dispatchers
import com.neeva.app.NeevaConstants
import com.neeva.app.NeevaConstants.browserTypeCookie
import com.neeva.app.NeevaConstants.browserVersionCookie
import com.neeva.app.history.HistoryManager
import com.neeva.app.publicsuffixlist.DomainProvider
import com.neeva.app.saveLoginCookieFrom
import com.neeva.app.spaces.SpaceStore
import com.neeva.app.storage.NeevaUser
import com.neeva.app.storage.RegularTabScreenshotManager
import com.neeva.app.storage.entities.Site
import com.neeva.app.storage.favicons.RegularFaviconCache
import com.neeva.app.suggestions.SuggestionsModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.chromium.weblayer.BrowsingDataType
import org.chromium.weblayer.CookieChangedCallback
import org.chromium.weblayer.CookieManager
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
    val apolloClient: ApolloClient,
    override val historyManager: HistoryManager,
    spaceStore: SpaceStore,
    val neevaUser: NeevaUser
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
        coroutineScope.launch(dispatchers.io) {
            urlBarModel.queryTextFlow.collectLatest { queryText ->
                // Pull new suggestions from the database.
                historyManager.updateSuggestionQuery(queryText)

                // Ask the backend for suggestions appropriate for the currently typed in text.
                suggestionsModel?.getSuggestionsFromBackend(queryText)
            }
        }

        urlBarModel.isEditing
            .onEach { isEditing -> if (isEditing) spaceStore.refresh() }
            .flowOn(dispatchers.io)
            .launchIn(coroutineScope)
    }

    override fun createBrowserFragment() =
        WebLayer.createBrowserFragment(NON_INCOGNITO_PROFILE_NAME, PERSISTENCE_ID)

    override fun createTabScreenshotManager() = RegularTabScreenshotManager(appContext.cacheDir)

    override fun registerBrowserCallbacks(): Boolean {
        val wasRegistered = super.registerBrowserCallbacks()
        if (!wasRegistered) return false

        browser?.profile?.cookieManager?.apply {
            getCookie(Uri.parse(NeevaConstants.appURL)) {
                it?.split(";")?.forEach { cookie ->
                    saveLoginCookieFrom(neevaUser.neevaUserToken, cookie.trim())
                }
            }

            addCookieChangedCallback(
                Uri.parse(NeevaConstants.appURL),
                NeevaConstants.loginCookie,
                object : CookieChangedCallback() {
                    override fun onCookieChanged(cookie: String, cause: Int) {
                        saveLoginCookieFrom(neevaUser.neevaUserToken, cookie)
                        coroutineScope.launch(dispatchers.io) {
                            neevaUser.fetch(apolloClient)
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

    override fun onAuthTokenUpdated() {
        browser?.profile?.cookieManager?.setCookie(
            Uri.parse(NeevaConstants.appURL),
            neevaUser.neevaUserToken.loginCookieString()
        ) { success ->
            if (success && activeTabModel.urlFlow.value.toString() == NeevaConstants.appURL) {
                activeTabModel.reload()
            }
        }
    }

    fun getCookies(cookieManager: CookieManager?, site: Site, callBack: (String) -> Unit) {
        cookieManager?.apply {
            getCookie(Uri.parse(site.siteURL)) { cookieString ->
                callBack(cookieString)
            }
        }
    }

    fun clearNonNeevaCookies(@NonNull @BrowsingDataType flags: IntArray) {
        val oldNeevaAuthToken = neevaUser.neevaUserToken.getToken()
        browser?.profile?.clearBrowsingData(flags) {
            browser?.profile?.cookieManager
                ?.setCookie(
                    Uri.parse(NeevaConstants.appURL),
                    "${NeevaConstants.loginCookie}=$oldNeevaAuthToken;",
                    null
                )
        }
        onAuthTokenUpdated()
    }

    fun clearNeevaCookies() {
//        browser?.profile?.cookieManager
//            ?.setCookie(
//                Uri.parse(NeevaConstants.appURL),
//                "${NeevaConstants.loginCookie}=;",
//                null
//            )
        neevaUser.neevaUserToken.removeToken()
        onAuthTokenUpdated()
    }
}

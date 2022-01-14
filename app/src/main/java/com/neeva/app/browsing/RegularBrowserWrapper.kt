package com.neeva.app.browsing

import android.app.Application
import android.net.Uri
import androidx.fragment.app.Fragment
import com.apollographql.apollo3.ApolloClient
import com.neeva.app.NeevaConstants
import com.neeva.app.User
import com.neeva.app.history.HistoryManager
import com.neeva.app.publicsuffixlist.DomainProviderImpl
import com.neeva.app.saveLoginCookieFrom
import com.neeva.app.storage.FaviconCache
import com.neeva.app.storage.SpaceStore
import com.neeva.app.suggestions.SuggestionsModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.chromium.weblayer.CookieChangedCallback
import org.chromium.weblayer.WebLayer

/**
 * Encapsulates logic for a regular browser profile, which tracks history and automatically fetches
 * suggestions from the backend as the user types out a query.
 */
class RegularBrowserWrapper(
    appContext: Application,
    activityCallbackProvider: () -> ActivityCallbacks?,
    domainProviderImpl: DomainProviderImpl,
    apolloClient: ApolloClient,
    override val historyManager: HistoryManager,
    override val faviconCache: FaviconCache,
    spaceStore: SpaceStore,
    coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) : BrowserWrapper(
    isIncognito = false,
    appContext,
    activityCallbackProvider,
    coroutineScope
) {
    companion object {
        private const val NON_INCOGNITO_PROFILE_NAME = "DefaultProfile"
        private const val PERSISTENCE_ID = "Neeva_Browser"
    }

    override val suggestionsModel = SuggestionsModel(
        coroutineScope,
        historyManager,
        urlBarModel,
        apolloClient,
        domainProviderImpl
    )

    init {
        // Pull new suggestions from the database according to what's currently in the URL bar.
        coroutineScope.launch {
            urlBarModel.userInputText.collect {
                historyManager.updateSuggestionQuery(coroutineScope, it.text)
            }
        }

        coroutineScope.launch {
            urlBarModel.isEditing.collect { isEditing ->
                if (isEditing) spaceStore.refresh()
            }
        }
    }

    override fun createBrowserFragment(): Fragment {
        return WebLayer.createBrowserFragment(NON_INCOGNITO_PROFILE_NAME, PERSISTENCE_ID)
    }

    override fun createTabScreenshotManager() = TabScreenshotManager(appContext.cacheDir)

    override fun registerBrowserCallbacks(): Boolean {
        val wasRegistered = super.registerBrowserCallbacks()
        if (!wasRegistered) return false

        browser?.profile?.cookieManager?.apply {
            getCookie(Uri.parse(NeevaConstants.appURL)) {
                it?.split("; ")?.forEach { cookie ->
                    saveLoginCookieFrom(appContext, cookie)
                }
            }

            addCookieChangedCallback(
                Uri.parse(NeevaConstants.appURL),
                NeevaConstants.loginCookie,
                object : CookieChangedCallback() {
                    override fun onCookieChanged(cookie: String, cause: Int) {
                        saveLoginCookieFrom(appContext, cookie)
                    }
                }
            )

            if (!User.getToken(appContext).isNullOrEmpty()) {
                setCookie(
                    Uri.parse(NeevaConstants.appURL),
                    User.loginCookieString(appContext)
                ) {}
            }
        }

        return true
    }

    override fun onAuthTokenUpdated() {
        browser?.profile?.cookieManager?.setCookie(
            Uri.parse(NeevaConstants.appURL),
            User.loginCookieString(appContext)
        ) { success ->
            if (success && activeTabModel.urlFlow.value.toString() == NeevaConstants.appURL) {
                activeTabModel.reload()
            }
        }
    }
}

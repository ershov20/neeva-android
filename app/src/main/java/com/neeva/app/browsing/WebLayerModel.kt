package com.neeva.app.browsing

import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import com.apollographql.apollo3.ApolloClient
import com.neeva.app.Dispatchers
import com.neeva.app.LoadingState
import com.neeva.app.NeevaUserToken
import com.neeva.app.history.HistoryManager
import com.neeva.app.publicsuffixlist.DomainProviderImpl
import com.neeva.app.settings.SettingsToggle
import com.neeva.app.spaces.SpaceStore
import com.neeva.app.storage.favicons.FaviconCache
import java.lang.ref.WeakReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.chromium.weblayer.Browser
import org.chromium.weblayer.BrowsingDataType
import org.chromium.weblayer.Tab
import org.chromium.weblayer.UnsupportedVersionException
import org.chromium.weblayer.WebLayer

/**
 * Manages and maintains the interface between the Neeva browser and WebLayer.
 *
 * The WebLayer [Browser] maintains a set of [Tab]s that we are supposed to keep track of.  These
 * classes must be monitored using various callbacks that fire whenever a new tab is opened, or
 * whenever the current tab changes (e.g.).
 */
class WebLayerModel(
    private val appContext: Context,
    private val domainProviderImpl: DomainProviderImpl,
    private val historyManager: HistoryManager,
    apolloClient: ApolloClient,
    spaceStore: SpaceStore,
    private val coroutineScope: CoroutineScope,
    private val dispatchers: Dispatchers,
    neevaUserToken: NeevaUserToken
) {
    companion object {
        val TAG = WebLayerModel::class.simpleName
    }

    var activityCallbacks: WeakReference<ActivityCallbacks> = WeakReference(null)

    private val _webLayer = MutableStateFlow<WebLayer?>(null)

    private val regularBrowser = RegularBrowserWrapper(
        appContext = appContext,
        coroutineScope = coroutineScope,
        dispatchers = dispatchers,
        activityCallbackProvider = { activityCallbacks.get() },
        domainProvider = domainProviderImpl,
        apolloClient = apolloClient,
        historyManager = historyManager,
        spaceStore = spaceStore,
        neevaUserToken = neevaUserToken
    )
    private var incognitoBrowser: IncognitoBrowserWrapper? = null

    /** Keeps track of the initialization pipeline. */
    private val internalInitializationState = MutableStateFlow(LoadingState.UNINITIALIZED)
    val initializationState: StateFlow<LoadingState> =
        internalInitializationState
            .combine(_webLayer) { internalState, webLayer ->
                LoadingState.from(
                    internalState,
                    if (webLayer == null) LoadingState.LOADING else LoadingState.READY
                )
            }
            .stateIn(coroutineScope, SharingStarted.Lazily, LoadingState.LOADING)

    private val _browserWrapperFlow = MutableStateFlow<BrowserWrapper>(regularBrowser)
    val browserWrapperFlow: StateFlow<BrowserWrapper> = _browserWrapperFlow
    val currentBrowser
        get() = browserWrapperFlow.value

    init {
        coroutineScope.launch(dispatchers.io) {
            domainProviderImpl.initialize()
            CacheCleaner(appContext.cacheDir).run()
            internalInitializationState.value = LoadingState.READY
        }

        try {
            WebLayer.loadAsync(appContext) { webLayer ->
                webLayer.isRemoteDebuggingEnabled = true
                regularBrowser.initialize()
                incognitoBrowser?.initialize()
                _webLayer.value = webLayer
            }
        } catch (e: UnsupportedVersionException) {
            throw RuntimeException("Failed to initialize WebLayer, e")
        }
    }

    /**
     * Prepares the WebLayerModel to interface with the Browser.  Note that this is triggered every
     * time the Activity is recreated, which includes when the screen is rotated.  This means that
     * you should guard against two different instances of the same observer and or callback from
     * being registered.
     */
    fun onWebLayerReady(
        topControlsPlaceholder: View,
        bottomControlsPlaceholder: View,
        fragmentAttacher: (Fragment, Boolean) -> Unit
    ) {
        currentBrowser.createAndAttachBrowser(fragmentAttacher)

        currentBrowser.prepareBrowser(
            topControlsPlaceholder,
            bottomControlsPlaceholder
        )
    }

    fun onAuthTokenUpdated() = currentBrowser.onAuthTokenUpdated()

    /** Loads the given [url] in a new tab. */
    fun loadUrl(url: Uri) = currentBrowser.activeTabModel.loadUrl(url, true)

    fun switchToProfile(useIncognito: Boolean) {
        if (currentBrowser.isIncognito == useIncognito) return

        _browserWrapperFlow.value = if (useIncognito) {
            val delegate = incognitoBrowser ?: IncognitoBrowserWrapper(
                appContext = appContext,
                coroutineScope = coroutineScope,
                dispatchers = dispatchers,
                activityCallbackProvider = activityCallbacks::get,
                domainProvider = domainProviderImpl,
                onDestroyed = {
                    incognitoBrowser = null
                    switchToProfile(useIncognito = false)
                }
            ).also { it.initialize() }

            incognitoBrowser = delegate
            delegate
        } else {
            // Delete incognito if there's no reason to keep it around.
            if (incognitoBrowser?.hasNoTabs() == true) {
                Log.d(TAG, "Culling unnecessary incognito profile")
                activityCallbacks.get()?.detachIncognitoFragment()
                incognitoBrowser = null
            }

            regularBrowser
        }
    }

    fun clearNeevaCookies() = regularBrowser::clearNeevaCookies

    fun clearBrowsingData(clearingOptions: MutableMap<String, Boolean>) {
        val clearCookiesFlags = mutableListOf<Int>()
        clearingOptions.keys.forEach {
            if (clearingOptions[it] == true) {
                when (it) {
                    SettingsToggle.CLEAR_BROWSING_HISTORY.key -> {
                        historyManager.clearAllHistory()
                    }
                    SettingsToggle.CLEAR_BROWSING_TRACKING_PROTECTION.key -> {
                    }
                    SettingsToggle.CLEAR_DOWNLOADED_FILES.key -> {
                    }
                    SettingsToggle.CLEAR_COOKIES.key -> {
                        clearCookiesFlags.add(BrowsingDataType.COOKIES_AND_SITE_DATA)
                    }
                    SettingsToggle.CLEAR_CACHE.key -> {
                        clearCookiesFlags.add(BrowsingDataType.CACHE)
                    }
                    else -> { }
                    // TODO(kobec): finish this for the other parameters
                }
            }
        }
        if (clearCookiesFlags.size > 0) {
            regularBrowser.clearNonNeevaCookies(clearCookiesFlags.toIntArray())
        }
    }

    fun getRegularProfileFaviconCache(): FaviconCache = regularBrowser.faviconCache
}

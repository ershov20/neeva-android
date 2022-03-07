package com.neeva.app.browsing

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.neeva.app.ApolloWrapper
import com.neeva.app.Dispatchers
import com.neeva.app.LoadingState
import com.neeva.app.NeevaConstants
import com.neeva.app.history.HistoryManager
import com.neeva.app.logging.ClientLogger
import com.neeva.app.publicsuffixlist.DomainProviderImpl
import com.neeva.app.settings.SettingsToggle
import com.neeva.app.spaces.SpaceStore
import com.neeva.app.storage.favicons.FaviconCache
import com.neeva.app.userdata.NeevaUser
import dagger.hilt.android.lifecycle.HiltViewModel
import java.lang.ref.WeakReference
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.chromium.weblayer.Browser
import org.chromium.weblayer.BrowsingDataType
import org.chromium.weblayer.Profile
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
@HiltViewModel
class WebLayerModel @Inject constructor(
    application: Application,
    private val domainProviderImpl: DomainProviderImpl,
    private val historyManager: HistoryManager,
    private val apolloWrapper: ApolloWrapper,
    spaceStore: SpaceStore,
    private val dispatchers: Dispatchers,
    private val neevaUser: NeevaUser,
    private val clientLogger: ClientLogger
) : AndroidViewModel(application) {
    companion object {
        val TAG = WebLayerModel::class.simpleName
    }

    var activityCallbacks: WeakReference<ActivityCallbacks> = WeakReference(null)

    private val webLayer = MutableStateFlow<WebLayer?>(null)

    private val regularBrowser = RegularBrowserWrapper(
        appContext = application,
        coroutineScope = viewModelScope,
        dispatchers = dispatchers,
        activityCallbackProvider = { activityCallbacks.get() },
        domainProvider = domainProviderImpl,
        apolloWrapper = apolloWrapper,
        historyManager = historyManager,
        spaceStore = spaceStore,
        neevaUser = neevaUser,
        clientLogger = clientLogger
    )
    private var incognitoBrowser: IncognitoBrowserWrapper? = null
    private lateinit var regularProfile: Profile

    /** Keeps track of the non-WebLayer initialization pipeline. */
    private val internalInitializationState = MutableStateFlow(LoadingState.UNINITIALIZED)

    /** Keeps track of when everything is ready to use. */
    val initializationState: StateFlow<LoadingState> =
        internalInitializationState
            .combine(webLayer) { internalState, webLayer ->
                LoadingState.from(
                    internalState,
                    if (webLayer == null) LoadingState.LOADING else LoadingState.READY
                )
            }
            .stateIn(viewModelScope, SharingStarted.Lazily, LoadingState.LOADING)

    private val _browserWrapperFlow = MutableStateFlow<BrowserWrapper>(regularBrowser)
    val browserWrapperFlow: StateFlow<BrowserWrapper> get() = _browserWrapperFlow
    val currentBrowser get() = browserWrapperFlow.value

    /** Tracks the current BrowserWrapper, emitting them only while the pipeline is initialized. */
    val initializedBrowserFlow: Flow<BrowserWrapper> =
        initializationState
            .filter { it == LoadingState.READY }
            .combine(_browserWrapperFlow) { _, browserWrapper -> browserWrapper }

    init {
        viewModelScope.launch(dispatchers.io) {
            domainProviderImpl.initialize()
            CacheCleaner(application.cacheDir).run()
            internalInitializationState.value = LoadingState.READY
        }

        try {
            WebLayer.loadAsync(application) { webLayer ->
                webLayer.isRemoteDebuggingEnabled = true
                regularProfile = RegularBrowserWrapper.getProfile(webLayer)
                this.webLayer.value = webLayer
            }
        } catch (e: UnsupportedVersionException) {
            throw RuntimeException("Failed to initialize WebLayer", e)
        }
    }

    /**
     * Switches the user to the specified profile.
     *
     * If the user is trying to switch to Incognito after the Browser has been destroyed, it creates
     * a new one on the fly.  If the user is trying to switch back to regular mode after closing all
     * of their Incognito tabs, the Incognito profile is destroyed.
     */
    fun switchToProfile(useIncognito: Boolean) {
        if (currentBrowser.isIncognito == useIncognito) return

        _browserWrapperFlow.value = if (useIncognito) {
            val delegate = incognitoBrowser ?: IncognitoBrowserWrapper(
                appContext = getApplication(),
                coroutineScope = viewModelScope,
                dispatchers = dispatchers,
                activityCallbackProvider = activityCallbacks::get,
                apolloWrapper = apolloWrapper,
                domainProvider = domainProviderImpl,
                onDestroyed = {
                    incognitoBrowser = null
                    switchToProfile(useIncognito = false)
                }
            )

            incognitoBrowser = delegate
            delegate
        } else {
            // Delete the Incognito profile if the user has closed all of their tabs.
            if (incognitoBrowser?.hasNoTabs() == true) {
                Log.d(TAG, "Culling unnecessary incognito profile")
                activityCallbacks.get()?.removeIncognitoFragment()
                incognitoBrowser = null
            }

            regularBrowser
        }
    }

    fun onAuthTokenUpdated() {
        regularProfile.cookieManager.setCookie(
            Uri.parse(NeevaConstants.appURL),
            neevaUser.neevaUserToken.loginCookieString()
        ) { success ->
            val currentUrl = regularBrowser.activeTabModel.urlFlow.value
            if (success && currentUrl.toString().startsWith(NeevaConstants.appURL)) {
                regularBrowser.reload()
            }
        }
    }

    fun clearNeevaCookies() {
        neevaUser.neevaUserToken.removeToken()
        onAuthTokenUpdated()
        regularBrowser.reload()
    }

    fun clearNonNeevaCookies(clearCookiesFlags: List<Int>) {
        if (clearCookiesFlags.isNotEmpty()) {
            val oldNeevaAuthToken = neevaUser.neevaUserToken.getToken()
            regularProfile.clearBrowsingData(clearCookiesFlags.toIntArray()) {
                regularProfile.cookieManager.setCookie(
                    Uri.parse(NeevaConstants.appURL),
                    "${NeevaConstants.loginCookie}=$oldNeevaAuthToken;",
                    null
                )
            }
            onAuthTokenUpdated()
        }
    }

    fun clearBrowsingData(clearingOptions: Map<String, Boolean>) {
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
        clearNonNeevaCookies(clearCookiesFlags)
    }

    fun getRegularProfileFaviconCache(): FaviconCache = regularBrowser.faviconCache
}

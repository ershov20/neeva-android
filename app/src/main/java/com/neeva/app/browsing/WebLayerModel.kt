package com.neeva.app.browsing

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.neeva.app.Dispatchers
import com.neeva.app.LoadingState
import com.neeva.app.NeevaConstants
import com.neeva.app.history.HistoryManager
import com.neeva.app.logging.ClientLogger
import com.neeva.app.publicsuffixlist.DomainProviderImpl
import com.neeva.app.settings.SettingsDataModel
import com.neeva.app.settings.SettingsToggle
import com.neeva.app.storage.favicons.FaviconCache
import com.neeva.app.userdata.NeevaUser
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.chromium.weblayer.Browser
import org.chromium.weblayer.BrowsingDataType
import org.chromium.weblayer.Profile
import org.chromium.weblayer.Tab
import org.chromium.weblayer.UnsupportedVersionException
import org.chromium.weblayer.WebLayer

data class BrowsersState(
    val regularBrowser: RegularBrowserWrapper,
    val incognitoBrowserWrapper: IncognitoBrowserWrapper?,
    val isCurrentlyIncognito: Boolean = false
) {
    fun getCurrentBrowser(): BrowserWrapper {
        return if (isCurrentlyIncognito) {
            incognitoBrowserWrapper ?: throw IllegalStateException()
        } else {
            regularBrowser
        }
    }
}

/**
 * Manages and maintains the interface between the Neeva browser and WebLayer.
 *
 * The WebLayer [Browser] maintains a set of [Tab]s that we are supposed to keep track of.  These
 * classes must be monitored using various callbacks that fire whenever a new tab is opened, or
 * whenever the current tab changes (e.g.).
 */
@HiltViewModel
class WebLayerModel internal constructor(
    private val activityCallbackProvider: ActivityCallbackProvider,
    private val browserWrapperFactory: BrowserWrapperFactory,
    webLayerFactory: WebLayerFactory,
    application: Application,
    private val cacheCleaner: CacheCleaner,
    private val domainProviderImpl: DomainProviderImpl,
    private val historyManager: HistoryManager,
    private val dispatchers: Dispatchers,
    private val neevaUser: NeevaUser,
    private val settingsDataModel: SettingsDataModel,
    private val clientLogger: ClientLogger,
    overrideCoroutineScope: CoroutineScope?
) : AndroidViewModel(application) {
    @Inject constructor(
        activityCallbackProvider: ActivityCallbackProvider,
        browserWrapperFactory: BrowserWrapperFactory,
        webLayerFactory: WebLayerFactory,
        application: Application,
        cacheCleaner: CacheCleaner,
        domainProviderImpl: DomainProviderImpl,
        historyManager: HistoryManager,
        dispatchers: Dispatchers,
        neevaUser: NeevaUser,
        settingsDataModel: SettingsDataModel,
        clientLogger: ClientLogger
    ) : this(
        activityCallbackProvider = activityCallbackProvider,
        browserWrapperFactory = browserWrapperFactory,
        webLayerFactory = webLayerFactory,
        application = application,
        cacheCleaner = cacheCleaner,
        domainProviderImpl = domainProviderImpl,
        historyManager = historyManager,
        dispatchers = dispatchers,
        neevaUser = neevaUser,
        settingsDataModel = settingsDataModel,
        clientLogger = clientLogger,
        overrideCoroutineScope = null
    )

    companion object {
        val TAG = WebLayerModel::class.simpleName
    }

    private val coroutineScope = overrideCoroutineScope ?: viewModelScope
    private val webLayer = MutableStateFlow<WebLayer?>(null)

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
            .stateIn(coroutineScope, SharingStarted.Eagerly, LoadingState.LOADING)

    private lateinit var regularProfile: Profile

    private val regularBrowser: RegularBrowserWrapper =
        browserWrapperFactory.createRegularBrowser(coroutineScope = coroutineScope)

    private var incognitoBrowser: IncognitoBrowserWrapper? = null
        set(value) {
            field = value
            _browsersFlow.value = _browsersFlow.value.copy(incognitoBrowserWrapper = value)
        }
    private val _browsersFlow = MutableStateFlow(BrowsersState(regularBrowser, incognitoBrowser))

    /** Keeps track of the current BrowserWrappers for the regular and incognito profiles. */
    val browsersFlow: StateFlow<BrowsersState> get() = _browsersFlow

    /** Allows consumers to keep track of which BrowserWrapper is currently active. */
    val currentBrowserFlow: StateFlow<BrowserWrapper> = _browsersFlow
        .map { it.getCurrentBrowser() }
        .distinctUntilChanged()
        .stateIn(coroutineScope, SharingStarted.Eagerly, regularBrowser)

    /** Returns the currently active BrowserWrapper. */
    val currentBrowser get() = currentBrowserFlow.value

    /** Tracks the current BrowserWrapper, emitting them only while the pipeline is initialized. */
    val initializedBrowserFlow: Flow<BrowserWrapper> =
        initializationState
            .filter { it == LoadingState.READY }
            .combine(currentBrowserFlow) { _, browserWrapper -> browserWrapper }

    init {
        coroutineScope.launch(dispatchers.io) {
            domainProviderImpl.initialize()
            historyManager.pruneDatabase()
            internalInitializationState.value = LoadingState.READY
        }

        try {
            webLayerFactory.load { webLayer ->
                webLayer.isRemoteDebuggingEnabled = true
                regularProfile = RegularBrowserWrapper.getProfile(webLayer)

                activityCallbackProvider.get()?.getWebLayerFragment(isIncognito = true)?.let {
                    incognitoBrowser = createIncognitoBrowserWrapper()
                }

                coroutineScope.launch {
                    if (incognitoBrowser == null) {
                        // Clean up any previous incarnations of the Incognito profile.
                        val incognitoProfile = IncognitoBrowserWrapper.getProfile(webLayer)
                        IncognitoBrowserWrapper.cleanUpIncognito(
                            dispatchers = dispatchers,
                            incognitoProfile = incognitoProfile,
                            cacheCleaner = cacheCleaner
                        )
                    }

                    withContext(dispatchers.main) {
                        // Let the rest of the app know that WebLayer is ready to use.
                        this@WebLayerModel.webLayer.value = webLayer
                    }
                }
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
        // Make sure that the ClientLogger is disabled when the user enters Incognito.
        clientLogger.onProfileSwitch(useIncognito)

        if (_browsersFlow.value.isCurrentlyIncognito == useIncognito) return

        if (useIncognito) {
            if (incognitoBrowser == null) {
                incognitoBrowser = createIncognitoBrowserWrapper()
            }
        } else {
            val closeIncognitoTabsOnScreenSwitch =
                settingsDataModel.getSettingsToggleValue(SettingsToggle.CLOSE_INCOGNITO_TABS)
            if (closeIncognitoTabsOnScreenSwitch) {
                incognitoBrowser?.closeAllTabs()
            }
        }

        _browsersFlow.value = _browsersFlow.value.copy(isCurrentlyIncognito = useIncognito)
    }

    private fun createIncognitoBrowserWrapper() = browserWrapperFactory.createIncognitoBrowser(
        coroutineScope = coroutineScope,
        onRemovedFromHierarchy = {
            // Because this is asynchronous, make sure that the destroyed one is the one we are
            // currently tracking.
            if (it != incognitoBrowser) return@createIncognitoBrowser

            incognitoBrowser = null
            switchToProfile(useIncognito = false)
        }
    )

    /** Delete the Incognito profile if the user has closed all of their tabs. */
    fun deleteIncognitoProfileIfUnused() {
        if (!currentBrowser.isIncognito && incognitoBrowser?.hasNoTabs() == true) {
            Log.d(TAG, "Culling unnecessary incognito profile")
            activityCallbackProvider.get()?.removeIncognitoFragment()
            incognitoBrowser?.destroyProfile()
            incognitoBrowser = null
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

    fun clearNonNeevaCookies(clearCookiesFlags: List<Int>, fromMillis: Long, toMillis: Long) {
        if (clearCookiesFlags.isNotEmpty()) {
            val oldNeevaAuthToken = neevaUser.neevaUserToken.getToken()

            regularProfile.clearBrowsingData(
                clearCookiesFlags.toIntArray(),
                fromMillis,
                toMillis
            ) {
                // Since all cookies got cleared, add back the original Neeva Cookie after it finished clearing.
                regularProfile.cookieManager.setCookie(
                    Uri.parse(NeevaConstants.appURL),
                    "${NeevaConstants.loginCookie}=$oldNeevaAuthToken;",
                    null
                )
            }
            onAuthTokenUpdated()
        }
    }

    fun clearBrowsingData(
        clearingOptions: Map<String, Boolean>,
        fromMillis: Long,
        toMillis: Long
    ) {
        val clearCookiesFlags = mutableListOf<Int>()
        clearingOptions.keys.forEach {
            if (clearingOptions[it] == true) {
                when (it) {
                    SettingsToggle.CLEAR_BROWSING_HISTORY.key -> {
                        historyManager.clearHistory(fromMillis)
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
        clearNonNeevaCookies(clearCookiesFlags, fromMillis, toMillis)
    }

    fun getRegularProfileFaviconCache(): FaviconCache = regularBrowser.faviconCache
}

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
import com.neeva.app.sharedprefs.SharedPrefFolder
import com.neeva.app.sharedprefs.SharedPreferencesModel
import com.neeva.app.storage.favicons.FaviconCache
import com.neeva.app.userdata.NeevaUser
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.chromium.weblayer.Browser
import org.chromium.weblayer.BrowsingDataType
import org.chromium.weblayer.Profile
import org.chromium.weblayer.Tab
import org.chromium.weblayer.UnsupportedVersionException
import org.chromium.weblayer.WebLayer

data class BrowsersState(
    val regularBrowserWrapper: RegularBrowserWrapper,
    val incognitoBrowserWrapper: IncognitoBrowserWrapper?,
    val isCurrentlyIncognito: Boolean = false
) {
    fun getCurrentBrowser(): BrowserWrapper {
        return if (isCurrentlyIncognito) {
            incognitoBrowserWrapper ?: throw IllegalStateException()
        } else {
            regularBrowserWrapper
        }
    }
}

enum class WebLayerPrefs {
    /**
     * Boolean: Tracks whether the user is using the Regular or Incognito profile.  Meant to be read
     * only during WebLayerModel initialization.
     */
    IsCurrentlyIncognito
}

/**
 * Manages and maintains the interface between the Neeva browser and WebLayer.
 *
 * ## Tabs
 * The WebLayer [Browser] maintains a set of [Tab]s that we are supposed to keep track of.  These
 * classes must be monitored using various callbacks that fire whenever a new tab is opened, or
 * whenever the current tab changes (e.g.).
 *
 * ## Fragments
 * WebLayer uses Fragments to control its logic and store all of its Browser state.  These Fragments
 * are attached to the Activity via the [ActivityCallbacks], and persist after the app is restarted
 * because we explicitly set Fragment.retainInstance = true.  The main benefit to using that flag is
 * that the same Fragment is re-used if the Activity is recreated after a temporary configuration
 * change -- if the user fullscreens a video and ends up in landscape, the video continues playing
 * after the Activity is recreated in the new state.
 *
 * Another effect of retaining the Fragment is that WebLayer stores the encryption key for the
 * Incognito state as part of the Fragment's state.  If the app died in the background, the
 * Fragments will be automatically reattached to the new Activity when it is restarted, allowing
 * WebLayer to access the encryption key.  If the Activity was killed (e.g. by the user swiping away
 * the task in Android's recents menu), the saved instance state is all lost -- along with the
 * Incognito encryption key.
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
    private val sharedPreferencesModel: SharedPreferencesModel,
    private val neevaConstants: NeevaConstants,
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
        sharedPreferencesModel: SharedPreferencesModel,
        neevaConstants: NeevaConstants,
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
        sharedPreferencesModel = sharedPreferencesModel,
        settingsDataModel = settingsDataModel,
        clientLogger = clientLogger,
        overrideCoroutineScope = null,
        neevaConstants = neevaConstants
    )

    companion object {
        val TAG = WebLayerModel::class.simpleName
    }

    /** [CoroutineScope] that should be used for all Flows.  May be overridden by tests. */
    private val coroutineScope = overrideCoroutineScope ?: viewModelScope

    private val webLayer = MutableStateFlow<WebLayer?>(null)

    /** Keeps track of when everything is ready to use. */
    val initializationState = MutableStateFlow(LoadingState.LOADING)

    private val _browsersFlow: MutableStateFlow<BrowsersState> = MutableStateFlow(
        BrowsersState(
            regularBrowserWrapper = browserWrapperFactory.createRegularBrowser(coroutineScope),
            incognitoBrowserWrapper = null,
            isCurrentlyIncognito = false
        )
    )

    /** Keeps track of the current BrowserWrappers for the regular and incognito profiles. */
    val browsersFlow: StateFlow<BrowsersState> get() = _browsersFlow

    /** Returns the [BrowserWrapper] associated with the regular profile. */
    private val regularBrowser
        get() = _browsersFlow.value.regularBrowserWrapper

    /** Returns the [BrowserWrapper] associated with the Incognito profile, if one exists. */
    private val incognitoBrowser: IncognitoBrowserWrapper?
        get() = _browsersFlow.value.incognitoBrowserWrapper

    /** Returns the currently active BrowserWrapper. */
    val currentBrowser get() = browsersFlow.value.getCurrentBrowser()

    private lateinit var regularProfile: Profile

    /** Tracks the current BrowserWrapper, emitting them only while the pipeline is initialized. */
    val initializedBrowserFlow: Flow<BrowserWrapper> =
        initializationState
            .filter { it == LoadingState.READY }
            .combine(browsersFlow) { _, browsers -> browsers.getCurrentBrowser() }
            .distinctUntilChanged()

    init {
        val webLayerDeferred = CompletableDeferred<WebLayer>()
        try {
            webLayerFactory.load { webLayer -> webLayerDeferred.complete(webLayer) }
        } catch (e: UnsupportedVersionException) {
            throw RuntimeException("Failed to initialize WebLayer", e)
        }

        coroutineScope.launch(dispatchers.main) {
            // Initialize our internal classes while WebLayer is loading.
            withContext(dispatchers.io) {
                domainProviderImpl.initialize()
                historyManager.pruneDatabase()
            }

            // Wait for WebLayer to finish initialization.
            val webLayer = webLayerDeferred.await()
            webLayer.isRemoteDebuggingEnabled = true
            regularProfile = RegularBrowserWrapper.getProfile(webLayer)

            restoreIncognitoState(webLayer)

            // Let the rest of the app know that WebLayer is ready to use.
            this@WebLayerModel.webLayer.value = webLayer

            initializationState.value = LoadingState.READY
        }
    }

    private suspend fun restoreIncognitoState(webLayer: WebLayer) {
        // When the app dies in the background, WebLayer's Fragments are automatically recreated and
        // reattached to the Activity.
        val incognitoFragmentExists =
            activityCallbackProvider.get()?.getWebLayerFragment(isIncognito = true) != null
        if (!incognitoFragmentExists) {
            // There's no state to restore and the Incognito encryption key (if there was one) has
            // been lost.  Send the user to the regular profile and clean up.
            switchToProfile(useIncognito = false)
            cleanUpIncognito(webLayer)
            return
        }

        // Recreate the IncognitoBrowserWrapper using the old Fragment.
        _browsersFlow.value = _browsersFlow.value.copy(
            incognitoBrowserWrapper = createIncognitoBrowserWrapper()
        )

        val userWasIncognitoWhenAppDied = sharedPreferencesModel.getValue(
            folder = SharedPrefFolder.WEBLAYER,
            key = WebLayerPrefs.IsCurrentlyIncognito.name,
            defaultValue = false
        )
        switchToProfile(useIncognito = userWasIncognitoWhenAppDied)
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

        // Keep track of what mode the user entered so that we can send them back there after the
        // app restarts.
        sharedPreferencesModel.setValue(
            folder = SharedPrefFolder.WEBLAYER,
            key = WebLayerPrefs.IsCurrentlyIncognito.name,
            value = useIncognito
        )

        if (_browsersFlow.value.isCurrentlyIncognito == useIncognito) return

        // Start a "transaction" that avoids triggering Flow collectors until we have determined all
        // the values we want to update.
        var newValue = _browsersFlow.value.copy(isCurrentlyIncognito = useIncognito)

        if (useIncognito) {
            if (newValue.incognitoBrowserWrapper == null) {
                newValue = newValue.copy(
                    incognitoBrowserWrapper = createIncognitoBrowserWrapper()
                )
            }
        } else {
            // Check for an Incognito Fragment instead of the IncognitoBrowserWrapper because that
            // is a better signal that we have a profile to clean up.
            val incognitoFragmentExists =
                activityCallbackProvider.get()?.getWebLayerFragment(isIncognito = true) != null

            if ((incognitoFragmentExists && shouldDestroyIncognitoOnSwitch()) ||
                newValue.incognitoBrowserWrapper?.hasNoTabs() == true
            ) {
                coroutineScope.launch {
                    webLayer.value?.let { cleanUpIncognito(it) }
                }

                activityCallbackProvider.get()?.removeIncognitoFragment()
                newValue = newValue.copy(incognitoBrowserWrapper = null)
            }
        }

        _browsersFlow.value = newValue
    }

    private fun createIncognitoBrowserWrapper() = browserWrapperFactory.createIncognitoBrowser(
        coroutineScope = coroutineScope,
        onRemovedFromHierarchy = {
            // This can get called if the app is shutting down in the background, so we can't
            // rely on it as a signal for cleaning up the Incognito profile data.
            //
            // Because this is asynchronous, make sure that the destroyed one is the one we
            // are currently tracking.
            if (it == incognitoBrowser) {
                _browsersFlow.value = _browsersFlow.value.copy(incognitoBrowserWrapper = null)
            }
        }
    )

    private suspend fun cleanUpIncognito(webLayer: WebLayer) {
        Log.d(TAG, "Cleaning up incognito profile")
        val incognitoProfile = IncognitoBrowserWrapper.getProfile(webLayer)
        IncognitoBrowserWrapper.cleanUpIncognito(
            dispatchers = dispatchers,
            incognitoProfile = incognitoProfile,
            cacheCleaner = cacheCleaner
        )
    }

    fun onAuthTokenUpdated() {
        regularProfile.cookieManager.setCookie(
            Uri.parse(neevaConstants.appURL),
            neevaUser.neevaUserToken.loginCookieString()
        ) { success ->
            val currentUrl = regularBrowser.activeTabModel.urlFlow.value
            if (success && currentUrl.toString().startsWith(neevaConstants.appURL)) {
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
                    Uri.parse(neevaConstants.appURL),
                    "${neevaConstants.loginCookie}=$oldNeevaAuthToken;",
                    null
                )
            }
            onAuthTokenUpdated()
        }
    }

    fun clearBrowsingData(
        clearingOptions: Map<SettingsToggle, Boolean>,
        fromMillis: Long,
        toMillis: Long
    ) {
        val clearCookiesFlags = mutableListOf<Int>()
        clearingOptions
            .filter { it.value }
            .forEach { (toggle, isEnabled) ->
                when (toggle) {
                    SettingsToggle.CLEAR_BROWSING_HISTORY -> {
                        historyManager.clearHistory(fromMillis)
                    }
                    SettingsToggle.CLEAR_BROWSING_TRACKING_PROTECTION -> {
                    }
                    SettingsToggle.CLEAR_DOWNLOADED_FILES -> {
                    }
                    SettingsToggle.CLEAR_COOKIES -> {
                        clearCookiesFlags.add(BrowsingDataType.COOKIES_AND_SITE_DATA)
                    }
                    SettingsToggle.CLEAR_CACHE -> {
                        clearCookiesFlags.add(BrowsingDataType.CACHE)
                    }
                    else -> { }
                    // TODO(kobec): finish this for the other parameters
                }
            }
        clearNonNeevaCookies(clearCookiesFlags, fromMillis, toMillis)
    }

    fun getRegularProfileFaviconCache(): FaviconCache = regularBrowser.faviconCache

    fun updateBrowsersCookieCutterConfig() {
        regularBrowser.updateCookieCutterConfigAndRefreshTabs()
        incognitoBrowser?.updateCookieCutterConfigAndRefreshTabs()
    }

    private fun shouldDestroyIncognitoOnSwitch(): Boolean {
        return settingsDataModel.getSettingsToggleValue(SettingsToggle.CLOSE_INCOGNITO_TABS)
    }
}

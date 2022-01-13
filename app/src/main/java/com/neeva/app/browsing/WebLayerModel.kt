package com.neeva.app.browsing

import android.app.Application
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apollographql.apollo3.ApolloClient
import com.neeva.app.LoadingState
import com.neeva.app.history.HistoryManager
import com.neeva.app.publicsuffixlist.DomainProviderImpl
import com.neeva.app.storage.FaviconCache
import com.neeva.app.storage.SpaceStore
import dagger.hilt.android.lifecycle.HiltViewModel
import java.lang.ref.WeakReference
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.chromium.weblayer.Browser
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
    private val appContext: Application,
    private val domainProviderImpl: DomainProviderImpl,
    historyManager: HistoryManager,
    apolloClient: ApolloClient,
    faviconCache: FaviconCache,
    spaceStore: SpaceStore
) : ViewModel() {
    companion object {
        val TAG = WebLayerModel::class.simpleName
    }

    var activityCallbacks: WeakReference<ActivityCallbacks> = WeakReference(null)

    private val _webLayer = MutableStateFlow<WebLayer?>(null)

    private val regularBrowser = RegularBrowserWrapper(
        appContext = appContext,
        activityCallbackProvider = { activityCallbacks.get() },
        domainProviderImpl = domainProviderImpl,
        apolloClient = apolloClient,
        historyManager = historyManager,
        faviconCache = faviconCache,
        spaceStore = spaceStore,
        coroutineScope = viewModelScope
    )
    private var incognitoBrowser: IncognitoBrowserWrapper? = null

    /** Keeps track of the initialization pipeline. */
    private val internalInitializationState = MutableStateFlow(LoadingState.UNINITIALIZED)
    val initializationState: StateFlow<LoadingState> =
        internalInitializationState.combine(_webLayer) { internalState, webLayer ->
            LoadingState.from(
                internalState,
                when (webLayer) {
                    null -> LoadingState.LOADING
                    else -> LoadingState.READY
                }
            )
        }
            .stateIn(viewModelScope, SharingStarted.Lazily, LoadingState.LOADING)

    private val _browserWrapperFlow = MutableStateFlow<BrowserWrapper>(regularBrowser)
    val browserWrapperFlow: StateFlow<BrowserWrapper> = _browserWrapperFlow
    val currentBrowser
        get() = browserWrapperFlow.value

    init {
        viewModelScope.launch(Dispatchers.IO) {
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

    fun switchToProfile(useIncognito: Boolean) {
        if (useIncognito) {
            val delegate = incognitoBrowser ?: run {
                IncognitoBrowserWrapper(
                    appContext = appContext,
                    activityCallbackProvider = { activityCallbacks.get() },
                    coroutineScope = viewModelScope
                ).also { it.initialize() }
            }

            incognitoBrowser = delegate
            _browserWrapperFlow.value = delegate
        } else {
            if (incognitoBrowser?.hasNoTabs() == true) {
                Log.d(TAG, "Culling unnecessary incognito profile")
                activityCallbacks.get()?.onDeleteIncognitoProfile()
                incognitoBrowser = null
            }

            // Delete incognito if necessary.
            _browserWrapperFlow.value = regularBrowser
        }
    }
}

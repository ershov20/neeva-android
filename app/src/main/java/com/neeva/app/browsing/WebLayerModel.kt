package com.neeva.app.browsing

import android.app.Application
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
    appContext: Application,
    private val domainProviderImpl: DomainProviderImpl,
    historyManager: HistoryManager,
    apolloClient: ApolloClient,
    faviconCache: FaviconCache,
    spaceStore: SpaceStore
) : ViewModel() {

    private var _activityCallbacks: WeakReference<ActivityCallbacks> = WeakReference(null)

    fun setBrowserCallbacks(activityCallbacks: WeakReference<ActivityCallbacks>) {
        _activityCallbacks = activityCallbacks
        regularBrowser.activityCallbacks = activityCallbacks
    }

    private val _webLayer = MutableStateFlow<WebLayer?>(null)

    private val regularBrowser = RegularBrowserWrapper(
        appContext = appContext,
        domainProviderImpl = domainProviderImpl,
        apolloClient = apolloClient,
        historyManager = historyManager,
        faviconCache = faviconCache,
        spaceStore = spaceStore,
        coroutineScope = viewModelScope
    )

    lateinit var initializationState: StateFlow<LoadingState>

    private val _browserWrapperFlow = MutableStateFlow<BrowserWrapper>(regularBrowser)
    val browserWrapperFlow: StateFlow<BrowserWrapper> = _browserWrapperFlow
    val currentBrowser
        get() = _browserWrapperFlow.value

    init {
        viewModelScope.launch {
            initializationState =
                domainProviderImpl.loadingState
                    .combine(_webLayer) { domainState, webLayer ->
                        val webLayerState = if (webLayer == null) {
                            LoadingState.LOADING
                        } else {
                            LoadingState.READY
                        }
                        LoadingState.from(domainState, webLayerState)
                    }
                    .stateIn(viewModelScope)
        }

        viewModelScope.launch(Dispatchers.IO) {
            domainProviderImpl.initialize()
        }

        try {
            WebLayer.loadAsync(appContext) { webLayer ->
                webLayer.isRemoteDebuggingEnabled = true
                regularBrowser.initialize(_activityCallbacks)
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
        fragmentAttacher: (Fragment) -> Unit
    ) {
        regularBrowser.createAndAttachBrowser(fragmentAttacher)

        regularBrowser.prepareBrowser(
            _activityCallbacks,
            topControlsPlaceholder,
            bottomControlsPlaceholder
        )
    }

    fun onAuthTokenUpdated() = currentBrowser.onAuthTokenUpdated()
}

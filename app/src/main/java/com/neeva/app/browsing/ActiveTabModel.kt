package com.neeva.app.browsing

import android.net.Uri
import com.apollographql.apollo3.ApolloClient
import com.neeva.app.NeevaConstants
import com.neeva.app.spaces.Space
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.chromium.weblayer.Browser
import org.chromium.weblayer.NavigateParams
import org.chromium.weblayer.Navigation
import org.chromium.weblayer.NavigationCallback
import org.chromium.weblayer.Tab
import org.chromium.weblayer.TabCallback

/** Monitors changes to the [Browser]'s active tab and emits values related to it. */
class ActiveTabModel(private val tabCreator: TabCreator) {
    data class NavigationInfo(
        val canGoBackward: Boolean = false,
        val canGoForward: Boolean = false
    )

    /** Tracks the URL displayed for the active tab. */
    private val _urlFlow = MutableStateFlow(Uri.EMPTY)
    val urlFlow: StateFlow<Uri> = _urlFlow

    /** Tracks the title displayed for the active tab. */
    private val _titleFlow = MutableStateFlow("")
    val titleFlow: StateFlow<String> = _titleFlow

    /** Indicates whether the active tab can navigate backwards or forwards. */
    private val _navigationInfoFlow = MutableStateFlow(NavigationInfo())
    val navigationInfoFlow: StateFlow<NavigationInfo> = _navigationInfoFlow

    /** Indicates how much of the website for the current page has loaded. */
    private val _progressFlow = MutableStateFlow(100)
    val progressFlow: StateFlow<Int> = _progressFlow

    /** Tracks which tab is currently active. */
    internal val activeTabFlow = MutableStateFlow<Tab?>(null)

    private val _displayedText = MutableStateFlow("")
    val displayedText: StateFlow<String> = _displayedText

    private val _isShowingQuery = MutableStateFlow(false)
    val isShowingQuery: StateFlow<Boolean> = _isShowingQuery

    internal fun onActiveTabChanged(newActiveTab: Tab?) {
        val previousTab = activeTabFlow.value
        previousTab?.apply {
            unregisterTabCallback(selectedTabCallback)
            navigationController.unregisterNavigationCallback(selectedTabNavigationCallback)
        }

        // We don't have a way to update the load progress without monitoring the tab, so hide the
        // bar until the NavigationCallback fires.
        _progressFlow.value = 100

        activeTabFlow.value = newActiveTab
        newActiveTab?.apply {
            registerTabCallback(selectedTabCallback)
            navigationController.registerNavigationCallback(selectedTabNavigationCallback)
        }

        // Update all the state to account for the currently selected tab's information.
        updateNavigationInfo()
        updateUrl(newActiveTab?.currentDisplayUrl ?: Uri.EMPTY)
        _titleFlow.value = newActiveTab?.currentDisplayTitle ?: ""
    }

    fun reload() {
        activeTabFlow.value?.navigationController?.reload()
    }

    fun loadUrl(uri: Uri, newTab: Boolean = false, isViaIntent: Boolean = false) {
        if (newTab || activeTabFlow.value == null) {
            tabCreator.createTabWithUri(
                uri = uri,
                parentTabId = null,
                isViaIntent = isViaIntent
            )
            return
        }

        // Disable intent processing for urls typed in. Allows the user to navigate to app urls.
        val navigateParamsBuilder = NavigateParams.Builder().disableIntentProcessing()
        activeTabFlow.value?.navigationController?.navigate(uri, navigateParamsBuilder.build())
    }

    private fun updateUrl(uri: Uri) {
        val isNeevaSearch = uri.toString().startsWith(NeevaConstants.appSearchURL)
        val query = if (isNeevaSearch) uri.getQueryParameter("q") else null

        _urlFlow.value = uri
        _displayedText.value = query ?: uri.host ?: ""
        _isShowingQuery.value = query != null
    }

    private val selectedTabCallback: TabCallback = object : TabCallback() {
        override fun onVisibleUriChanged(uri: Uri) {
            updateUrl(uri)
        }

        override fun onTitleUpdated(title: String) {
            _titleFlow.value = title
        }
    }

    private val selectedTabNavigationCallback = object : NavigationCallback() {
        override fun onLoadProgressChanged(progress: Double) {
            _progressFlow.value = (100 * progress).roundToInt()
        }

        override fun onNavigationStarted(navigation: Navigation) {
            updateNavigationInfo()
        }

        override fun onNavigationCompleted(navigation: Navigation) {
            updateNavigationInfo()
        }
    }

    fun goBack() {
        if (activeTabFlow.value?.navigationController?.canGoBack() == true) {
            activeTabFlow.value?.navigationController?.goBack()
            updateNavigationInfo()
        }
    }

    fun goForward() {
        if (activeTabFlow.value?.navigationController?.canGoForward() == true) {
            activeTabFlow.value?.navigationController?.goForward()
            updateNavigationInfo()
        }
    }

    /** Adds or removes the active tab from the given [space]. */
    suspend fun modifySpace(space: Space, apolloClient: ApolloClient) {
        space.addOrRemove(
            apolloClient = apolloClient,
            url = urlFlow.value,
            title = titleFlow.value
        )
    }

    private fun updateNavigationInfo() {
        _navigationInfoFlow.value = NavigationInfo(
            activeTabFlow.value?.navigationController?.canGoBack() ?: false,
            activeTabFlow.value?.navigationController?.canGoForward() ?: false
        )
    }
}

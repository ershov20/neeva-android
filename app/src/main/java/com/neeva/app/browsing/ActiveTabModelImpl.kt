package com.neeva.app.browsing

import android.net.Uri
import com.neeva.app.Dispatchers
import com.neeva.app.NeevaConstants
import com.neeva.app.spaces.SpaceStore
import com.neeva.app.storage.TabScreenshotManager
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import org.chromium.weblayer.Navigation
import org.chromium.weblayer.NavigationCallback
import org.chromium.weblayer.Tab
import org.chromium.weblayer.TabCallback

/**
 * Implements [ActiveTabModel] which exposes read-only Stateflows that monitor activeTab [Tab] changes.
 * Provides an API to use activeTab [Tab] and update respective Stateflow values.
 */
class ActiveTabModelImpl(
    private val spaceStore: SpaceStore? = null,
    val coroutineScope: CoroutineScope,
    val dispatchers: Dispatchers,
    private val neevaConstants: NeevaConstants,
    private val tabScreenshotManager: TabScreenshotManager
) : ActiveTabModel {
    private val _urlFlow = MutableStateFlow(Uri.EMPTY)
    override val urlFlow: StateFlow<Uri> = _urlFlow

    override val isCurrentUrlInSpaceFlow: StateFlow<Boolean> =
        spaceStore
            ?.stateFlow
            ?.filter { it == SpaceStore.State.READY }
            ?.combine(_urlFlow) { _, url -> spaceStore.spaceStoreContainsUrl(url) }
            ?.flowOn(dispatchers.io)
            ?.stateIn(coroutineScope, SharingStarted.Lazily, false)
            ?: MutableStateFlow(false)

    override val spacesContainingCurrentUrlFlow: StateFlow<List<String>> =
        spaceStore
            ?.stateFlow
            ?.filter { it == SpaceStore.State.READY }
            ?.combine(_urlFlow) { _, url -> spaceStore.spaceIDsContainingURL(url) }
            ?.flowOn(dispatchers.io)
            ?.distinctUntilChanged()
            ?.stateIn(coroutineScope, SharingStarted.Lazily, emptyList())
            ?: MutableStateFlow(emptyList())

    private val _titleFlow = MutableStateFlow("")
    override val titleFlow: StateFlow<String> = _titleFlow

    private val _navigationInfoFlow = MutableStateFlow(ActiveTabModel.NavigationInfo())
    override val navigationInfoFlow: StateFlow<ActiveTabModel.NavigationInfo> = _navigationInfoFlow

    private val _progressFlow = MutableStateFlow(100)
    override val progressFlow: StateFlow<Int> = _progressFlow

    private val _locationLabelInfoFlow = MutableStateFlow(ActiveTabModel.DisplayedInfo())
    override val displayedInfoFlow: StateFlow<ActiveTabModel.DisplayedInfo> =
        _locationLabelInfoFlow

    /** Tracks which tab is currently active. */
    private val activeTabFlow = MutableStateFlow<Tab?>(null)
    internal val activeTab: Tab? get() = activeTabFlow.value.takeUnless { it?.isDestroyed == true }

    /** Emits the number of cookie trackers from the active tab's webpage. */
    private val _trackersFlow = MutableStateFlow(0)
    override val trackersFlow: StateFlow<Int>
        get() = _trackersFlow

    internal fun onActiveTabChanged(newActiveTab: Tab?) {
        val previousTab = activeTab
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
        activeTab?.navigationController?.reload()
    }

    /** Don't call this directly.  Instead, use [BrowserWrapper.loadUrl]. */
    internal fun loadUrlInActiveTab(uri: Uri, stayInApp: Boolean = true) {
        activeTab?.navigate(uri, stayInApp)
    }

    private fun updateUrl(uri: Uri) {
        val isNeevaHomepage = uri.toString() == neevaConstants.appURL
        val isNeevaSearch = uri.toString().startsWith(neevaConstants.appSearchURL)
        val query = if (isNeevaSearch) uri.getQueryParameter("q") else null

        val mode = when {
            // Show the query if the user is actively searching.
            query != null -> ActiveTabModel.DisplayMode.QUERY

            // If on the homepage or the user did an empty search, show placeholder text.
            isNeevaSearch || isNeevaHomepage -> ActiveTabModel.DisplayMode.PLACEHOLDER

            // Display the current URL.
            else -> ActiveTabModel.DisplayMode.URL
        }

        val displayedText = when (mode) {
            ActiveTabModel.DisplayMode.QUERY -> query
            ActiveTabModel.DisplayMode.URL -> uri.host
            else -> null
        }

        _urlFlow.value = uri
        _locationLabelInfoFlow.value = ActiveTabModel.DisplayedInfo(
            displayedText = displayedText ?: "",
            mode = mode
        )
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

            if (_progressFlow.value == 100) {
                tabScreenshotManager.captureAndSaveScreenshot(activeTab)
            }
        }

        override fun onNavigationStarted(navigation: Navigation) {
            updateNavigationInfo()
        }

        override fun onNavigationFailed(navigation: Navigation) {
            updateNavigationInfo()
        }

        override fun onNavigationCompleted(navigation: Navigation) {
            updateNavigationInfo()
        }
    }

    fun goBack() {
        if (activeTab?.navigationController?.canGoBack() == true) {
            activeTab?.navigationController?.goBack()
            updateNavigationInfo()
        }
    }

    fun goForward() {
        if (activeTab?.navigationController?.canGoForward() == true) {
            activeTab?.navigationController?.goForward()
            updateNavigationInfo()
        }
    }

    fun toggleViewDesktopSite() {
        activeTab?.let {
            it.isDesktopUserAgentEnabled = !it.isDesktopUserAgentEnabled
            updateNavigationInfo()
        }
    }

    private fun updateNavigationInfo() {
        _navigationInfoFlow.value = ActiveTabModel.NavigationInfo(
            activeTab?.navigationController?.navigationListSize ?: 0,
            activeTab?.navigationController?.canGoBack() ?: false,
            activeTab?.navigationController?.canGoForward() ?: false,
            activeTab?.isDesktopUserAgentEnabled ?: false
        )
    }
}

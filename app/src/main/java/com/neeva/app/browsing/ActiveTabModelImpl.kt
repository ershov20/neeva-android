package com.neeva.app.browsing

import android.net.Uri
import android.view.MotionEvent
import com.neeva.app.Dispatchers
import com.neeva.app.NeevaConstants
import com.neeva.app.spaces.SpaceStore
import com.neeva.app.storage.TabScreenshotManager
import kotlin.math.abs
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
import org.chromium.weblayer.ScrollNotificationType
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
    private val tabScreenshotManager: TabScreenshotManager,
    private val tabList: TabList
) : ActiveTabModel {

    companion object {
        private const val RELOAD_THRESHOLD_FOR_OVERSCROLL = 750
    }

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

    private val _verticalOverscrollFlow = MutableStateFlow(0f)
    override val verticalOverscrollFlow: StateFlow<Float> = _verticalOverscrollFlow

    private val _isGestureActive = MutableStateFlow(false)

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

    internal fun onTabRemoved(removedTabId: String) {
        val activeTabGuid = activeTab?.guid ?: return
        val tabInfo = tabList.getTabInfo(activeTabGuid) ?: return
        if (tabInfo.data.parentTabId == removedTabId) {
            updateNavigationInfo()
        }
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

        override fun onScrollNotification(notificationType: Int, currentScrollRatio: Float) {
            if (notificationType == ScrollNotificationType.DIRECTION_CHANGED_DOWN) {
                _verticalOverscrollFlow.value = 0f
            }
        }

        override fun onVerticalOverscroll(accumulatedOverscrollY: Float) {
            // Since overscroll events fire independent of the touch handling system, we sometimes
            // get a race that sends one last overscroll even after an UP event. To avoid,
            // continuing the overscroll gesture, we only update overscroll if there is an active
            // gesture.

            if (_isGestureActive.value) {
                _verticalOverscrollFlow.value = accumulatedOverscrollY
            }
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
            // WebLayer doesn't seem to send an updated progress value on a load timeout.
            _progressFlow.value = 100
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
        } else if (tabList.isParentTabInList(activeTab?.guid)) {
            activeTab?.dispatchBeforeUnloadAndClose()
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

    fun resetOverscroll(action: Int) {
        when (action) {
            MotionEvent.ACTION_DOWN -> _isGestureActive.value = true
            MotionEvent.ACTION_UP -> _isGestureActive.value = false
        }

        if (abs(verticalOverscrollFlow.value) > RELOAD_THRESHOLD_FOR_OVERSCROLL) {
            reload()
        }

        _verticalOverscrollFlow.value = 0f
    }

    private fun updateNavigationInfo() {
        val hasBackNavigations = activeTab?.navigationController?.canGoBack() ?: false
        val isParentTabInList = tabList.isParentTabInList(activeTab?.guid)

        _navigationInfoFlow.value = ActiveTabModel.NavigationInfo(
            activeTab?.navigationController?.navigationListSize ?: 0,
            hasBackNavigations || isParentTabInList,
            activeTab?.navigationController?.canGoForward() ?: false,
            activeTab?.isDesktopUserAgentEnabled ?: false
        )
    }
}

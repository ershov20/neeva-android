package com.neeva.app.browsing

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.ContextMenu
import android.view.ContextMenu.ContextMenuInfo
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import com.neeva.app.NeevaBrowser
import com.neeva.app.NeevaConstants.appURL
import com.neeva.app.NeevaConstants.loginCookie
import com.neeva.app.history.HistoryViewModel
import com.neeva.app.saveLoginCookieFrom
import com.neeva.app.storage.DateConverter
import com.neeva.app.storage.DomainViewModel
import com.neeva.app.storage.Visit
import com.neeva.app.storage.toFavicon
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.chromium.weblayer.*
import java.io.File
import java.io.FileOutputStream
import java.lang.ref.WeakReference
import java.util.*

class WebLayerModel(
    private val domainViewModel: DomainViewModel,
    private val historyViewModel: HistoryViewModel
): ViewModel() {
    companion object {
        private const val KEY_PREVIOUS_TAB_GUIDS = "previousTabGuids"
        const val DIRECTORY_TAB_SCREENSHOTS = "tab_screenshots"
    }

    var browserCallbacks: WeakReference<BrowserCallbacks> = WeakReference(null)

    private class PerTabState(
        val faviconFetcher: FaviconFetcher,
        val tabCallback: TabCallback
    )

    val selectedTabFlow = MutableStateFlow<Pair<Tab?, Tab?>>(Pair(null, null))

    private inner class FullscreenCallbackImpl : FullscreenCallback() {
        private var mSystemVisibilityToRestore = 0

        override fun onEnterFullscreen(exitFullscreenRunnable: Runnable) {
            browserCallbacks.get()?.onEnterFullscreen()?.let {
                mSystemVisibilityToRestore = it
            }
        }

        override fun onExitFullscreen() {
            browserCallbacks.get()?.onExitFullscreen(mSystemVisibilityToRestore)
        }
    }

    class ContextMenuCreator(
        var webLayerModel: WebLayerModel,
        var params: ContextMenuParams,
        var tab: Tab? = null,
        var context: Context? = null,
    ): View.OnCreateContextMenuListener, MenuItem.OnMenuItemClickListener {
        companion object {
            private const val MENU_ID_COPY_LINK_URI = 1
            private const val MENU_ID_COPY_LINK_TEXT = 2
            private const val MENU_ID_DOWNLOAD_IMAGE = 3
            private const val MENU_ID_DOWNLOAD_VIDEO = 4
            private const val MENU_ID_DOWNLOAD_LINK = 5
            private const val MENU_ID_OPEN_IN_NEW_TAB = 6
        }

        override fun onCreateContextMenu(
            menu: ContextMenu,
            v: View,
            menuInfo: ContextMenuInfo?
        ) {
            context = v.context
            menu.add(params.pageUri.toString())
            if (params.linkUri != null) {
                val openNewTabItem =
                    menu.add(Menu.NONE, MENU_ID_OPEN_IN_NEW_TAB, Menu.NONE, "Open in new tab")
                openNewTabItem.setOnMenuItemClickListener(this)
            }
            if (params.linkUri != null) {
                val copyLinkUriItem =
                    menu.add(Menu.NONE, MENU_ID_COPY_LINK_URI, Menu.NONE, "Copy link address")
                copyLinkUriItem.setOnMenuItemClickListener(this)
            }
            if (!TextUtils.isEmpty(params.linkText)) {
                val copyLinkTextItem =
                    menu.add(Menu.NONE, MENU_ID_COPY_LINK_TEXT, Menu.NONE, "Copy link text")
                copyLinkTextItem.setOnMenuItemClickListener(this)
            }
            if (params.canDownload) {
                if (params.isImage) {
                    val downloadImageItem = menu.add(
                        Menu.NONE, MENU_ID_DOWNLOAD_IMAGE, Menu.NONE, "Download image"
                    )
                    downloadImageItem.setOnMenuItemClickListener(this)
                } else if (params.isVideo) {
                    val downloadVideoItem = menu.add(
                        Menu.NONE, MENU_ID_DOWNLOAD_VIDEO, Menu.NONE, "Download video"
                    )
                    downloadVideoItem.setOnMenuItemClickListener(this)
                } else if (params.linkUri != null) {
                    val downloadVideoItem =
                        menu.add(Menu.NONE, MENU_ID_DOWNLOAD_LINK, Menu.NONE, "Download link")
                    downloadVideoItem.setOnMenuItemClickListener(this)
                }
            }
            if (!TextUtils.isEmpty(params.titleOrAltText)) {
                val altTextView = TextView(context)
                altTextView.text = params.titleOrAltText
                menu.setHeaderView(altTextView)
            }
            v.setOnCreateContextMenuListener(null)
        }

        override fun onMenuItemClick(item: MenuItem): Boolean {
            val clipboard =
                context!!.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            when (item.itemId) {
                MENU_ID_OPEN_IN_NEW_TAB -> webLayerModel.createTabFor(params.linkUri!!)
                MENU_ID_COPY_LINK_URI -> clipboard.setPrimaryClip(
                    ClipData.newPlainText("link address", params.linkUri.toString())
                )
                MENU_ID_COPY_LINK_TEXT -> clipboard.setPrimaryClip(
                    ClipData.newPlainText(
                        "link text",
                        params.linkText
                    )
                )
                MENU_ID_DOWNLOAD_IMAGE, MENU_ID_DOWNLOAD_VIDEO, MENU_ID_DOWNLOAD_LINK -> tab!!.download(
                    params
                )
                else -> {
                }
            }
            return true
        }
    }

    private var uriRequestForNewTab: Uri? = null
    private var lastSavedInstanceState: Bundle? = null

    private lateinit var profile: Profile
    private lateinit var browser: Browser

    private val tabListCallback = object : TabListCallback() {
        override fun onActiveTabChanged(activeTab: Tab?) {
            viewModelScope.launch {
                val previous = selectedTabFlow.asLiveData().value?.second
                selectedTabFlow.emit(Pair(previous, activeTab))
            }
            tabList.updatedSelectedTab(activeTab?.guid)
        }

        override fun onTabRemoved(tab: Tab) {
            val newIndex = (tabList.indexOf(tab) - 1).coerceAtLeast(0)
            tabList.remove(tab)

            unregisterTabCallbacks(tab)

            // Don't switch tabs unless there isn't one currently selected.
            if (browser.activeTab == null) {
                if (orderedTabList.value?.isNotEmpty() == true) {
                    browser.setActiveTab(tabList.getTab(newIndex))
                } else {
                    createTabFor(Uri.parse(appURL))
                    browser.setActiveTab(tabList.getTab(newIndex))
                    browserCallbacks.get()?.bringToForeground()
                }
            }
        }

        override fun onTabAdded(tab: Tab) {
            tabList.add(tab)

            registerTabCallbacks(tab)
            uriRequestForNewTab?.let {
                selectTab(tab)
                tab.navigationController.navigate(it)
                uriRequestForNewTab = null
            }
        }

        override fun onWillDestroyBrowserAndAllTabs() {
            unregisterBrowserAndTabCallbacks()
        }
    }

    private val navigationCallback = object : NavigationCallback() {
        override fun onNavigationStarted(navigation: Navigation) {
            if (navigation.isSameDocument) return

            val timestamp = Date()
            val visit = Visit(timestamp = timestamp,
                visitRootID = DateConverter.fromDate(timestamp)!!, visitType = 0)
            historyViewModel.insert(navigation.uri, visit = visit)
        }
    }

    private val browserRestoreCallback: BrowserRestoreCallback = object : BrowserRestoreCallback() {
        override fun onRestoreCompleted() {
            super.onRestoreCompleted()

            restorePreviousTabList(lastSavedInstanceState);
            if (browser.tabs.count() == 1
                && browser.activeTab == browser.tabs.first()
                && browser.activeTab?.navigationController?.navigationListCurrentIndex == -1
            ) {
                browser.activeTab?.navigationController?.navigate(Uri.parse(appURL))
            } else if (browser.tabs.isEmpty()) {
                createTabFor(Uri.parse(appURL))
            }
        }
    }

    private val cookieChangedCallback: CookieChangedCallback = object : CookieChangedCallback() {
        override fun onCookieChanged(cookie: String, cause: Int) {
            saveLoginCookieFrom(cookie)
        }
    }

    private val bottomControlsOffsetCallback: BrowserControlsOffsetCallback = object : BrowserControlsOffsetCallback() {
        override fun onBottomViewOffsetChanged(offset: Int) {
            browserCallbacks.get()?.onBottomBarOffsetChanged(offset)
        }
    }

    private var tabList = TabList()
    val orderedTabList: LiveData<List<BrowserPrimitive>>
        get() = tabList.orderedTabList

    private var fullscreenCallback: FullscreenCallbackImpl? = null
    private val tabToPerTabState: HashMap<Tab, PerTabState> = HashMap()

    fun onSaveInstanceState(outState: Bundle) {
        // Store the stack of previous tab GUIDs that are used to set the next active tab when a tab
        // closes. Also used to setup various callbacks again on restore.
        val previousTabGuids = orderedTabList.value?.map { it.id }?.toTypedArray()
        outState.putStringArray(KEY_PREVIOUS_TAB_GUIDS, previousTabGuids)
    }

    fun onWebLayerReady(
        fragment: Fragment,
        bottomControlsPlaceholder: View,
        savedInstanceState: Bundle?
    ) {
        lastSavedInstanceState = savedInstanceState
        // Have WebLayer Shell retain the fragment instance to simulate the behavior of
        // external embedders (note that if this is changed, then WebLayer Shell should handle
        // rotations and resizes itself via its manifest, as otherwise the user loses all state
        // when the shell is rotated in the foreground).
        fragment.retainInstance = true
        browser = Browser.fromFragment(fragment)!!

        browserCallbacks.get()?.getDisplaySize()?.let { windowSize ->
            browser.setMinimumSurfaceSize(windowSize.x, windowSize.y)
        }

        profile = browser.profile

        profile.setTablessOpenUrlCallback(object : OpenUrlCallback() {
            override fun getBrowserForNewTab(): Browser {
                return browser
            }

            override fun onTabAdded(tab: Tab) {
                registerTabCallbacks(tab)
            }
        })

        // There appears to be a bug in WebLayer that prevents the bottom bar from being rendered,
        // and also prevents Composables from being re-rendered when their state changes.  To get
        // around this, we pass in a fake view that is the same height as the real bottom toolbar
        // and listen for the scrolling offsets, which we then apply to the real bottom toolbar.
        // This is a valid use case according to the BrowserControlsOffsetCallback.
        browser.setBottomView(bottomControlsPlaceholder)
        bottomControlsPlaceholder.layoutParams.height =
            fragment.context?.resources?.getDimensionPixelSize(com.neeva.app.R.dimen.bottom_toolbar_height) ?: 0
        bottomControlsPlaceholder.requestLayout()
        browser.registerBrowserControlsOffsetCallback(bottomControlsOffsetCallback)

        browser.registerBrowserRestoreCallback(browserRestoreCallback)
        profile.cookieManager.getCookie(Uri.parse(appURL)) {
            it?.split("; ")?.forEach { cookie ->
                saveLoginCookieFrom(cookie)
            }
        }
        profile.cookieManager.addCookieChangedCallback(Uri.parse(appURL),
            loginCookie, cookieChangedCallback)

        browser.registerTabListCallback(tabListCallback)
    }

    private fun restorePreviousTabList(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) return
        val previousTabGuids = savedInstanceState.getStringArray(KEY_PREVIOUS_TAB_GUIDS) ?: return
        val currentTabMap: MutableMap<String, Tab> = HashMap()
        browser.tabs.forEach { currentTabMap[it.guid] = it }
        previousTabGuids.forEach {
            val tab = currentTabMap[it] ?: return
            tabList.add(tab)
            registerTabCallbacks(tab)
        }
    }

    fun createTabFor(uri: Uri) {
        uriRequestForNewTab = uri
        browser.createTab()
    }

    fun registerNewTab(tab: Tab, @NewTabType type: Int) {
        registerTabCallbacks(tab)
        if (type == NewTabType.FOREGROUND_TAB) {
            selectTab(tab)
        }
    }

    fun registerTabCallbacks(tab: Tab) {
        when {
            fullscreenCallback != null -> tab.fullscreenCallback = fullscreenCallback

            tab.fullscreenCallback != null -> {
                fullscreenCallback = tab.fullscreenCallback as FullscreenCallbackImpl
            }

            else -> {
                fullscreenCallback = FullscreenCallbackImpl()
                tab.fullscreenCallback = fullscreenCallback
            }
        }

        tab.navigationController.registerNavigationCallback(navigationCallback)
        val tabCallback: TabCallback = object : TabCallback() {
            override fun bringTabToFront() {
                tab.browser.setActiveTab(tab)
                browserCallbacks.get()?.bringToForeground()
            }

            override fun onTitleUpdated(title: String) {
                domainViewModel.insert(tab.currentDisplayUrl.toString(), title)
                historyViewModel.insert(url = tab.currentDisplayUrl!!, title = title)
                tabList.updateTabTitle(tab.guid, tab.currentDisplayTitle)
            }

            override fun onVisibleUriChanged(uri: Uri) {
                tabList.updateUrl(tab.guid, uri)
            }

            override fun showContextMenu(params: ContextMenuParams) {
                if (tab != browser.activeTab) return
                browserCallbacks.get()?.showContextMenuForTab(params, tab)
            }
        }
        tab.registerTabCallback(tabCallback)
        val faviconFetcher = tab.createFaviconFetcher(object : FaviconCallback() {
            override fun onFaviconChanged(favicon: Bitmap?) {
                val icon = favicon ?: return
                domainViewModel.updateFaviconFor(tab.currentDisplayUrl.toString(), icon.toFavicon())
                historyViewModel.insert(url = tab.currentDisplayUrl!!, favicon = icon.toFavicon())
            }
        })
        tabToPerTabState[tab] = PerTabState(faviconFetcher, tabCallback)
    }

    private fun unregisterBrowserAndTabCallbacks() {
        browser.unregisterTabListCallback(tabListCallback)
        browser.unregisterBrowserRestoreCallback(browserRestoreCallback)
        browser.unregisterBrowserControlsOffsetCallback(bottomControlsOffsetCallback)
        tabToPerTabState.forEach { unregisterTabCallbacks(it.key) }
        tabToPerTabState.clear()
    }

    private fun unregisterTabCallbacks(tab: Tab) {
        // Do not unset FullscreenCallback here which is called from onDestroy, since
        // unsetting FullscreenCallback also exits fullscreen.
        tab.navigationController.unregisterNavigationCallback(navigationCallback)
        val perTabState: PerTabState = tabToPerTabState[tab]!!
        tab.unregisterTabCallback(perTabState.tabCallback)
        perTabState.faviconFetcher.destroy()
        tabToPerTabState.remove(tab)
    }

    fun onGridShown() {
        browser.activeTab?.captureAndSaveScreenshot()
    }

    fun select(primitive: BrowserPrimitive) {
        val tab = tabList.findTab(primitive.id) ?: return
        selectTab(tab)
    }

    fun close(primitive: BrowserPrimitive) {
        val tab = tabList.findTab(primitive.id) ?: return
        tab.dispatchBeforeUnloadAndClose()
    }

    fun selectTab(tab: Tab) {
        browser.activeTab?.takeUnless { it.isDestroyed }?.let { activeTab ->
            activeTab.setNewTabCallback(null)
            activeTab.setErrorPageCallback(null)
            activeTab.captureAndSaveScreenshot()
        }
        browser.setActiveTab(tab)
    }

    private fun Tab.captureAndSaveScreenshot() {
        // TODO(dan.alcantara): There appears to be a race condition that results in the Tab being
        //                      destroyed (and unusable by WebLayer) before this is called.
        if (isDestroyed) return

        captureScreenShot(0.5f) { thumbnail, _ ->
            val dir = File(NeevaBrowser.context.filesDir, DIRECTORY_TAB_SCREENSHOTS)
            dir.mkdirs()

            val file = File(dir, "tab_$guid.jpg")
            if (file.exists()) file.delete()

            try {
                val out = FileOutputStream(file)
                thumbnail?.compress(CompressFormat.JPEG, 100, out)
                out.flush()
                out.close()
                tabList.updateThumbnailUri(guid, file.toUri())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

@Suppress("UNCHECKED_CAST")
class WebViewModelFactory(
    private val domainModel: DomainViewModel,
    private val historyViewModel: HistoryViewModel
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return WebLayerModel(
            domainModel,
            historyViewModel = historyViewModel
        ) as T
    }
}

data class BrowserPrimitive(
    val id: String,
    val thumbnailUri: Uri? = File(
        File(NeevaBrowser.context.filesDir, WebLayerModel.DIRECTORY_TAB_SCREENSHOTS),
        "tab_$id.jpg"
    ).toUri(),
    val url: Uri?,
    val title: String?,
    val isSelected: Boolean
)
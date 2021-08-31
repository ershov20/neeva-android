package com.neeva.app.web

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Display
import android.view.View
import android.view.WindowManager
import android.webkit.ValueCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import com.neeva.app.*
import com.neeva.app.R
import com.neeva.app.history.HistoryViewModel
import com.neeva.app.storage.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.chromium.weblayer.*
import java.util.*

// TODO(yusuf) Lose the dependency on activity here and pass a FullscreenCallbackProvider instead
class WebLayerModel(
        private val activity: NeevaActivity,
        private val domainViewModel: DomainViewModel,
        private val historyViewModel: HistoryViewModel
    ): ViewModel() {

    private val KEY_PREVIOUS_TAB_GUIDS = "previousTabGuids"

    private class PerTabState(
        val faviconFetcher: FaviconFetcher,
        val tabCallback: TabCallback
    )

    val selectedTabFlow = MutableStateFlow<Pair<Tab?, Tab?>>(Pair(null, null))

    private class FullscreenCallbackImpl(var activity: NeevaActivity) : FullscreenCallback() {
        private var mSystemVisibilityToRestore = 0

        override fun onEnterFullscreen(exitFullscreenRunnable: Runnable) {
            // This avoids an extra resize.
            val attrs: WindowManager.LayoutParams = activity.window.attributes
            attrs.flags = attrs.flags or WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
            activity.window.attributes = attrs
            val decorView: View = activity.window.decorView
            // Caching the system ui visibility is ok for shell, but likely not ok for
            // real code.
            mSystemVisibilityToRestore = decorView.systemUiVisibility
            decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                    or View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                    or View.SYSTEM_UI_FLAG_LOW_PROFILE or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        }

        override fun onExitFullscreen() {
            val decorView: View = activity.window.decorView
            decorView.systemUiVisibility = mSystemVisibilityToRestore
            val attrs: WindowManager.LayoutParams = activity.window.attributes
            if (attrs.flags and WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS != 0) {
                attrs.flags =
                    attrs.flags and WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS.inv()
                activity.window.attributes = attrs
            }
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
        }

        override fun onTabRemoved(tab: Tab) {
            onTabClosed(tab)
        }

        override fun onTabAdded(tab: Tab) {
            _tabList.value?.add(tab)
            _tabList.value = _tabList.value
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
        }
    }

    private val cookieChangedCallback: CookieChangedCallback = object : CookieChangedCallback() {
        override fun onCookieChanged(cookie: String, cause: Int) {
            saveLoginCookieFrom(cookie)
        }
    }

    private val _tabList = MutableLiveData(ArrayList<Tab>())
    val tabList: LiveData<ArrayList<Tab>> = _tabList
    private var fullscreenCallback: FullscreenCallbackImpl? = null
    private val tabToPerTabState: HashMap<Tab, PerTabState> = HashMap()

    fun onSaveInstanceState(outState: Bundle) {
        // Store the stack of previous tab GUIDs that are used to set the next active tab when a tab
        // closes. Also used to setup various callbacks again on restore.
        val previousTabGuids = tabList.value?.map { it.guid }?.toTypedArray()
        outState.putStringArray(KEY_PREVIOUS_TAB_GUIDS, previousTabGuids)
    }

    fun onWebLayerReady(fragment: Fragment, bottomControls: View, savedInstanceState: Bundle?) {
        lastSavedInstanceState = savedInstanceState
        // Have WebLayer Shell retain the fragment instance to simulate the behavior of
        // external embedders (note that if this is changed, then WebLayer Shell should handle
        // rotations and resizes itself via its manifest, as otherwise the user loses all state
        // when the shell is rotated in the foreground).
        fragment.retainInstance = true
        browser = Browser.fromFragment(fragment)!!
        val display: Display = getDefaultDisplay()
        val point = Point()
        display.getRealSize(point)
        browser.setMinimumSurfaceSize(point.x, point.y)
        profile = browser.profile
        profile.setUserIdentityCallback(object : UserIdentityCallback() {
            override fun getEmail(): String {
                return "user@example.com"
            }

            override fun getFullName(): String {
                return "Jill Doe"
            }

            override fun getAvatar(desiredSize: Int, avatarLoadedCallback: ValueCallback<Bitmap>) {
                // Simulate a delayed load.
                val handler = Handler(Looper.getMainLooper())
                handler.postDelayed({
                    val bitmap = BitmapFactory.decodeResource(
                        activity.resources, R.drawable.ic_baseline_image_24
                    )
                    // Curveball: set an arbitrary density.
                    bitmap.density = 120
                    avatarLoadedCallback.onReceiveValue(bitmap)
                }, 3000)
            }
        })

        profile.setTablessOpenUrlCallback(object : OpenUrlCallback() {
            override fun getBrowserForNewTab(): Browser {
                return browser
            }

            override fun onTabAdded(tab: Tab) {
                registerTabCallbacks(tab)
            }
        })
        browser.setBottomView(bottomControls)
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
            _tabList.value?.add(tab)
            registerTabCallbacks(tab)
        }
    }

    fun createTabFor(uri: Uri) {
        uriRequestForNewTab = uri
        browser.createTab()
    }

    fun registerTabCallbacks(tab: Tab) {
        when {
            fullscreenCallback != null -> tab.fullscreenCallback = fullscreenCallback
            tab.fullscreenCallback != null -> {
                fullscreenCallback = tab.fullscreenCallback as FullscreenCallbackImpl
                fullscreenCallback!!.activity = activity
            }
            else -> {
                fullscreenCallback = FullscreenCallbackImpl(activity)
                tab.fullscreenCallback = fullscreenCallback
            }

        }
        tab.navigationController.registerNavigationCallback(navigationCallback)
        val tabCallback: TabCallback = object : TabCallback() {
            override fun bringTabToFront() {
                tab.browser.setActiveTab(tab)
                val intent =
                    Intent(activity, NeevaActivity::class.java)
                intent.action = Intent.ACTION_MAIN
                activity.startActivity(intent)
            }

            override fun onTitleUpdated(title: String) {
                domainViewModel.insert(tab.currentDisplayUrl.toString(), title)
                historyViewModel.insert(url = tab.currentDisplayUrl!!, title = title)
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

    fun selectTab(tab: Tab) {
        browser.activeTab?.let {
            it.setNewTabCallback(null)
            it.setErrorPageCallback(null)
        }
        browser.setActiveTab(tab)
    }

    private fun onTabClosed(tab: Tab) {
        _tabList.value?.remove(tab)
        unregisterTabCallbacks(tab)
        if (browser.activeTab == null && tabList.value?.isNotEmpty() == true) {
            val size = tabList.value?.size ?: return
            _tabList.value?.removeAt(size - 1)?.let { browser.setActiveTab(it) }
        }
    }

    private fun getDefaultDisplay(): Display {
        val windowManager = activity.getSystemService(AppCompatActivity.WINDOW_SERVICE) as WindowManager
        return windowManager.defaultDisplay
    }
}

val Tab.currentDisplayUrl: Uri?
    get() {
        val navigationController = navigationController
        if (navigationController.navigationListSize == 0) return null

        return navigationController.getNavigationEntryDisplayUri(
            navigationController.navigationListCurrentIndex)
    }

val Tab.currentDisplayTitle: String?
    get() {
        val navigationController = navigationController
        if (navigationController.navigationListSize == 0) return null

        return navigationController.getNavigationEntryTitle(
            navigationController.navigationListCurrentIndex)
    }

val Tab.isSelected: Boolean
    get() {
        return browser.activeTab == this
    }

@Suppress("UNCHECKED_CAST")
class WebViewModelFactory(private val activity: NeevaActivity,
                          private val domainModel: DomainViewModel,
                          private val historyViewModel: HistoryViewModel) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return WebLayerModel(activity, domainModel, historyViewModel = historyViewModel) as T
    }
}
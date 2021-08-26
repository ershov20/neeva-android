package com.neeva.app.web

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Patterns
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
import org.chromium.weblayer.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.math.roundToInt

// TODO(yusuf) Lose the dependency on activity here and pass a FullscreenCallbackProvider instead
class WebLayerModel(
        private val activity: NeevaActivity,
        private val domainViewModel: DomainViewModel,
        private val historyViewModel: HistoryViewModel
    ): ViewModel() {

    private class PerTabState(
        val faviconFetcher: FaviconFetcher,
        val tabCallback: TabCallback
    )

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

    private val _currentUrl = MutableLiveData(Uri.parse(appURL)!!)
    val currentUrl: LiveData<Uri> = _currentUrl

    private val _currentTitle = MutableLiveData("")
    val currentTitle: LiveData<String> = _currentTitle

    private val _canGoBack = MutableLiveData(false)
    val canGoBack: LiveData<Boolean> = _canGoBack

    private val _canGoForward = MutableLiveData(false)
    val canGoForward: LiveData<Boolean> = _canGoForward

    private val _progress = MutableLiveData(0)
    val progress: LiveData<Int> = _progress


    private lateinit var profile: Profile
    private lateinit var browser: Browser

    private val tabListCallback = object : TabListCallback() {
        override fun onActiveTabChanged(activeTab: Tab?) {
            //TODO Set url and update favicon! Right now, we don't change tabs...
        }

        override fun onTabRemoved(tab: Tab) {
            closeTab(tab)
        }

        override fun onWillDestroyBrowserAndAllTabs() {
            unregisterBrowserAndTabCallbacks()
        }
    }

    private val cookieChangedCallback: CookieChangedCallback = object : CookieChangedCallback() {
        override fun onCookieChanged(cookie: String, cause: Int) {
            saveLoginCookieFrom(cookie)
        }
    }

    private val newTabCallback: NewTabCallback = object : NewTabCallback() {
        override fun onNewTab(newTab: Tab, @NewTabType type: Int) {
            onTabAddedImpl(newTab)
        }
    }

    private val navigationCallback = object : NavigationCallback() {
        override fun onLoadProgressChanged(progress: Double) {
            updateProgress((100 * progress).roundToInt())
        }

        override fun onNavigationStarted(navigation: Navigation) {
            super.onNavigationStarted(navigation)
            if (navigation.isSameDocument) return
            _canGoBack.value = browser.activeTab?.navigationController?.canGoBack()
            _canGoForward.value = browser.activeTab?.navigationController?.canGoForward()

            val timestamp = Date()
            val visit = Visit(timestamp = timestamp,
                visitRootID = DateConverter.fromDate(timestamp)!!, visitType = 0)
            historyViewModel.insert(navigation.uri, visit = visit)
        }

        override fun onNavigationCompleted(navigation: Navigation) {
            super.onNavigationCompleted(navigation)
            _canGoBack.value = browser.activeTab?.navigationController?.canGoBack()
            _canGoForward.value = browser.activeTab?.navigationController?.canGoForward()
        }
    }

    private val errorPageCallback = object : ErrorPageCallback() {
        override fun onBackToSafety(): Boolean {
            //context.onBackPressed()
            return true
        }
    }

    private var fullscreenCallback: FullscreenCallbackImpl? = null
    private val previousTabList: ArrayList<Tab> = ArrayList()
    private val tabToPerTabState: HashMap<Tab, PerTabState> = HashMap()

    fun goBack() = browser.activeTab?.navigationController?.goBack()
    fun goForward() = browser.activeTab?.navigationController?.goForward()
    fun reload() {
        browser.activeTab?.navigationController?.reload()
    }

    fun updateProgress(progress: Int) {
        _progress.value = progress
    }

    fun loadUrl(uri: Uri) {
        // Disable intent processing for urls typed in. Allows the user to navigate to app urls.
        val navigateParamsBuilder = NavigateParams.Builder().disableIntentProcessing()
        browser.activeTab?.navigationController?.navigate(uri, navigateParamsBuilder.build())
    }

    fun onWebLayerReady(fragment: Fragment, bottomControls: View) {
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
                onTabAddedImpl(tab)
            }
        })
        browser.activeTab?.let { registerTabCallbacks(it) }
        browser.setBottomView(bottomControls)
        browser.registerTabListCallback(tabListCallback)
        if (getCurrentDisplayUrl() != null) {
            return
        }
        profile.cookieManager.getCookie(Uri.parse(appURL)) {
            it?.split("; ")?.forEach { cookie ->
                saveLoginCookieFrom(cookie)
            }
        }
        profile.cookieManager.addCookieChangedCallback(Uri.parse(appURL),
            loginCookie, cookieChangedCallback)
        loadUrl(Uri.parse(appURL))
    }

    private fun registerTabCallbacks(tab: Tab) {
        tab.setNewTabCallback(newTabCallback)
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
        tab.setErrorPageCallback(errorPageCallback)
        val tabCallback: TabCallback = object : TabCallback() {
            override fun bringTabToFront() {
                tab.browser.setActiveTab(tab)
                val intent =
                    Intent(activity, NeevaActivity::class.java)
                intent.action = Intent.ACTION_MAIN
                activity.startActivity(intent)
            }

            override fun onVisibleUriChanged(uri: Uri) {
                super.onVisibleUriChanged(uri)
                _currentUrl.value = uri
            }

            override fun onTitleUpdated(title: String) {
                super.onTitleUpdated(title)
                _currentTitle.value = title
                domainViewModel.insert(_currentUrl.value.toString(), title)
                historyViewModel.insert(url = _currentUrl.value!!, title = title)
            }
        }
        tab.registerTabCallback(tabCallback)
        val faviconFetcher = tab.createFaviconFetcher(object : FaviconCallback() {
            override fun onFaviconChanged(favicon: Bitmap?) {
                val url = currentUrl.value.toString() ?: return
                val icon = favicon ?: return
                domainViewModel.updateFaviconFor(url, icon.toFavicon())
                historyViewModel.insert(url = _currentUrl.value!!, favicon = icon.toFavicon())
            }
        })
        tabToPerTabState[tab] = PerTabState(faviconFetcher, tabCallback)
    }

    private fun unregisterBrowserAndTabCallbacks() {
        browser.unregisterTabListCallback(tabListCallback)
        tabToPerTabState.forEach { unregisterTabCallbacks(it.key) }
        tabToPerTabState.clear()
    }

    private fun unregisterTabCallbacks(tab: Tab) {
        // Do not unset FullscreenCallback here which is called from onDestroy, since
        // unsetting FullscreenCallback also exits fullscreen.
        tab.setNewTabCallback(null)
        tab.navigationController.unregisterNavigationCallback(navigationCallback)
        tab.setErrorPageCallback(null)
        val perTabState: PerTabState = tabToPerTabState[tab]!!
        tab.unregisterTabCallback(perTabState.tabCallback)
        perTabState.faviconFetcher.destroy()
        tabToPerTabState.remove(tab)
    }

    private fun onTabAddedImpl(newTab: Tab) {
        registerTabCallbacks(newTab)
        browser.activeTab?.let { previousTabList.add(it) }
        browser.setActiveTab(newTab)
    }

    private fun closeTab(tab: Tab) {
        previousTabList.remove(tab)
        if (browser.activeTab == null && previousTabList.isNotEmpty()) {
            browser.setActiveTab(previousTabList.removeAt(previousTabList.size - 1))
        }
        unregisterTabCallbacks(tab)
    }

    private fun getDefaultDisplay(): Display {
        val windowManager = activity.getSystemService(AppCompatActivity.WINDOW_SERVICE) as WindowManager
        return windowManager.defaultDisplay
    }

    /* Returns the Url for the current tab as a String, or null if there is no
     * current tab. */
    private fun getCurrentDisplayUrl(): String? {
        val tab = browser.activeTab ?: return null
        val navigationController = tab.navigationController
        if (navigationController.navigationListSize == 0) return null

        return navigationController.getNavigationEntryDisplayUri(
            navigationController.navigationListCurrentIndex).toString()
    }

    /**
     * Given input which may be empty, null, a URL, or search terms, this forms a URI suitable for
     * loading in a tab.
     * @param input The text.
     * @return A valid URL.
     */
    private fun getUriFromInput(input: String): Uri? {
        if (TextUtils.isEmpty(input)) {
            return Uri.parse("https://neeva.com")
        }

        // WEB_URL doesn't match port numbers. Special case "localhost:" to aid
        // testing where a port is remapped.
        // Use WEB_URL first to ensure this matches urls such as 'https.'
        if (Patterns.WEB_URL.matcher(input).matches() || input.startsWith("http://localhost:")) {
            // WEB_URL matches relative urls (relative meaning no scheme), but this branch is only
            // interested in absolute urls. Fall through if no scheme is supplied.
            val uri = Uri.parse(input)
            if (!uri.isRelative) return uri
        }
        if (input.startsWith("www.") || input.indexOf(":") == -1) {
            val url = "http://$input"
            if (Patterns.WEB_URL.matcher(url).matches()) {
                return Uri.parse(url)
            }
        }
        return Uri.parse(appSearchURL)
            .buildUpon()
            .appendQueryParameter("q", input)
            .appendQueryParameter("src", "nvobar")
            .build()
    }
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
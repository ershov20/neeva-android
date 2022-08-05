package com.neeva.app.browsing

import android.net.Uri
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neeva.app.BaseTest
import com.neeva.app.CoroutineScopeRule
import com.neeva.app.cookiecutter.CookieCutterModel
import com.neeva.app.cookiecutter.ScriptInjectionManager
import com.neeva.app.cookiecutter.TrackersAllowList
import com.neeva.app.cookiecutter.TrackingData
import com.neeva.app.history.HistoryManager
import com.neeva.app.publicsuffixlist.DomainProvider
import com.neeva.app.storage.favicons.FaviconCache
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import org.chromium.weblayer.Browser
import org.chromium.weblayer.FaviconFetcher
import org.chromium.weblayer.FullscreenCallback
import org.chromium.weblayer.Navigation
import org.chromium.weblayer.NavigationCallback
import org.chromium.weblayer.NavigationController
import org.chromium.weblayer.Tab
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class TabCallbacksTest : BaseTest() {
    @Rule
    @JvmField
    val coroutineScopeRule = CoroutineScopeRule()

    @MockK lateinit var activityCallbacks: ActivityCallbacks
    @MockK lateinit var domainProvider: DomainProvider
    @MockK lateinit var faviconCache: FaviconCache
    @MockK lateinit var faviconFetcher: FaviconFetcher
    @MockK lateinit var fullscreenCallback: FullscreenCallback
    @MockK lateinit var historyManager: HistoryManager
    @MockK lateinit var registerNewTab: (tab: Tab, type: Int) -> Unit
    @MockK lateinit var scriptInjectionManager: ScriptInjectionManager
    @MockK lateinit var trackersAllowList: TrackersAllowList

    private lateinit var activityCallbackProvider: ActivityCallbackProvider
    private lateinit var browserFlow: StateFlow<Browser?>
    private lateinit var cookieCutterModel: CookieCutterModel
    private lateinit var navigationController: NavigationController
    private lateinit var tabList: TabList

    private lateinit var browser: Browser
    private lateinit var tab: Tab
    private lateinit var tabCallbacks: TabCallbacks

    // CookieCutterModel related mocks.
    private lateinit var cookieNoticeBlockedFlow: MutableStateFlow<Boolean>
    private lateinit var trackingDataFlow: MutableStateFlow<TrackingData?>
    private lateinit var enableTrackingProtection: MutableState<Boolean>

    private val navigationCallback = mutableListOf<NavigationCallback>()

    override fun setUp() {
        super.setUp()

        cookieNoticeBlockedFlow = MutableStateFlow(false)
        trackingDataFlow = MutableStateFlow(null)
        enableTrackingProtection = mutableStateOf(true)

        cookieCutterModel = mockk {
            every { cookieNoticeBlockedFlow } returns this@TabCallbacksTest.cookieNoticeBlockedFlow
            every { trackingDataFlow } returns this@TabCallbacksTest.trackingDataFlow
            every { enableTrackingProtection }.returns(
                this@TabCallbacksTest.enableTrackingProtection
            )
            every { trackersAllowList }.returns(
                this@TabCallbacksTest.trackersAllowList
            )
        }

        browser = mockk {
            every { isDestroyed } returns false
            every { isRestoringPreviousState } returns false
        }

        browserFlow = MutableStateFlow(browser)
        tabList = IncognitoTabList()

        activityCallbackProvider = mockk {
            every { get() } returns this@TabCallbacksTest.activityCallbacks
        }

        navigationController = mockk {
            every { registerNavigationCallback(capture(navigationCallback)) } returns Unit
        }

        tab = mockk {
            every { guid } returns "tab guid"
            every { navigationController } returns this@TabCallbacksTest.navigationController
            every { createFaviconFetcher(any()) } returns faviconFetcher
            every { setErrorPageCallback(any()) } returns Unit
            every { fullscreenCallback = any() } returns Unit
            every { setNewTabCallback(any()) } returns Unit
            every { registerTabCallback(any()) } returns Unit
            every { setContentFilterCallback(any()) } returns Unit
            every { isDestroyed } returns false
        }

        every {
            scriptInjectionManager.injectNavigationCompletedScripts(any(), tab, any())
        } returns Unit
        every { scriptInjectionManager.initializeMessagePassing(tab, any()) } returns Unit

        tabCallbacks = TabCallbacks(
            browserFlow = browserFlow,
            isIncognito = false,
            tab = tab,
            coroutineScope = coroutineScopeRule.scope,
            historyManager = historyManager,
            faviconCache = faviconCache,
            tabList = tabList,
            activityCallbackProvider = activityCallbackProvider,
            registerNewTab = registerNewTab,
            fullscreenCallback = fullscreenCallback,
            cookieCutterModel = cookieCutterModel,
            domainProvider = domainProvider,
            scriptInjectionManager = scriptInjectionManager
        )
    }

    @Test
    fun navigationCallback_afterCompletedNavigation_commitsVisit() {
        val uri = Uri.parse("https://www.example.com")
        val navigation = mockk<Navigation> {
            every { getUri() } returns uri
            every { isDownload } returns false
            every { isErrorPage } returns false
            every { isReload } returns false
            every { isSameDocument } returns false
        }

        every { tab.contentFilterStats } returns emptyMap()
        every { browser.activeTab } answers { tab }

        // Start the navigation.
        navigationCallback.forEach { it.onNavigationStarted(navigation) }

        // Have the NavigationController record the mocked out navigation.
        navigationController.apply {
            every { navigationListSize } returns 1
            every { navigationListCurrentIndex } returns 0
            every { getNavigationEntryDisplayUri(0) } returns uri
            every { getNavigationEntryTitle(0) } returns "navigation title"
        }

        // Complete the navigation.
        navigationCallback.forEach { it.onNavigationCompleted(navigation) }
        verify { scriptInjectionManager.injectNavigationCompletedScripts(any(), tab, any()) }

        coroutineScopeRule.scope.advanceUntilIdle()
        coVerify {
            historyManager.upsert(
                url = uri,
                title = "navigation title",
                visit = any()
            )
        }
    }
}

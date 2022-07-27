package com.neeva.app.cookiecutter

import androidx.compose.runtime.mutableStateOf
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neeva.app.BaseTest
import com.neeva.app.publicsuffixlist.DomainProviderImpl
import kotlinx.coroutines.flow.MutableStateFlow
import org.chromium.weblayer.Browser
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import strikt.api.expectThat
import strikt.assertions.isEqualTo

/**
 * Tests that the TabCookieCutterModel updates the stats properly
 */
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class TabCookieCutterModelTest : BaseTest() {
    private lateinit var model: TabCookieCutterModel
    private lateinit var domainProviderImpl: DomainProviderImpl
    private lateinit var cookieNoticeBlockedFlow: MutableStateFlow<Boolean>

    @Mock
    private lateinit var browser: Browser

    override fun setUp() {
        super.setUp()
        domainProviderImpl = DomainProviderImpl(RuntimeEnvironment.getApplication())
        cookieNoticeBlockedFlow = MutableStateFlow(false)

        model = TabCookieCutterModel(
            browserFlow = MutableStateFlow(null),
            tabId = "tab guid 1",
            trackingDataFlow = MutableStateFlow(null),
            cookieNoticeBlockedFlow = cookieNoticeBlockedFlow,
            enableCookieNoticeSuppression = mutableStateOf(true),
            domainProvider = domainProviderImpl
        )
    }

    override fun tearDown() {
        super.tearDown()
    }

    @Test
    fun testCookieNoticesInModel() {
        // pretend we've blocked a cookie notice
        model.cookieNoticeBlocked = true

        // assert that the model's own state reflects this
        expectThat(model.cookieNoticeBlocked).isEqualTo(true)

        // but we're not the active tab, so make sure the state flow wasn't updated
        expectThat(cookieNoticeBlockedFlow.value).isEqualTo(false)
    }

    @Test
    fun testTrackingStatsInModel() {
        model.updateStats(
            mapOf(
                "1emn.com" to 1,
                "accountkit.com" to 2,
                "ads-twitter.com" to 3
            )
        )
        val trackingData = model.currentTrackingData()
        expectThat(trackingData.numTrackers).isEqualTo(6)
        expectThat(trackingData.numDomains).isEqualTo(3)
        expectThat(trackingData.trackingEntities.size).isEqualTo(3)

        model.resetStat()
        val emptyTrackingData = model.currentTrackingData()
        expectThat(emptyTrackingData.numTrackers).isEqualTo(0)
        expectThat(emptyTrackingData.numDomains).isEqualTo(0)
        expectThat(emptyTrackingData.trackingEntities.size).isEqualTo(0)
    }
}

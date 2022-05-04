package com.neeva.app.browsing

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.neeva.app.BaseTest
import com.neeva.app.CoroutineScopeRule
import com.neeva.app.Dispatchers
import com.neeva.app.LoadingState
import com.neeva.app.history.HistoryManager
import com.neeva.app.logging.ClientLogger
import com.neeva.app.publicsuffixlist.DomainProviderImpl
import com.neeva.app.settings.SettingsDataModel
import com.neeva.app.settings.SettingsToggle
import com.neeva.app.userdata.NeevaUser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import org.chromium.weblayer.Callback
import org.chromium.weblayer.Profile
import org.chromium.weblayer.WebLayer
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNull
import strikt.assertions.isTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class WebLayerModelTest : BaseTest() {
    @Rule
    @JvmField
    val coroutineScopeRule = CoroutineScopeRule()

    private lateinit var activityCallbackProvider: ActivityCallbackProvider
    private lateinit var application: Application
    private lateinit var browserWrapperFactory: BrowserWrapperFactory
    private lateinit var webLayer: WebLayer

    @Mock private lateinit var activityCallbacks: ActivityCallbacks
    @Mock private lateinit var cacheCleaner: CacheCleaner
    @Mock private lateinit var clientLogger: ClientLogger
    @Mock private lateinit var domainProviderImpl: DomainProviderImpl
    @Mock private lateinit var historyManager: HistoryManager
    @Mock private lateinit var incognitoBrowserWrapper: IncognitoBrowserWrapper
    @Mock private lateinit var incognitoProfile: Profile
    @Mock private lateinit var neevaUser: NeevaUser
    @Mock private lateinit var regularBrowserWrapper: RegularBrowserWrapper
    @Mock private lateinit var regularProfile: Profile
    @Mock private lateinit var settingsDataModel: SettingsDataModel
    @Mock private lateinit var webLayerFactory: WebLayerFactory

    private lateinit var webLayerModel: WebLayerModel

    override fun setUp() {
        super.setUp()

        application = ApplicationProvider.getApplicationContext()

        activityCallbackProvider = mock {
            on { get() } doReturn activityCallbacks
        }

        browserWrapperFactory = mock {
            on { createRegularBrowser(any()) } doReturn regularBrowserWrapper
            on { createIncognitoBrowser(any(), any()) } doReturn incognitoBrowserWrapper
        }

        webLayer = mock {
            on {
                getProfile(eq(RegularBrowserWrapper.NON_INCOGNITO_PROFILE_NAME))
            } doReturn regularProfile

            on {
                getIncognitoProfile(eq(IncognitoBrowserWrapper.INCOGNITO_PROFILE_NAME))
            } doReturn incognitoProfile
        }

        webLayerModel = WebLayerModel(
            activityCallbackProvider = activityCallbackProvider,
            browserWrapperFactory = browserWrapperFactory,
            webLayerFactory = webLayerFactory,
            application = application,
            cacheCleaner = cacheCleaner,
            domainProviderImpl = domainProviderImpl,
            historyManager = historyManager,
            dispatchers = Dispatchers(
                main = StandardTestDispatcher(coroutineScopeRule.scope.testScheduler),
                io = StandardTestDispatcher(coroutineScopeRule.scope.testScheduler)
            ),
            neevaUser = neevaUser,
            settingsDataModel = settingsDataModel,
            clientLogger = clientLogger,
            overrideCoroutineScope = coroutineScopeRule.scope
        )
    }

    private fun completeWebLayerInitialization() {
        val loadCallback = argumentCaptor<Callback<WebLayer>>()
        verify(webLayerFactory).load(loadCallback.capture())
        loadCallback.lastValue.onResult(webLayer)
        coroutineScopeRule.scope.advanceUntilIdle()
    }

    @Test
    fun testInitializationFlow() {
        expectThat(webLayerModel.initializationState.value).isEqualTo(LoadingState.LOADING)

        // Allow the coroutines to run, which should allow initialization to finish.
        coroutineScopeRule.scope.advanceUntilIdle()

        // Confirm that the other initialization tasks were run.
        runBlocking {
            verify(domainProviderImpl).initialize()
            verify(historyManager).pruneDatabase()
        }

        // Fire the callback WebLayerModel requires to store the WebLayer.
        completeWebLayerInitialization()

        // Because an Incognito Fragment couldn't be found, the cacheCleaner should have tried to
        // clean up everything.
        runBlocking { verify(cacheCleaner).run() }

        coroutineScopeRule.scope.advanceUntilIdle()
        expectThat(webLayerModel.initializationState.value).isEqualTo(LoadingState.READY)

        val browsers = webLayerModel.browsersFlow.value
        expectThat(browsers.isCurrentlyIncognito).isFalse()
        expectThat(browsers.incognitoBrowserWrapper).isNull()
    }

    @Test
    fun switchToProfile_whenSwitchingBetweenProfiles_reactsCorrectly() {
        completeWebLayerInitialization()

        webLayerModel.switchToProfile(true)
        coroutineScopeRule.scope.advanceUntilIdle()
        verify(clientLogger).onProfileSwitch(eq(true))
        expectThat(webLayerModel.browsersFlow.value.isCurrentlyIncognito).isEqualTo(true)
        expectThat(webLayerModel.currentBrowser).isEqualTo(incognitoBrowserWrapper)

        webLayerModel.switchToProfile(false)
        coroutineScopeRule.scope.advanceUntilIdle()
        verify(clientLogger).onProfileSwitch(eq(false))
        expectThat(webLayerModel.browsersFlow.value.isCurrentlyIncognito).isEqualTo(false)
        expectThat(webLayerModel.currentBrowser).isEqualTo(regularBrowserWrapper)
    }

    @Test
    fun switchToProfile_withoutIncognitoProfile_createsAndDeletesIncognitoProfile() {
        completeWebLayerInitialization()
        webLayerModel.switchToProfile(true)

        // The Incognito Browser should have been created.
        val onDestroyedCaptor = argumentCaptor<(IncognitoBrowserWrapper) -> Unit>()
        verify(clientLogger).onProfileSwitch(eq(true))
        verify(browserWrapperFactory).createIncognitoBrowser(any(), onDestroyedCaptor.capture())

        val browsersBefore = webLayerModel.browsersFlow.value
        expectThat(browsersBefore.isCurrentlyIncognito).isTrue()
        expectThat(browsersBefore.incognitoBrowserWrapper).isEqualTo(incognitoBrowserWrapper)

        // Signal that the Incognito profile was destroyed.
        onDestroyedCaptor.lastValue.invoke(incognitoBrowserWrapper)

        val browsersAfter = webLayerModel.browsersFlow.value
        expectThat(browsersAfter.incognitoBrowserWrapper).isEqualTo(null)
        expectThat(browsersAfter.isCurrentlyIncognito).isFalse()
    }

    @Test
    fun switchToProfile_withoutIncognitoProfile_createsIncognitoOnlyIfNull() {
        completeWebLayerInitialization()
        webLayerModel.switchToProfile(true)

        // The Incognito Browser should have been created.
        verify(clientLogger).onProfileSwitch(eq(true))
        verify(browserWrapperFactory, times(1)).createIncognitoBrowser(any(), any())

        val browsersBefore = webLayerModel.browsersFlow.value
        expectThat(browsersBefore.isCurrentlyIncognito).isTrue()
        expectThat(browsersBefore.incognitoBrowserWrapper).isEqualTo(incognitoBrowserWrapper)

        // Switch to the regular profile.
        webLayerModel.switchToProfile(false)
        expectThat(webLayerModel.browsersFlow.value.isCurrentlyIncognito).isFalse()

        // Switch back to Incognito.
        webLayerModel.switchToProfile(true)
        expectThat(webLayerModel.browsersFlow.value.isCurrentlyIncognito).isTrue()

        // Verify that the previous Incognito BrowserWrapper was re-used.
        verify(browserWrapperFactory, times(1)).createIncognitoBrowser(any(), any())

        val browsersAfter = webLayerModel.browsersFlow.value
        expectThat(browsersAfter.incognitoBrowserWrapper).isEqualTo(incognitoBrowserWrapper)
        expectThat(browsersAfter.isCurrentlyIncognito).isTrue()
    }

    @Test
    fun switchToProfile_whenSettingToggledOn_closesTabsOnSwitch() {
        completeWebLayerInitialization()
        webLayerModel.switchToProfile(true)

        // The Incognito Browser should have been created.
        verify(clientLogger).onProfileSwitch(eq(true))
        verify(browserWrapperFactory, times(1)).createIncognitoBrowser(any(), any())

        val browsersBefore = webLayerModel.browsersFlow.value
        expectThat(browsersBefore.isCurrentlyIncognito).isTrue()
        expectThat(browsersBefore.incognitoBrowserWrapper).isEqualTo(incognitoBrowserWrapper)

        Mockito.`when`(
            settingsDataModel.getSettingsToggleValue(eq(SettingsToggle.CLOSE_INCOGNITO_TABS))
        ).thenReturn(true)

        // Switch to the regular profile.
        webLayerModel.switchToProfile(false)
        expectThat(webLayerModel.browsersFlow.value.isCurrentlyIncognito).isFalse()

        verify(incognitoBrowserWrapper).closeAllTabs()
    }

    @Test
    fun switchToProfile_whenSettingToggledOff_closesTabsOnSwitch() {
        completeWebLayerInitialization()
        webLayerModel.switchToProfile(true)

        // The Incognito Browser should have been created.
        verify(clientLogger).onProfileSwitch(eq(true))
        verify(browserWrapperFactory, times(1)).createIncognitoBrowser(any(), any())

        val browsersBefore = webLayerModel.browsersFlow.value
        expectThat(browsersBefore.isCurrentlyIncognito).isTrue()
        expectThat(browsersBefore.incognitoBrowserWrapper).isEqualTo(incognitoBrowserWrapper)

        Mockito.`when`(
            settingsDataModel.getSettingsToggleValue(eq(SettingsToggle.CLOSE_INCOGNITO_TABS))
        ).thenReturn(false)

        // Switch to the regular profile.
        webLayerModel.switchToProfile(false)
        expectThat(webLayerModel.browsersFlow.value.isCurrentlyIncognito).isFalse()

        verify(incognitoBrowserWrapper, never()).closeAllTabs()
    }
}

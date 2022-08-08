package com.neeva.app.appnav

import androidx.navigation.NavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.neeva.app.BaseTest
import com.neeva.app.CoroutineScopeRule
import com.neeva.app.NeevaConstants
import com.neeva.app.browsing.BrowserWrapper
import com.neeva.app.browsing.GoBackResult
import com.neeva.app.browsing.WebLayerModel
import com.neeva.app.spaces.SpaceStore
import com.neeva.app.ui.PopupModel
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class AppNavModelImplTest : BaseTest() {
    @get:Rule
    val coroutineScopeRule = CoroutineScopeRule()

    @MockK lateinit var onTakeScreenshot: (callback: () -> Unit) -> Unit
    @MockK lateinit var popupModel: PopupModel
    @MockK lateinit var spaceStore: SpaceStore

    private lateinit var mockCurrentBrowser: BrowserWrapper
    private lateinit var mockInitializedBrowserFlow: MutableStateFlow<BrowserWrapper>

    private var mockCurrentDestination: AppNavDestination? = null
    private lateinit var navHostController: NavHostController

    private lateinit var neevaConstants: NeevaConstants
    private lateinit var webLayerModel: WebLayerModel

    private lateinit var appNavModelImpl: AppNavModelImpl

    override fun setUp() {
        super.setUp()

        mockCurrentDestination = null
        navHostController = mockk {
            every { enableOnBackPressed(any()) } returns Unit
            every { addOnDestinationChangedListener(any()) } returns Unit
            every { currentDestination } answers { mockCurrentDestination }
        }

        neevaConstants = NeevaConstants()

        mockCurrentBrowser = mockk {
            every { isIncognito } returns false
            every { userMustStayInCardGridFlow } returns MutableStateFlow(false)
        }
        mockInitializedBrowserFlow = MutableStateFlow(mockCurrentBrowser)
        webLayerModel = mockk {
            every { initializedBrowserFlow } answers { mockInitializedBrowserFlow }
            every { currentBrowser } answers { mockCurrentBrowser }
        }

        appNavModelImpl = AppNavModelImpl(
            context = InstrumentationRegistry.getInstrumentation().targetContext,
            navController = navHostController,
            webLayerModel = webLayerModel,
            coroutineScope = coroutineScopeRule.scope,
            dispatchers = coroutineScopeRule.dispatchers,
            popupModel = popupModel,
            spaceStore = spaceStore,
            onTakeScreenshot = onTakeScreenshot,
            neevaConstants = neevaConstants
        )
    }

    @Test
    fun navigateBackOnActiveTab_givenNoSpaceId_doesNothing() {
        every { mockCurrentBrowser.goBack() } returns GoBackResult()

        appNavModelImpl.navigateBackOnActiveTab()
        coroutineScopeRule.advanceUntilIdle()

        verify(exactly = 0) { spaceStore.detailedSpaceIDFlow }
        verify(exactly = 0) { navHostController.navigate(any<String>()) }
    }

    @Test
    fun navigateBackOnActiveTab_givenSpaceId_opensSpace() {
        every { mockCurrentBrowser.goBack() } returns GoBackResult(
            spaceIdToOpen = "space_id"
        )
        every {
            navHostController.navigate(
                route = "${AppNavDestination.SPACE_DETAIL.route}/space_id",
                builder = any()
            )
        } returns Unit

        val spaceIdFlow = MutableStateFlow<String?>(null)
        every { spaceStore.detailedSpaceIDFlow } answers { spaceIdFlow }

        appNavModelImpl.navigateBackOnActiveTab()
        coroutineScopeRule.advanceUntilIdle()

        expectThat(spaceIdFlow.value).isEqualTo("space_id")
        verify {
            navHostController.navigate(
                route = "${AppNavDestination.SPACE_DETAIL.route}/space_id",
                builder = any()
            )
        }
    }
}

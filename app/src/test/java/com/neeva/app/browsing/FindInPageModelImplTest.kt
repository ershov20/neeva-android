package com.neeva.app.browsing

import com.neeva.app.BaseTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.chromium.weblayer.FindInPageCallback
import org.chromium.weblayer.FindInPageController
import org.chromium.weblayer.Tab
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class FindInPageModelImplTest : BaseTest() {

    private lateinit var findInPageController: FindInPageController
    private lateinit var tab: Tab

    private lateinit var model: FindInPageModelImpl

    override fun setUp() {
        super.setUp()

        findInPageController = mock()
        tab = mock {
            on { getFindInPageController() } doReturn findInPageController
        }

        model = FindInPageModelImpl()
    }

    @Test
    fun findInPage_textIsNullByDefault() {
        expectThat(model.findInPageInfo.value).isEqualTo(FindInPageInfo())
    }

    @Test
    fun findInPage_whenNotShown_textIsNullWithNonNullInput() {
        // Skip calling model.showFindInPage(tab).
        model.updateFindInPageQuery(tab, "test")
        expectThat(model.findInPageInfo.value).isEqualTo(FindInPageInfo())
    }

    @Test
    fun findInPage_whenShown_textIsEmpty() {
        model.showFindInPage(tab)
        expectThat(model.findInPageInfo.value).isEqualTo(FindInPageInfo(text = ""))
    }

    @Test
    fun findInPage_whenShownWithTab_textIsPropagated() {
        verify(findInPageController, times(0)).setFindInPageCallback(any())

        model.showFindInPage(tab)

        val findInPageCallback = argumentCaptor<FindInPageCallback>()
        verify(findInPageController, times(1))
            .setFindInPageCallback(findInPageCallback.capture())

        val query = "test"
        model.showFindInPage(tab)
        model.updateFindInPageQuery(tab, query)
        verify(tab.findInPageController, times(1)).find(query, true)
        expectThat(model.findInPageInfo.value.text).isEqualTo(query)
    }

    @Test
    fun findInPage_whenShownWithTab_callbackUpdatesInfo() {
        model.showFindInPage(tab)

        val findInPageCallback = argumentCaptor<FindInPageCallback>()
        verify(findInPageController, times(1))
            .setFindInPageCallback(findInPageCallback.capture())

        findInPageCallback.lastValue.onFindResult(5, 2, false)
        expectThat(model.findInPageInfo.value).isEqualTo(FindInPageInfo("", 2, 5, false))
    }
}

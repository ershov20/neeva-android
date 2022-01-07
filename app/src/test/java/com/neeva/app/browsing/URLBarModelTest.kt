package com.neeva.app.browsing

import android.net.Uri
import com.neeva.app.BaseTest
import com.neeva.app.CoroutineScopeRule
import com.neeva.app.urlbar.URLBarModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
@OptIn(ExperimentalCoroutinesApi::class)
class URLBarModelTest : BaseTest() {
    @Rule @JvmField
    val coroutineScopeRule = CoroutineScopeRule()

    private lateinit var urlFlow: MutableStateFlow<Uri>
    private lateinit var activeTabModel: ActiveTabModel
    private lateinit var model: URLBarModel

    private val urlBarModelText: String
        get() = model.userInputText.value.text

    override fun setUp() {
        super.setUp()
        urlFlow = MutableStateFlow(Uri.EMPTY)

        activeTabModel = mock()
        Mockito.`when`(activeTabModel.urlFlow).thenReturn(urlFlow)

        model = URLBarModel(activeTabModel)
    }

    @Test
    fun loadUrl_withoutLazyTab() {
        val uri = Uri.parse("https://www.reddit.com/r/android")
        model.loadUrl(uri)
        verify(activeTabModel, times(1)).loadUrl(eq(uri), eq(false))
    }

    @Test
    fun loadUrl_withLazyTab() {
        // Open a lazy tab.
        model.openLazyTab()
        expectThat(urlBarModelText).isEqualTo("")

        // Loading the URL should send it to a new tab.
        val uri = Uri.parse("https://www.reddit.com/r/android")
        model.loadUrl(uri)
        verify(activeTabModel, times(1)).loadUrl(eq(uri), eq(true))
    }

    @Test
    fun reload() {
        model.reload()
        verify(activeTabModel, times(1)).reload()
    }

    @Test
    fun replaceLocationBarText() {
        // Focus the URL bar so that it can be edited.  This should normally be called when the
        // Composable representing the URL bar triggers it.
        model.onFocusChanged(true)
        model.replaceLocationBarText("random query")
        expectThat(urlBarModelText).isEqualTo("random query")

        model.replaceLocationBarText("query text")
        expectThat(urlBarModelText).isEqualTo("query text")
    }

    @Test
    fun onFocusChanged_withLazyTabAndThenUnfocusing_stopsLazyTab() {
        model.openLazyTab()
        expectThat(model.isLazyTab.value).isEqualTo(true)
        model.onFocusChanged(false)
        expectThat(model.isLazyTab.value).isEqualTo(false)
    }

    @Test
    fun onFocusChanged() {
        // When the bar is focused, remove whatever text was being displayed.
        model.onFocusChanged(true)
        expectThat(urlBarModelText).isEqualTo("")
        expectThat(model.isEditing.value).isTrue()

        model.replaceLocationBarText("reddit.com/r/android")
        expectThat(urlBarModelText).isEqualTo("reddit.com/r/android")

        // When the bar is unfocused, it should return to showing the webpage domain.
        model.onFocusChanged(false)
        expectThat(model.isEditing.value).isFalse()
    }
}

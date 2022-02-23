package com.neeva.app.urlbar

import android.net.Uri
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.test.core.app.ApplicationProvider
import com.neeva.app.BaseTest
import com.neeva.app.CoroutineScopeRule
import com.neeva.app.Dispatchers
import com.neeva.app.browsing.ActiveTabModel
import com.neeva.app.browsing.toSearchUri
import com.neeva.app.storage.favicons.FaviconCache
import com.neeva.app.suggestions.NavSuggestion
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.robolectric.RobolectricTestRunner
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNull
import strikt.assertions.isTrue

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class URLBarModelImplTest : BaseTest() {
    @Rule @JvmField
    val coroutineScopeRule = CoroutineScopeRule()

    @Mock private lateinit var faviconCache: FaviconCache

    private lateinit var dispatchers: Dispatchers
    private lateinit var suggestionFlow: MutableStateFlow<NavSuggestion?>
    private lateinit var urlFlow: MutableStateFlow<Uri>
    private lateinit var activeTabModel: ActiveTabModel
    private lateinit var model: URLBarModelImpl

    private val urlBarModelText: String
        get() = model.state.value.userTypedInput

    override fun setUp() {
        super.setUp()

        dispatchers = Dispatchers(
            main = StandardTestDispatcher(coroutineScopeRule.scope.testScheduler),
            io = StandardTestDispatcher(coroutineScopeRule.scope.testScheduler),
        )
        suggestionFlow = MutableStateFlow(null)
        urlFlow = MutableStateFlow(Uri.EMPTY)

        activeTabModel = mock()
        Mockito.`when`(activeTabModel.urlFlow).thenReturn(urlFlow)

        model = URLBarModelImpl(
            suggestionFlow = suggestionFlow,
            appContext = ApplicationProvider.getApplicationContext(),
            coroutineScope = coroutineScopeRule.scope,
            faviconCache = faviconCache,
            dispatchers = dispatchers
        )
    }

    /* Disabled: Functionality was moved to BrowserWrapper, which isn't easy to test at the moment.
    @Test
    fun loadUrl_withoutLazyTab() {
        val uri = Uri.parse("https://www.reddit.com/r/android")
        model.loadUrl(uri)
        verify(activeTabModel, times(1)).loadUrl(eq(uri), eq(false), eq(false))
    }

    @Test
    fun loadUrl_withLazyTab() {
        // Open a lazy tab.
        model.openLazyTab()
        expectThat(urlBarModelText).isEqualTo("")

        // Loading the URL should send it to a new tab.
        val uri = Uri.parse("https://www.reddit.com/r/android")
        model.loadUrl(uri)
        verify(activeTabModel, times(1)).loadUrl(eq(uri), eq(true), eq(false))
    }

    @Test
    fun onFocusChanged_withLazyTabAndThenUnfocusing_stopsLazyTab() {
        model.openLazyTab()
        expectThat(model.isLazyTab.value).isEqualTo(true)
        model.onFocusChanged(false)
        expectThat(model.isLazyTab.value).isEqualTo(false)
    }
    */

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
    fun onFocusChanged() {
        // When the bar is focused, remove whatever text was being displayed.
        model.onFocusChanged(true)
        expectThat(urlBarModelText).isEqualTo("")
        expectThat(model.state.value.isEditing).isTrue()

        model.replaceLocationBarText("reddit.com/r/android")
        expectThat(urlBarModelText).isEqualTo("reddit.com/r/android")

        // When the bar is unfocused, it should return to showing the webpage domain.
        model.onFocusChanged(false)
        expectThat(model.state.value.isEditing).isFalse()
    }

    @Test
    fun determineUrlBarText_withMatch_showsAutocompletedText() {
        val autocompleteSuggestion = NavSuggestion(
            url = Uri.parse("https://www.reddit.com/r/android"),
            label = "Primary label",
            secondaryLabel = "https://www.reddit.com/r/android"
        )

        val inputState = URLBarModelState(
            isEditing = true,
            allowAutocomplete = true,
            userTypedInput = "redd"
        )

        val actualValue = runBlocking {
            model.determineDisplayState(autocompleteSuggestion, inputState)
        }
        expectThat(actualValue.textFieldValue.text).isEqualTo("reddit.com/r/android")
        expectThat(actualValue.textFieldValue.selection)
            .isEqualTo(TextRange(4, "reddit.com/r/android".length))
    }

    @Test
    fun determineUrlBarText_withoutMatch_returnsNull() {
        val autocompleteSuggestion = NavSuggestion(
            url = Uri.parse("https://www.reddit.com/r/android"),
            label = "Primary label",
            secondaryLabel = "https://www.reddit.com/r/android"
        )

        val inputState = URLBarModelState(
            isEditing = true,
            allowAutocomplete = true,
            userTypedInput = "not a match",
            textFieldValue = TextFieldValue("not a match")
        )

        val actualValue = runBlocking {
            model.determineDisplayState(autocompleteSuggestion, inputState)
        }
        expectThat(actualValue.textFieldValue.text).isEqualTo("not a match")
    }

    @Test
    fun getUrlToLoad_withUrl_returnsUrl() {
        expectThat(
            URLBarModelImpl.getUrlToLoad("https://www.url.bar.contents.com").toString()
        ).isEqualTo("https://www.url.bar.contents.com")
    }

    @Test
    fun getUrlToLoad_withQuery_returnsUrl() {
        expectThat(
            URLBarModelImpl.getUrlToLoad("query text")
        ).isEqualTo("query text".toSearchUri())
    }

    @Test
    fun getAutocompleteText_withNoSubdomain() {
        val autocompleteSuggestion = NavSuggestion(
            url = Uri.parse("https://www.reddit.com/r/android"),
            label = "Primary label",
            secondaryLabel = "https://www.reddit.com/r/android"
        )

        expectThat(URLBarModelImpl.computeAutocompleteText(autocompleteSuggestion, "http"))
            .isEqualTo("https://www.reddit.com/r/android")
        expectThat(URLBarModelImpl.computeAutocompleteText(autocompleteSuggestion, "redd"))
            .isEqualTo("reddit.com/r/android")
        expectThat(URLBarModelImpl.computeAutocompleteText(autocompleteSuggestion, "mismatch"))
            .isNull()
        expectThat(URLBarModelImpl.computeAutocompleteText(autocompleteSuggestion, "com")).isNull()
    }

    @Test
    fun getAutocompleteText_withSubdomain() {
        val autocompleteSuggestion = NavSuggestion(
            url = Uri.parse("https://news.google.com"),
            label = "Primary label",
            secondaryLabel = "https://news.google.com"
        )

        expectThat(URLBarModelImpl.computeAutocompleteText(autocompleteSuggestion, "http"))
            .isEqualTo("https://news.google.com")
        expectThat(URLBarModelImpl.computeAutocompleteText(autocompleteSuggestion, "news"))
            .isEqualTo("news.google.com")
        expectThat(URLBarModelImpl.computeAutocompleteText(autocompleteSuggestion, "google"))
            .isNull()
        expectThat(URLBarModelImpl.computeAutocompleteText(autocompleteSuggestion, "mismatch"))
            .isNull()
        expectThat(URLBarModelImpl.computeAutocompleteText(autocompleteSuggestion, "com"))
            .isNull()
    }
}

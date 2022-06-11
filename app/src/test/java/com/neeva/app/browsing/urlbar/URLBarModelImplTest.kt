package com.neeva.app.browsing.urlbar

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.test.core.app.ApplicationProvider
import com.neeva.app.BaseTest
import com.neeva.app.CoroutineScopeRule
import com.neeva.app.Dispatchers
import com.neeva.app.NeevaConstants
import com.neeva.app.browsing.ActiveTabModel
import com.neeva.app.browsing.toSearchUri
import com.neeva.app.storage.favicons.FaviconCache
import com.neeva.app.suggestions.NavSuggestion
import com.neeva.app.suggestions.SuggestionType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
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

    private lateinit var activeTabModel: ActiveTabModel
    private lateinit var dispatchers: Dispatchers
    private lateinit var model: URLBarModelImpl
    private lateinit var neevaConstants: NeevaConstants
    private lateinit var suggestionFlow: MutableStateFlow<NavSuggestion?>
    private lateinit var urlFlow: MutableStateFlow<Uri>

    private val urlBarModelText: String
        get() = model.stateFlow.value.userTypedInput

    @Mock private lateinit var faviconCache: FaviconCache

    override fun setUp() {
        super.setUp()

        neevaConstants = NeevaConstants()

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
            dispatchers = dispatchers,
            neevaConstants = neevaConstants
        )
    }

    @Test
    fun onLocationBarTextChanged_withoutFocus_doesNothing() {
        expectThat(model.stateFlow.value.isEditing).isFalse()
        expectThat(model.stateFlow.value.textFieldValue.text).isEqualTo("")

        model.onLocationBarTextChanged(TextFieldValue("no effect"))
        expectThat(model.stateFlow.value.textFieldValue.text).isEqualTo("")
    }

    @Test
    fun onLocationBarTextChanged_withFocus_setsText() {
        model.showZeroQuery()
        expectThat(model.stateFlow.value.isEditing).isTrue()
        expectThat(model.stateFlow.value.isAutocompleteAllowed).isTrue()
        expectThat(model.stateFlow.value.textFieldValue.text).isEqualTo("")

        model.onLocationBarTextChanged(TextFieldValue("new text"))
        expectThat(model.stateFlow.value.textFieldValue.text).isEqualTo("new text")
    }

    @Test
    fun onLocationBarTextChanged_whenRemovingCharacterAndValidSuggestion_deletesSuggestion() {
        model.showZeroQuery()
        expectThat(model.stateFlow.value.isEditing).isTrue()
        expectThat(model.stateFlow.value.isAutocompleteAllowed).isTrue()
        expectThat(model.stateFlow.value.textFieldValue.text).isEqualTo("")

        model.onLocationBarTextChanged(TextFieldValue("exam"))

        // Indicate that a new suggestion has come down the pipeline that matches the user query.
        val mockBitmap = mock<Bitmap>()
        runBlocking {
            Mockito.`when`(
                faviconCache.getFavicon(
                    eq(Uri.parse("https://www.example.com")),
                    eq(false)
                )
            ).thenReturn(mockBitmap)
        }
        suggestionFlow.value = NavSuggestion(
            url = Uri.parse("https://www.example.com"),
            label = "",
            secondaryLabel = "",
            queryIndex = null,
            type = SuggestionType.AUTOCOMPLETE_SUGGESTION
        )
        coroutineScopeRule.scope.advanceUntilIdle()

        // Confirm that everything looks good.
        model.stateFlow.value.let {
            expectThat(it.textFieldValue.text).isEqualTo("exam")
            expectThat(it.autocompleteSuggestion).isEqualTo("example.com")
            expectThat(it.autocompleteSuggestionText).isEqualTo("ple.com")
            expectThat(it.uriToLoad).isEqualTo(Uri.parse("https://www.example.com"))
            expectThat(it.faviconBitmap).isEqualTo(mockBitmap)
        }
        runBlocking {
            verify(faviconCache, times(1)).getFavicon(
                eq(Uri.parse("https://www.example.com")),
                eq(false)
            )
        }

        // Delete a character from the current query.  Because there is an autocomplete suggestion,
        // the text change should be rejected and the existing suggestion should be removed.
        model.onLocationBarTextChanged(TextFieldValue("exa"))
        model.stateFlow.value.let {
            expectThat(it.textFieldValue.text).isEqualTo("exam")
            expectThat(it.autocompleteSuggestion).isNull()
            expectThat(it.autocompleteSuggestionText).isNull()
            expectThat(it.uriToLoad).isEqualTo("exam".toSearchUri(neevaConstants))
            expectThat(it.faviconBitmap).isEqualTo(null)
        }

        // We shouldn't have tried to get the favicon again.
        runBlocking {
            verify(faviconCache, times(1)).getFavicon(
                eq(Uri.parse("https://www.example.com")),
                eq(false)
            )
        }
    }

    @Test
    fun onLocationBarTextChanged_whenRemovingCharacterAndNoSuggestion_acceptsTextChange() {
        model.showZeroQuery()
        expectThat(model.stateFlow.value.isEditing).isTrue()
        expectThat(model.stateFlow.value.isAutocompleteAllowed).isTrue()
        expectThat(model.stateFlow.value.textFieldValue.text).isEqualTo("")

        model.onLocationBarTextChanged(TextFieldValue("exam"))

        // Confirm that everything looks good.
        model.stateFlow.value.let {
            expectThat(it.textFieldValue.text).isEqualTo("exam")
            expectThat(it.autocompleteSuggestion).isNull()
            expectThat(it.autocompleteSuggestionText).isNull()
            expectThat(it.uriToLoad).isEqualTo("exam".toSearchUri(neevaConstants))
            expectThat(it.faviconBitmap).isEqualTo(null)
        }

        // Delete a character from the current query.  Because there is no autocomplete suggestion,
        // the text change should be accepted
        model.onLocationBarTextChanged(TextFieldValue("exa"))
        model.stateFlow.value.let {
            expectThat(it.textFieldValue.text).isEqualTo("exa")
            expectThat(it.autocompleteSuggestion).isNull()
            expectThat(it.autocompleteSuggestionText).isNull()
            expectThat(it.uriToLoad).isEqualTo("exa".toSearchUri(neevaConstants))
            expectThat(it.faviconBitmap).isEqualTo(null)
        }
    }

    @Test
    fun onLocationBarTextChanged_whenAddingCharacterAndValidSuggestion_keepsSameSuggestion() {
        model.showZeroQuery()
        expectThat(model.stateFlow.value.isEditing).isTrue()
        expectThat(model.stateFlow.value.isAutocompleteAllowed).isTrue()
        expectThat(model.stateFlow.value.textFieldValue.text).isEqualTo("")

        model.onLocationBarTextChanged(TextFieldValue("exam"))

        // Indicate that a new suggestion has come down the pipeline that matches the user query.
        val mockBitmap = mock<Bitmap>()
        runBlocking {
            Mockito.`when`(
                faviconCache.getFavicon(
                    eq(Uri.parse("https://www.example.com")),
                    eq(false)
                )
            ).thenReturn(mockBitmap)
        }
        suggestionFlow.value = NavSuggestion(
            url = Uri.parse("https://www.example.com"),
            label = "",
            secondaryLabel = "",
            queryIndex = null,
            type = SuggestionType.AUTOCOMPLETE_SUGGESTION
        )
        coroutineScopeRule.scope.advanceUntilIdle()

        // Confirm that everything looks good.
        model.stateFlow.value.let {
            expectThat(it.textFieldValue.text).isEqualTo("exam")
            expectThat(it.autocompleteSuggestion).isEqualTo("example.com")
            expectThat(it.autocompleteSuggestionText).isEqualTo("ple.com")
            expectThat(it.uriToLoad).isEqualTo(Uri.parse("https://www.example.com"))
            expectThat(it.faviconBitmap).isEqualTo(mockBitmap)
        }
        runBlocking {
            verify(faviconCache, times(1)).getFavicon(
                eq(Uri.parse("https://www.example.com")),
                eq(false)
            )
        }

        // Add a character that matches the same autocomplete suggestion.
        model.onLocationBarTextChanged(TextFieldValue("examp"))
        model.stateFlow.value.let {
            expectThat(it.textFieldValue.text).isEqualTo("examp")
            expectThat(it.autocompleteSuggestion).isEqualTo("example.com")
            expectThat(it.autocompleteSuggestionText).isEqualTo("le.com")
            expectThat(it.uriToLoad).isEqualTo(Uri.parse("https://www.example.com"))
            expectThat(it.faviconBitmap).isEqualTo(mockBitmap)
        }

        // We shouldn't have tried to get the favicon again.
        runBlocking {
            verify(faviconCache, times(1)).getFavicon(
                eq(Uri.parse("https://www.example.com")),
                eq(false)
            )
        }
    }

    @Test
    fun onLocationBarTextChanged_whenAddingCharacterAndInvalidSuggestion_removesSuggestion() {
        model.showZeroQuery()
        expectThat(model.stateFlow.value.isEditing).isTrue()
        expectThat(model.stateFlow.value.isAutocompleteAllowed).isTrue()
        expectThat(model.stateFlow.value.textFieldValue.text).isEqualTo("")

        model.onLocationBarTextChanged(TextFieldValue("exam"))

        // Indicate that a new suggestion has come down the pipeline that matches the user query.
        val mockBitmap = mock<Bitmap>()
        runBlocking {
            Mockito.`when`(
                faviconCache.getFavicon(
                    eq(Uri.parse("https://www.example.com")),
                    eq(false)
                )
            ).thenReturn(mockBitmap)
        }
        suggestionFlow.value = NavSuggestion(
            url = Uri.parse("https://www.example.com"),
            label = "",
            secondaryLabel = "",
            queryIndex = null,
            type = SuggestionType.AUTOCOMPLETE_SUGGESTION
        )
        coroutineScopeRule.scope.advanceUntilIdle()

        // Confirm that everything looks good.
        model.stateFlow.value.let {
            expectThat(it.textFieldValue.text).isEqualTo("exam")
            expectThat(it.autocompleteSuggestion).isEqualTo("example.com")
            expectThat(it.autocompleteSuggestionText).isEqualTo("ple.com")
            expectThat(it.uriToLoad).isEqualTo(Uri.parse("https://www.example.com"))
            expectThat(it.faviconBitmap).isEqualTo(mockBitmap)
        }
        runBlocking {
            verify(faviconCache, times(1)).getFavicon(
                eq(Uri.parse("https://www.example.com")),
                eq(false)
            )
        }

        // Add a character that no longer matches the suggestion.
        model.onLocationBarTextChanged(TextFieldValue("exam_"))
        model.stateFlow.value.let {
            expectThat(it.textFieldValue.text).isEqualTo("exam_")
            expectThat(it.autocompleteSuggestion).isNull()
            expectThat(it.autocompleteSuggestionText).isNull()
            expectThat(it.uriToLoad).isEqualTo("exam_".toSearchUri(neevaConstants))
            expectThat(it.faviconBitmap).isEqualTo(null)
        }

        // We shouldn't have tried to get the favicon because the suggestion flow hasn't updated.
        runBlocking {
            verify(faviconCache, times(1)).getFavicon(
                eq(Uri.parse("https://www.example.com")),
                eq(false)
            )
        }
    }

    @Test
    fun onLocationBarTextChanged_whenOnlyCompositionChanges_stillAllowsAutocomplete() {
        model.showZeroQuery()
        expectThat(model.stateFlow.value.isEditing).isTrue()
        expectThat(model.stateFlow.value.isAutocompleteAllowed).isTrue()
        expectThat(model.stateFlow.value.textFieldValue.text).isEqualTo("")

        val initialTextFieldValue = TextFieldValue("new text")
        model.onLocationBarTextChanged(initialTextFieldValue)
        expectThat(model.stateFlow.value.textFieldValue.text).isEqualTo("new text")
        expectThat(model.stateFlow.value.isAutocompleteAllowed).isTrue()

        val newTextFieldValue = initialTextFieldValue.copy(
            composition = TextRange(0, initialTextFieldValue.text.length)
        )
        model.onLocationBarTextChanged(newTextFieldValue)
        expectThat(model.stateFlow.value.textFieldValue.text).isEqualTo("new text")
        expectThat(model.stateFlow.value.isAutocompleteAllowed).isTrue()
    }

    @Test
    fun replaceLocationBarText() {
        model.replaceLocationBarText("random query")
        val firstValue = model.stateFlow.value.textFieldValue
        expectThat(firstValue.text).isEqualTo("random query")
        expectThat(firstValue.selection.collapsed).isTrue()
        expectThat(firstValue.selection.start).isEqualTo("random query".length)
        expectThat(model.stateFlow.value.isAutocompleteAllowed).isFalse()

        model.replaceLocationBarText("query text")
        val secondValue = model.stateFlow.value.textFieldValue
        expectThat(secondValue.text).isEqualTo("query text")
        expectThat(secondValue.selection.collapsed).isTrue()
        expectThat(secondValue.selection.start).isEqualTo("query text".length)
        expectThat(model.stateFlow.value.isAutocompleteAllowed).isFalse()
    }

    @Test
    fun autocompleteSuggestionFlow_detectsMatch() {
        // Make the FaviconCache return a fake bitmap for https://www.example.com
        val mockBitmap = mock<Bitmap>()
        runBlocking {
            Mockito.`when`(
                faviconCache.getFavicon(
                    eq(Uri.parse("https://www.example.com")),
                    eq(false)
                )
            ).thenReturn(mockBitmap)
        }

        model.showZeroQuery()
        model.onLocationBarTextChanged(TextFieldValue("exam"))
        expectThat(model.stateFlow.value.textFieldValue.text).isEqualTo("exam")
        expectThat(model.stateFlow.value.isAutocompleteAllowed).isTrue()

        // Indicate that a new suggestion has come down the pipeline that matches the user query.
        suggestionFlow.value = NavSuggestion(
            url = Uri.parse("https://www.example.com"),
            label = "",
            secondaryLabel = "",
            queryIndex = null,
            type = SuggestionType.AUTOCOMPLETE_SUGGESTION
        )
        coroutineScopeRule.scope.advanceUntilIdle()

        model.stateFlow.value.let {
            expectThat(it.autocompleteSuggestion).isEqualTo("example.com")
            expectThat(it.autocompleteSuggestionText).isEqualTo("ple.com")
            expectThat(it.uriToLoad).isEqualTo(Uri.parse("https://www.example.com"))
            expectThat(it.faviconBitmap).isEqualTo(mockBitmap)
        }
    }

    @Test
    fun autocompleteSuggestionFlow_forSearchButGivenWrongSuggestion_removesAutocomplete() {
        model.showZeroQuery()
        model.onLocationBarTextChanged(TextFieldValue("exam"))
        expectThat(model.stateFlow.value.textFieldValue.text).isEqualTo("exam")
        expectThat(model.stateFlow.value.isAutocompleteAllowed).isTrue()

        // Indicate that a new suggestion has come down the pipeline that matches the user query.
        suggestionFlow.value = NavSuggestion(
            url = Uri.parse("https://www.reddit.com"),
            label = "",
            secondaryLabel = "",
            queryIndex = null,
            type = SuggestionType.AUTOCOMPLETE_SUGGESTION
        )
        coroutineScopeRule.scope.advanceUntilIdle()

        model.stateFlow.value.let {
            expectThat(it.autocompleteSuggestion).isNull()
            expectThat(it.autocompleteSuggestionText).isNull()
            expectThat(it.uriToLoad).isEqualTo("exam".toSearchUri(neevaConstants))
            expectThat(it.faviconBitmap).isEqualTo(model.neevaFavicon)
        }

        runBlocking {
            verify(faviconCache, never()).getFavicon(any(), any())
        }
    }

    @Test
    fun autocompleteSuggestionFlow_forSiteButGivenWrongSuggestion_removesAutocomplete() {
        model.showZeroQuery()
        model.onLocationBarTextChanged(TextFieldValue("example.com"))
        expectThat(model.stateFlow.value.textFieldValue.text).isEqualTo("example.com")
        expectThat(model.stateFlow.value.isAutocompleteAllowed).isTrue()

        // Indicate that a new suggestion has come down the pipeline that matches the user query.
        suggestionFlow.value = NavSuggestion(
            url = Uri.parse("https://www.reddit.com"),
            label = "",
            secondaryLabel = "",
            queryIndex = null,
            type = SuggestionType.AUTOCOMPLETE_SUGGESTION
        )
        coroutineScopeRule.scope.advanceUntilIdle()

        model.stateFlow.value.let {
            expectThat(it.autocompleteSuggestion).isNull()
            expectThat(it.autocompleteSuggestionText).isNull()
            expectThat(it.uriToLoad).isEqualTo(Uri.parse("http://example.com"))
            expectThat(it.faviconBitmap).isEqualTo(null)
        }

        runBlocking {
            verify(faviconCache, never()).getFavicon(any(), any())
        }
    }

    @Test
    fun onFocusChanged() {
        // When the bar is focused, remove whatever text was being displayed.
        model.showZeroQuery()
        expectThat(urlBarModelText).isEqualTo("")
        expectThat(model.stateFlow.value.isEditing).isTrue()

        model.replaceLocationBarText("reddit.com/r/android")
        expectThat(urlBarModelText).isEqualTo("reddit.com/r/android")

        // When the bar is unfocused, it should return to showing the webpage domain.
        model.clearFocus()
        expectThat(model.stateFlow.value.isEditing).isFalse()
        expectThat(urlBarModelText).isEqualTo("")
    }

    @Test
    fun determineUrlBarText_withMatch_showsAutocompletedText() {
        val autocompleteSuggestion = NavSuggestion(
            url = Uri.parse("https://www.reddit.com/r/android"),
            label = "Primary label",
            secondaryLabel = "https://www.reddit.com/r/android",
            type = SuggestionType.AUTOCOMPLETE_SUGGESTION
        )

        val inputState = URLBarModelState(
            isEditing = true,
            isAutocompleteAllowed = true,
            textFieldValue = TextFieldValue("redd")
        )

        val actualValue = runBlocking {
            model.determineDisplayState(autocompleteSuggestion, inputState)
        }
        expectThat(actualValue.userTypedInput).isEqualTo("redd")
        expectThat(actualValue.autocompleteSuggestion).isEqualTo("reddit.com/r/android")
    }

    @Test
    fun determineUrlBarText_withoutMatch_returnsNull() {
        val autocompleteSuggestion = NavSuggestion(
            url = Uri.parse("https://www.reddit.com/r/android"),
            label = "Primary label",
            secondaryLabel = "https://www.reddit.com/r/android",
            type = SuggestionType.AUTOCOMPLETE_SUGGESTION
        )

        val inputState = URLBarModelState(
            isEditing = true,
            isAutocompleteAllowed = true,
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
            URLBarModelImpl.getUrlToLoad(
                "https://www.url.bar.contents.com",
                neevaConstants
            ).toString()
        ).isEqualTo("https://www.url.bar.contents.com")
    }

    @Test
    fun getUrlToLoad_withQuery_returnsUrl() {
        expectThat(
            URLBarModelImpl.getUrlToLoad("query text", neevaConstants)
        ).isEqualTo("query text".toSearchUri(neevaConstants))
    }

    @Test
    fun getAutocompleteText_withNoSubdomain() {
        val autocompleteSuggestion = NavSuggestion(
            url = Uri.parse("https://www.reddit.com/r/android"),
            label = "Primary label",
            secondaryLabel = "https://www.reddit.com/r/android",
            type = SuggestionType.AUTOCOMPLETE_SUGGESTION
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
            secondaryLabel = "https://news.google.com",
            type = SuggestionType.AUTOCOMPLETE_SUGGESTION
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

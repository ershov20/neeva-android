package com.neeva.app.urlbar

import android.content.Context
import android.net.Uri
import android.util.Patterns
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.core.graphics.drawable.toBitmap
import com.neeva.app.Dispatchers
import com.neeva.app.R
import com.neeva.app.browsing.isNeevaSearchUri
import com.neeva.app.browsing.toSearchUri
import com.neeva.app.storage.favicons.FaviconCache
import com.neeva.app.suggestions.NavSuggestion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn

class URLBarModelImpl(
    suggestionFlow: StateFlow<NavSuggestion?>,
    appContext: Context,
    coroutineScope: CoroutineScope,
    dispatchers: Dispatchers,
    private val faviconCache: FaviconCache
) : URLBarModel {
    private val neevaFavicon =
        AppCompatResources.getDrawable(appContext, R.mipmap.ic_neeva_logo)?.toBitmap()

    private val _state = MutableStateFlow(URLBarModelState())
    override val state: StateFlow<URLBarModelState> = _state

    override var focusRequester: FocusRequester? = null

    init {
        // Update what is displayed in the URL bar as the user types.
        suggestionFlow
            .combine(_state) { suggestion, currentState ->
                val newState = determineDisplayState(suggestion, currentState)

                // Only update the data if nothing changed while we were processing the data.
                // This reduces the inherent raciness in doing things while the user is typing out
                // something in the URL bar.
                _state.compareAndSet(currentState, newState)
            }
            .flowOn(dispatchers.io)
            .launchIn(coroutineScope)
    }

    /** Determines what should be displayed in the URL bar as the user types something. */
    internal suspend fun determineDisplayState(
        suggestionValue: NavSuggestion?,
        userInputStateValue: URLBarModelState
    ): URLBarModelState {
        var newState = userInputStateValue.copy()
        val userInput = userInputStateValue.userTypedInput
        val suggestion = suggestionValue.takeIf {
            userInputStateValue.allowAutocomplete || isExactMatch(suggestionValue, userInput)
        }

        // Check for an autocomplete match.
        val autocompletedText = computeAutocompleteText(suggestion, userInput)
            ?: run {
                // If there isn't a match, show the search provider's icon if the URI will perform
                // a search.
                val isSearchUri = userInputStateValue.uriToLoad.isNeevaSearchUri()
                return newState.copy(
                    faviconBitmap = if (isSearchUri) neevaFavicon else null,
                    hasAutocompleteSuggestion = false
                )
            }

        newState = newState.copy(hasAutocompleteSuggestion = suggestion != null)

        // Display the user's text with the autocomplete suggestion tacked on.
        val newTextFieldValue = TextFieldValue(
            text = autocompletedText,
            selection = TextRange(userInput.length, autocompletedText.length)
        )
        if (newTextFieldValue.text != userInputStateValue.textFieldValue.text) {
            // We explicitly check only for text equality because:
            // * The selection can change if the user long presses on the text field
            // * The composition can change whenever IME decides the user is typing out something
            newState = newState.copy(textFieldValue = newTextFieldValue)
        }

        // Load the favicon from the cache, if it's available.
        val uriToLoad = suggestion?.url ?: getUrlToLoad(newTextFieldValue.text)
        if (uriToLoad != userInputStateValue.uriToLoad) {
            val favicon = faviconCache.getFavicon(uriToLoad, false)
            newState = newState.copy(
                uriToLoad = uriToLoad,
                faviconBitmap = favicon
            )
        }

        return newState
    }

    /** Completely replaces what is displayed in the URL bar for user editing. */
    override fun replaceLocationBarText(newValue: String) {
        onRequestFocus()
        onLocationBarTextChanged(
            TextFieldValue(
                text = newValue,
                selection = TextRange(newValue.length)
            )
        )
    }

    /** Updates what is displayed in the URL bar as the user edits it. */
    override fun onLocationBarTextChanged(newValue: TextFieldValue) {
        val currentState = _state.value
        if (currentState.isEditing) {
            val oldText = currentState.userTypedInput
            val newText = newValue.text
            val userTypedSomething =
                newText.startsWith(oldText) && ((oldText.length + 1) == newText.length)

            _state.value = currentState.copy(
                allowAutocomplete = userTypedSomething,
                userTypedInput = newValue.text,
                textFieldValue = newValue,
                uriToLoad = getUrlToLoad(newValue.text)
            )
        }
    }

    override fun onFocusChanged(isFocused: Boolean) {
        // The user has either started editing a query or stopped trying.  Clear out the text.
        _state.value = _state.value.copy(
            isEditing = isFocused,
            allowAutocomplete = true,
            userTypedInput = "",
            textFieldValue = TextFieldValue(),
            uriToLoad = Uri.EMPTY,
            faviconBitmap = null
        )
    }

    companion object {
        /**
         * Determines if what is in the URL bar is related to the [autocompletedSuggestion] via the
         * given [comparator].
         *
         * This applies a bunch of hand wavy heuristics, including chopping off "https://www" and
         * checking if there's a straight match.
         */
        private fun fuzzyMatchSuggestion(
            autocompletedSuggestion: NavSuggestion?,
            urlBarContents: String?,
            comparator: (String, String) -> Boolean
        ): String? {
            if (urlBarContents.isNullOrBlank() || autocompletedSuggestion == null) return null

            // Perform fuzzy matching before checking for a direct match because it's unlikely that
            // a user would type in "https://www." before anything else.
            val suggestionUri = autocompletedSuggestion.url.toString().takeIf { it.isNotBlank() }
                ?: return null
            listOf("https://www.", "https://", "http://www.", "http://").forEach { prefix ->
                suggestionUri.takeIf { it.startsWith(prefix) }
                    ?.removePrefix(prefix)
                    ?.let { if (comparator(it, urlBarContents)) return it }
            }

            // Check if we have a direct match.
            if (comparator(suggestionUri, urlBarContents)) return suggestionUri

            // There's no way we can get a match on the URL.
            return null
        }

        internal fun computeAutocompleteText(
            autocompletedSuggestion: NavSuggestion?,
            urlBarContents: String?,
        ): String? {
            return fuzzyMatchSuggestion(autocompletedSuggestion, urlBarContents) { first, second ->
                first.startsWith(second)
            }
        }

        private fun isExactMatch(
            autocompletedSuggestion: NavSuggestion?,
            urlBarContents: String?
        ): Boolean {
            return fuzzyMatchSuggestion(autocompletedSuggestion, urlBarContents) { first, second ->
                first == second
            } != null
        }

        /** Returns which URL should be loaded when the user submits their text. */
        internal fun getUrlToLoad(urlBarContents: String): Uri {
            return when {
                // Try to figure out if the user typed in a query or a URL.
                Patterns.WEB_URL.matcher(urlBarContents).matches() -> {
                    Uri.parse(
                        when {
                            !urlBarContents.startsWith("http") -> "http://$urlBarContents"
                            else -> urlBarContents
                        }
                    )
                }

                else -> {
                    urlBarContents.toSearchUri()
                }
            }
        }
    }
}

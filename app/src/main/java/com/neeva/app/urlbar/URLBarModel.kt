package com.neeva.app.urlbar

import android.net.Uri
import android.util.Patterns
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.neeva.app.browsing.ActiveTabModel
import com.neeva.app.browsing.toSearchUri
import com.neeva.app.suggestions.NavSuggestion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

data class URLBarModelState(
    val isEditing: Boolean = false,
    val allowAutocomplete: Boolean = false,
    val userTypedInput: String = "",
) {
    /** Alerts observers to what the user is currently typing. */
    val queryText: String?
        get() = userTypedInput.takeIf { isEditing }
}

/**
 * Maintains logic required to provide a URL bar to the user.  There is a single URL bar that is
 * used across all tabs.
 *
 * When the user asks to create a new tab from the CardGrid UI, we don't actually create the tab
 * until the user has chosen to enter a query/URL or select an item from the suggested list.  This
 * "lazy tab" state means that we have to be careful to avoid mutating the state of the currently
 * active tab.
 *
 * TODO(dan.alcantara): Audit how lazy tab opening works.
 */
class URLBarModel(
    val isIncognito: Boolean,
    private val activeTabModel: ActiveTabModel,
    suggestionFlow: Flow<NavSuggestion?>,
    coroutineScope: CoroutineScope
) {
    private val _textFieldValue = MutableStateFlow(TextFieldValue())
    val textFieldValue: StateFlow<TextFieldValue> = _textFieldValue

    private var _isLazyTab = MutableStateFlow(false)
    val isLazyTab: StateFlow<Boolean> = _isLazyTab

    private val _userInputState = MutableStateFlow(URLBarModelState())
    val userInputState: StateFlow<URLBarModelState> = _userInputState
    val userInputTextIsBlank: Flow<Boolean> = _userInputState.map { it.queryText.isNullOrBlank() }
    val isEditing: Flow<Boolean> = _userInputState.map { it.isEditing }

    var focusRequester: FocusRequester? = null

    init {
        // Update what is displayed in the URL bar as the user types.
        suggestionFlow
            .combine(userInputState) { suggestion, state -> determineUrlBarText(suggestion, state) }
            .onEach { _textFieldValue.value = it }
            .launchIn(coroutineScope)
    }

    /** Determines what should be displayed in the URL bar as the user types something. */
    internal fun determineUrlBarText(
        suggestionValue: NavSuggestion?,
        userInputStateValue: URLBarModelState
    ): TextFieldValue {
        val suggestion = suggestionValue.takeIf { userInputStateValue.allowAutocomplete }
        val userInput = userInputStateValue.userTypedInput

        return getAutocompleteText(suggestion, userInput)?.let { autocompletedText ->
            // Display the user's text with the autocomplete suggestion tacked on.
            TextFieldValue(
                text = autocompletedText,
                selection = TextRange(userInput.length, autocompletedText.length)
            )
        } ?: run {
            // Display exactly what the user has typed in.
            TextFieldValue(
                text = userInput,
                selection = TextRange(userInput.length)
            )
        }
    }

    /**
     * Prepare to open a new tab.  This mechanism doesn't create a new tab until the user actually
     * navigates somewhere or performs a query.
     */
    fun openLazyTab() {
        _isLazyTab.value = true
        onRequestFocus()
    }

    fun loadUrl(url: Uri) = activeTabModel.loadUrl(url, _isLazyTab.value)
    fun reload() = activeTabModel.reload()

    /** Completely replaces what is displayed in the URL bar for user editing. */
    fun replaceLocationBarText(newValue: String) {
        onRequestFocus()
        onLocationBarTextChanged(newValue)
    }

    /** Updates what is displayed in the URL bar as the user edits it. */
    fun onLocationBarTextChanged(newValue: String) {
        val currentState = userInputState.value
        if (currentState.isEditing) {
            val oldValue = currentState.userTypedInput
            val userTypedSomething =
                newValue.startsWith(oldValue) && ((oldValue.length + 1) == newValue.length)

            _userInputState.value = currentState.copy(
                allowAutocomplete = userTypedSomething,
                userTypedInput = newValue
            )
        }
    }

    fun onFocusChanged(isFocused: Boolean) {
        // The user has either started editing a query or stopped trying.  Clear out the text.
        _userInputState.value = userInputState.value.copy(
            isEditing = isFocused,
            allowAutocomplete = true,
            userTypedInput = ""
        )

        // Clear out the URL bar contents whenever the user stops typing.
        if (!isFocused) {
            _textFieldValue.value = TextFieldValue()
        }

        _isLazyTab.value = _isLazyTab.value && userInputState.value.isEditing
    }

    fun onRequestFocus() {
        focusRequester?.requestFocus()
    }

    fun setTextFieldValue(textFieldValue: TextFieldValue) {
        _textFieldValue.value = textFieldValue
    }

    companion object {
        /**
         * Determines if what is in the URL bar can in any way be related to the autocomplete suggestion.
         *
         * This applies a bunch of hand wavy heuristics, including chopping off "https://www" and
         * checking if there's a straight match.
         */
        internal fun getAutocompleteText(
            autocompletedSuggestion: NavSuggestion?,
            urlBarContents: String?
        ): String? {
            if (urlBarContents.isNullOrBlank()) return null

            // Check if we have a direct match.
            val autocompleteText =
                autocompletedSuggestion?.secondaryLabel?.takeIf { it.isNotBlank() } ?: return null
            if (autocompleteText.startsWith(urlBarContents)) {
                return autocompleteText
            }

            // Check if the URL could possibly start with what has already been typed in.
            val autocompleteUri = autocompletedSuggestion.url.toString()

            val withoutScheme = autocompleteUri
                .takeIf { it.startsWith("https://") }
                ?.replaceFirst("https://", "")
                ?.takeIf { it.startsWith(urlBarContents) }
            if (withoutScheme != null) return withoutScheme

            val withoutWww = autocompleteUri
                .takeIf { it.startsWith("https://www.") }
                ?.replaceFirst("https://www.", "")
                ?.takeIf { it.startsWith(urlBarContents) }
            if (withoutWww != null) return withoutWww

            return null
        }

        /**
         * Returns which URL should be loaded when the user submits their text.
         *
         * This always prioritizes any provided autocompleted suggestion, so callers should ensure that what
         * is provided is a valid suggestion for the current query.
         */
        internal fun getUrlToLoad(urlBarContents: String): Uri {
            return when {
                // Try to figure out if the user typed in a query or a URL.
                // TODO(dan.alcantara): This won't always work, especially if the site doesn't have
                //                      an https equivalent.  We should either figure out something
                //                      more robust or do what iOS does (for consistency).
                Patterns.WEB_URL.matcher(urlBarContents).matches() -> {
                    Uri.parse(
                        when {
                            !urlBarContents.startsWith("http") -> "https://$urlBarContents"
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

package com.neeva.app.urlbar

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.text.input.TextFieldValue
import com.neeva.app.browsing.isNeevaSearchUri
import com.neeva.app.suggestions.SuggestionType

data class URLBarModelState(
    /** Whether or not the user is editing the URL bar and should be shown suggestions. */
    val isEditing: Boolean = false,

    /** Whether or not autocomplete suggestions should be allowed. */
    val allowAutocomplete: Boolean = false,

    /** Backing field for the [AutocompleteTextField]. */
    val textFieldValue: TextFieldValue = TextFieldValue(),

    /** URI that should be loaded, given what the user typed in and autocomplete suggests. */
    val uriToLoad: Uri = Uri.EMPTY,

    /** Favicon associated with the [uriToLoad]. */
    val faviconBitmap: Bitmap? = null,

    /** The full string of the autocomplete suggestion, if one is available. */
    val autocompleteSuggestion: String? = null

) {
    /** Exactly what the user has typed in, without any autocompleted text tacked on. */
    val userTypedInput: String
        get() = textFieldValue.text

    /** Alerts observers to what the user is currently typing. */
    val queryText: String?
        get() = userTypedInput.takeIf { isEditing }

    /** String that should be appended to the user's text to match the [autocompleteSuggestion]. */
    val autocompleteSuggestionText: String?
        get() = autocompleteSuggestion
            ?.takeIf { allowAutocomplete }
            ?.drop(userTypedInput.length)

    fun getSuggestionType(): SuggestionType {
        return when {
            autocompleteSuggestion != null -> SuggestionType.AUTOCOMPLETE_SUGGESTION
            uriToLoad.isNeevaSearchUri() -> SuggestionType.NO_SUGGESTION_QUERY
            else -> SuggestionType.NO_SUGGESTION_URL
        }
    }

    /** Creates a new [URLBarModelState] accounting for a change in the text. */
    fun withUpdatedTextFieldValue(
        newTextFieldValue: TextFieldValue,
        isAutocompleteAllowed: Boolean
    ): URLBarModelState {
        var newState = copy(
            allowAutocomplete = isAutocompleteAllowed,
            textFieldValue = newTextFieldValue
        )

        val isAutocompleteStillValid =
            autocompleteSuggestion?.startsWith(newTextFieldValue.text) == true
        if (!isAutocompleteAllowed || !isAutocompleteStillValid) {
            // Toss out the existing autocomplete suggestion.
            newState = newState.copy(
                autocompleteSuggestion = null,
                uriToLoad = URLBarModelImpl.getUrlToLoad(newState.textFieldValue.text)
            )
        }

        return newState
    }
}

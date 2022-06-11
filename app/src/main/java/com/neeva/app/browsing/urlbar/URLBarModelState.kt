package com.neeva.app.browsing.urlbar

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.text.input.TextFieldValue
import com.neeva.app.NeevaConstants
import com.neeva.app.browsing.isNeevaSearchUri
import com.neeva.app.suggestions.SuggestionType

data class URLBarModelState(
    /** Whether or not the user is editing the URL bar and should be shown suggestions. */
    val isEditing: Boolean = false,

    /** Zero Query with no focus */
    val focusUrlBar: Boolean = true,

    /** Whether or not autocomplete suggestions should be allowed. */
    internal val isAutocompleteAllowed: Boolean = true,

    /** Backing field for the [AutocompleteTextField]. */
    val textFieldValue: TextFieldValue = TextFieldValue(),

    /** URI that should be loaded, given what the user typed in and autocomplete suggests. */
    val uriToLoad: Uri = Uri.EMPTY,

    /** Favicon associated with the [uriToLoad]. */
    val faviconBitmap: Bitmap? = null,

    /** The full string of the autocomplete suggestion, if one is available. */
    internal val autocompleteSuggestion: String? = null

) {
    /** Exactly what the user has typed in, without any autocompleted text tacked on. */
    val userTypedInput: String
        get() = textFieldValue.text

    /** Alerts observers to what the user is currently typing. */
    internal val queryText: String?
        get() = userTypedInput.takeIf { isEditing }

    /** String that should be appended to the user's text to match the [autocompleteSuggestion]. */
    val autocompleteSuggestionText: String?
        get() = autocompleteSuggestion
            ?.takeIf { isAutocompleteAllowed }
            ?.drop(userTypedInput.length)

    fun getSuggestionType(neevaConstants: NeevaConstants): SuggestionType {
        return when {
            autocompleteSuggestion != null -> SuggestionType.AUTOCOMPLETE_SUGGESTION
            uriToLoad.isNeevaSearchUri(neevaConstants) -> SuggestionType.NO_SUGGESTION_QUERY
            else -> SuggestionType.NO_SUGGESTION_URL
        }
    }
}

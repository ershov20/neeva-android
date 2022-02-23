package com.neeva.app.urlbar

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.text.input.TextFieldValue

data class URLBarModelState(
    /** Whether or not the user is editing the URL bar and should be shown suggestions. */
    val isEditing: Boolean = false,

    /** Whether or not autocomplete suggestions should be allowed. */
    val allowAutocomplete: Boolean = false,

    /** Exactly what the user has typed in, without any autocompleted text tacked on. */
    val userTypedInput: String = "",

    /** Backing field for the [AutocompleteTextField]. */
    val textFieldValue: TextFieldValue = TextFieldValue(),

    /** URI that should be loaded, given what the user typed in and autocomplete suggests. */
    val uriToLoad: Uri = Uri.EMPTY,

    /** Favicon associated with the [uriToLoad]. */
    val faviconBitmap: Bitmap? = null
) {
    /** Alerts observers to what the user is currently typing. */
    val queryText: String?
        get() = userTypedInput.takeIf { isEditing }
}

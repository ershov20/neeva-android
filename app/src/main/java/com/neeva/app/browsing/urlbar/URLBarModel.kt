package com.neeva.app.browsing.urlbar

import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.chromium.weblayer.UrlBarController

/**
 * Maintains logic required to provide a URL bar to the user.  There is a single URL bar that is
 * used across all tabs for each [com.neeva.app.browsing.BrowserWrapper]
 *
 * When the user asks to create a new tab from the CardGrid UI, we don't actually create the tab
 * until the user has chosen to enter a query/URL or select an item from the suggested list.  This
 * "lazy tab" state means that we have to be careful to avoid mutating the state of the currently
 * active tab.
 */
interface URLBarModel {
    /**
     * Flow of _all_ things that can change in the URL bar.  If you want to watch just one thing,
     * use one of the other Flows (or add a new one tailored to your use case).
     */
    val stateFlow: StateFlow<URLBarModelState>

    /** Tracks the UrlBarController used to display information about the current page. */
    val urlBarControllerFlow: Flow<UrlBarController?>

    val queryTextFlow: Flow<String?>
        get() = stateFlow.map { it.queryText }.distinctUntilChanged()

    val isUserQueryBlank: Flow<Boolean>
        get() = stateFlow.map { it.queryText.isNullOrBlank() }.distinctUntilChanged()

    val isEditing: Flow<Boolean>
        get() = stateFlow.map { it.isEditing }.distinctUntilChanged()

    /** Replaces all of the text that is in the URL bar. */
    fun replaceLocationBarText(newValue: String)

    /** Called when the contents of the URL bar have been edited. */
    fun onLocationBarTextChanged(newValue: TextFieldValue)

    /** Accepts the autocomplete suggestion that is currently being displayed. */
    fun acceptAutocompleteSuggestion()

    /** Requests focus on the URL bar for editing. */
    fun requestFocus()

    /** Clears focus on the URL bar. */
    fun clearFocus()
}

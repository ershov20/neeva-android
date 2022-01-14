package com.neeva.app.urlbar

import android.net.Uri
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.neeva.app.browsing.ActiveTabModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

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
    private val activeTabModel: ActiveTabModel
) {
    private val _userInputText = MutableStateFlow(TextFieldValue(""))
    val userInputText: StateFlow<TextFieldValue> = _userInputText
    val userInputTextIsBlank: Flow<Boolean> = _userInputText.map { it.text.isBlank() }

    private val _isEditing = MutableStateFlow(false)
    val isEditing: StateFlow<Boolean> = _isEditing

    private var _isLazyTab = MutableStateFlow(false)
    val isLazyTab: StateFlow<Boolean> = _isLazyTab

    var focusRequester: FocusRequester? = null

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
        onLocationBarTextChanged(TextFieldValue(newValue, TextRange(newValue.length)))
    }

    /** Updates what is displayed in the URL bar as the user edits it. */
    fun onLocationBarTextChanged(newValue: TextFieldValue) {
        if (_isEditing.value) {
            _userInputText.value = newValue
        }
    }

    fun onFocusChanged(isFocused: Boolean) {
        _isEditing.value = isFocused
        _isLazyTab.value = _isLazyTab.value && _isEditing.value

        // The user has either started editing a query or stopped trying.  Clear out the text.
        _userInputText.value = TextFieldValue("", TextRange(0))
    }

    fun onRequestFocus() {
        focusRequester?.requestFocus()
    }
}

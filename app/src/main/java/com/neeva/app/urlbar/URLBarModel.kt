package com.neeva.app.urlbar

import android.net.Uri
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.neeva.app.browsing.ActiveTabModel
import com.neeva.app.browsing.baseDomain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

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
    coroutineScope: CoroutineScope,
    private val activeTabModel: ActiveTabModel,
    private val onTextChanged: (String) -> Unit
) {
    /**
     * Tracks the text displayed in the URL bar.  We need to use TextFieldValue to correctly keep
     * track of where the cursor is supposed to be when editing.
     */
    private val _text = MutableStateFlow(TextFieldValue(""))
    val textFieldValue: StateFlow<TextFieldValue> = _text

    private val _isEditing = MutableStateFlow(false)
    val isEditing: StateFlow<Boolean> = _isEditing

    private val _showLock = MutableStateFlow(false)
    val showLock: StateFlow<Boolean> = _showLock

    private var _isLazyTab = MutableStateFlow(false)
    val isLazyTab: StateFlow<Boolean> = _isLazyTab

    var focusRequester: FocusRequester? = null

    init {
        coroutineScope.launch {
            activeTabModel.urlFlow.collect {
                if (it.toString().isNotBlank()) {
                    onCurrentUrlChanged(it.toString())
                }
            }
        }
    }

    /**
     * Prepare to open a new tab.  This mechanism doesn't create a new tab until the user actually
     * navigates somewhere or performs a query.
     */
    fun openLazyTab() {
        onCurrentUrlChanged("")
        onRequestFocus()
        _isLazyTab.value = true
    }

    fun loadUrl(url: Uri) = activeTabModel.loadUrl(url, _isLazyTab.value)
    fun reload() = activeTabModel.reload()

    /** Completely replaces what is displayed in the URL bar for user editing. */
    fun replaceLocationBarText(newValue: String) {
        onRequestFocus()
        onLocationBarTextChanged(
            textFieldValue.value.copy(
                newValue,
                TextRange(newValue.length, newValue.length)
            )
        )
    }

    /** Updates what is displayed in the URL bar as the user edits it. */
    fun onLocationBarTextChanged(newValue: TextFieldValue) {
        if (_isEditing.value) {
            updateTextValue(newValue)
        }
    }

    private fun onCurrentUrlChanged(newUrl: String) {
        val uri = Uri.parse(newUrl)
        updateTextValue(_text.value.copy(uri.baseDomain() ?: ""))
        _showLock.value = uri.scheme.equals("https")
    }

    fun onFocusChanged(isFocused: Boolean) {
        _isEditing.value = isFocused
        _isLazyTab.value = _isLazyTab.value && _isEditing.value
        if (!isFocused) {
            // The user has unfocused the bar.  Show the domain for the currently loaded webpage.
            updateTextValue(_text.value.copy(activeTabModel.urlFlow.value.baseDomain() ?: ""))
        } else {
            // The user has started editing the contents of the bar.  Clear it out.
            updateTextValue(_text.value.copy(""))
        }
    }

    private fun updateTextValue(newValue: TextFieldValue) {
        _text.value = newValue
        onTextChanged(newValue.text)
    }

    fun onRequestFocus() {
        focusRequester?.requestFocus()
    }
}

package com.neeva.app.urlbar

import android.net.Uri
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.neeva.app.browsing.SelectedTabModel
import com.neeva.app.browsing.baseDomain
import com.neeva.app.history.DomainViewModel
import com.neeva.app.history.HistoryViewModel
import com.neeva.app.storage.SpaceStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class URLBarModel(
    private val selectedTabModel: SelectedTabModel,
    private val domainViewModel: DomainViewModel,
    private val historyViewModel: HistoryViewModel
): ViewModel() {
    private val _text = MutableStateFlow(TextFieldValue("", TextRange.Zero))
    val text: StateFlow<TextFieldValue> = _text

    private val _isEditing = MutableStateFlow(false)
    val isEditing: StateFlow<Boolean> = _isEditing

    private val _showLock = MutableStateFlow(false)
    val showLock: StateFlow<Boolean> = _showLock

    private var _isLazyTab = MutableStateFlow(false)
    val isLazyTab: StateFlow<Boolean> = _isLazyTab

    // TODO(dan.alcantara): This shouldn't be here, but is because of the way the URL bar comprises
    //                      two different Composables and because of how the URL bar can be focused
    //                      from around the app.  We should rethink this.
    val focusRequester = FocusRequester()

    init {
        viewModelScope.launch {
            _isEditing.collect {
                _isLazyTab.value = _isLazyTab.value && it
            }
        }

        viewModelScope.launch {
            selectedTabModel.urlFlow.collect {
                if (it.toString().isNotBlank()) {
                    onCurrentUrlChanged(it.toString())
                }
            }
        }
    }

    /**
     * Prepare to open a new tab.  This mechanism doesn't create a new tab until the user actually
     * navigates somewhere or performs a query.
     *
     * TODO(dan.alcantara): Rethink how lazy tab opening works.
     */
    fun openLazyTab() {
        onCurrentUrlChanged("")
        onRequestFocus()
        _isLazyTab.value = true
    }

    fun onReload() = selectedTabModel.reload()

    fun onLocationBarTextChanged(newValue: TextFieldValue) {
        if (_isEditing.value) {
            updateTextValue(newValue)
        }
    }

    fun loadUrl(url: Uri) {
        selectedTabModel.loadUrl(url, _isLazyTab.value)
    }

    private fun onCurrentUrlChanged(newUrl: String) {
        updateTextValue(_text.value.copy(Uri.parse(newUrl)?.baseDomain() ?: ""))
        _showLock.value = Uri.parse(newUrl).scheme.equals("https")
    }

    fun onFocusChanged(focus: FocusState) {
        _isEditing.value = focus.isFocused
        if (!focus.isFocused) {
            updateTextValue(_text.value.copy(selectedTabModel.urlFlow.value.baseDomain() ?: ""))
        } else {
            updateTextValue(_text.value.copy(""))
            viewModelScope.launch {
                SpaceStore.shared.refresh()
            }
        }
    }

    private fun updateTextValue(newValue: TextFieldValue) {
        _text.value = newValue

        // Pull new suggestions from the database according to what's currently in the URL bar.
        domainViewModel.updateSuggestionQuery(newValue.text)
        historyViewModel.updateSuggestionQuery(newValue.text)
    }

    fun onRequestFocus() {
        focusRequester.requestFocus()
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        class URLBarModelFactory(
            private val selectedTabModel: SelectedTabModel,
            private val domainViewModel: DomainViewModel,
            private val historyViewModel: HistoryViewModel
        ) :
            ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return URLBarModel(selectedTabModel, domainViewModel, historyViewModel) as T
            }
        }
    }
}
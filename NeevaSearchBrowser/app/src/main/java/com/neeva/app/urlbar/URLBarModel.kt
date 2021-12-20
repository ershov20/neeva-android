package com.neeva.app.urlbar

import android.net.Uri
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.*
import com.neeva.app.browsing.SelectedTabModel
import com.neeva.app.browsing.baseDomain
import com.neeva.app.storage.SpaceStore
import com.neeva.app.suggestions.NavSuggestion
import kotlinx.coroutines.launch

class URLBarModel(private val selectedTabModel: SelectedTabModel): ViewModel() {
    private val _text = MutableLiveData(TextFieldValue("", TextRange.Zero))
    val text: LiveData<TextFieldValue> = _text

    private val _isEditing = MutableLiveData(false)
    val isEditing: LiveData<Boolean> = _isEditing

    private val _showLock = MutableLiveData(false)
    val showLock: LiveData<Boolean> = _showLock

    lateinit var autocompletedSuggestion: LiveData<NavSuggestion?>

    private var _isLazyTab = MutableLiveData(false)
    val isLazyTab: LiveData<Boolean> = _isLazyTab

    val focusRequester = FocusRequester()

    init {
        // TODO(dan.alcantara): Rethink how lazy tab opening works.
        _isEditing.observeForever {
            _isLazyTab.value = _isLazyTab.value == true && it
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

    fun onReload() = selectedTabModel.reload()

    fun onLocationBarTextChanged(newValue: TextFieldValue) {
        if (_isEditing.value == true) _text.value = newValue
    }

    fun loadUrl(url: Uri) {
        selectedTabModel.loadUrl(url, _isLazyTab.value == true)
    }

    fun onCurrentUrlChanged(newUrl: String) {
        _text.value = _text.value?.copy(Uri.parse(newUrl)?.baseDomain() ?: "")
        _showLock.value = Uri.parse(newUrl).scheme.equals("https")
    }

    fun onFocusChanged(focus: FocusState) {
        _isEditing.value = focus.isFocused
        if (!focus.isFocused) {
            _text.value = _text.value?.copy(selectedTabModel.currentUrl.value?.baseDomain() ?: "")
        } else {
            _text.value = _text.value?.copy("")
            viewModelScope.launch {
                SpaceStore.shared.refresh()
            }
        }
    }

    fun onRequestFocus() {
        focusRequester.requestFocus()
    }
}

@Suppress("UNCHECKED_CAST")
class UrlBarModelFactory(private val selectedTabModel: SelectedTabModel) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return URLBarModel(selectedTabModel) as T
    }
}
package com.neeva.app.urlbar

import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.neeva.app.web.WebViewModel
import com.neeva.app.web.baseDomain

class URLBarModel(private val webViewModel: WebViewModel): ViewModel() {
    private val _text = MutableLiveData(TextFieldValue("", TextRange.Zero))
    val text: LiveData<TextFieldValue> = _text

    private val _isEditing = MutableLiveData(false)
    val isEditing: LiveData<Boolean> = _isEditing

    private val _showLock = MutableLiveData(false)
    val showLock: LiveData<Boolean> = _showLock

    val focusRequester = FocusRequester()

    fun onLocationBarTextChanged(newValue: TextFieldValue) {
        if (isEditing.value == true) _text.value = newValue
    }

    fun onCurrentUrlChanged(newUrl: String) {
        _text.value = _text.value?.copy(Uri.parse(newUrl)?.baseDomain() ?: "")
        _showLock.value = Uri.parse(newUrl).scheme.equals("https")
    }

    fun onFocusChanged(focus: FocusState) {
        _isEditing.value = focus.isFocused
        if (!focus.isFocused) {
            _text.value = _text.value?.copy(Uri.parse(webViewModel.currentUrl.value)?.baseDomain() ?: "")
        } else {
            _text.value = _text.value?.copy("")
        }
    }

    fun onRequestFocus() {
        focusRequester.requestFocus()
    }
}

@Suppress("UNCHECKED_CAST")
class UrlBarModelFactory(webModel: WebViewModel) :
    ViewModelProvider.Factory {
    private val webModel: WebViewModel = webModel
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return URLBarModel(webModel) as T
    }
}
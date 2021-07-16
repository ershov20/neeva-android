package com.neeva.app

import android.net.Uri
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class URLBarModel(private val webViewModel: WebViewModel): ViewModel() {
    private val _text = MutableLiveData("")
    val text: LiveData<String> = _text

    private val _isEditing = MutableLiveData(false)
    val isEditing: LiveData<Boolean> = _isEditing

    private val _showLock = MutableLiveData(false)
    val showLock: LiveData<Boolean> = _showLock

    val focusRequester = FocusRequester()

    fun onLocationBarTextChanged(newText: String) {
        if (isEditing.value == true) _text.value = newText
    }

    fun onCurrentUrlChanged(newUrl: String) {
        _text.value = Uri.parse(newUrl).authority ?: ""
        _showLock.value = Uri.parse(newUrl).scheme.equals("https")
    }

    fun onFocusChanged(focus: FocusState) {
        _isEditing.value = focus.isFocused
        if (!focus.isFocused) {
            _text.value = Uri.parse(webViewModel.currentUrl.value).authority ?: ""
        } else {
            _text.value = ""
        }
    }

    fun onRequestFocus() {
        focusRequester.requestFocus()
    }
}

class UrlBarModelFactory(webModel: WebViewModel) :
    ViewModelProvider.Factory {
    private val webModel: WebViewModel = webModel
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return URLBarModel(webModel) as T
    }
}
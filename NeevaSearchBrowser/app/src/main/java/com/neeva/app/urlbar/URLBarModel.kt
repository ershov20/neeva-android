package com.neeva.app.urlbar

import android.net.Uri
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.*
import com.neeva.app.suggestions.NavSuggestion
import com.neeva.app.web.WebLayerModel
import com.neeva.app.web.baseDomain
import kotlinx.coroutines.flow.*

class URLBarModel(private val webLayerModel: WebLayerModel): ViewModel() {
    private val _text = MutableLiveData(TextFieldValue("", TextRange.Zero))
    val text: LiveData<TextFieldValue> = _text

    private val _isEditing = MutableLiveData(false)
    val isEditing: LiveData<Boolean> = _isEditing

    private val _showLock = MutableLiveData(false)
    val showLock: LiveData<Boolean> = _showLock

    lateinit var autocompletedSuggestion: LiveData<NavSuggestion?>

    val focusRequester = FocusRequester()

    val onGo = webLayerModel::loadUrl
    val onReload = webLayerModel::reload

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
            _text.value = _text.value?.copy(webLayerModel.currentUrl.value?.baseDomain() ?: "")
        } else {
            _text.value = _text.value?.copy("")
        }
    }

    fun onRequestFocus() {
        focusRequester.requestFocus()
    }
}

@Suppress("UNCHECKED_CAST")
class UrlBarModelFactory(webModel: WebLayerModel) :
    ViewModelProvider.Factory {
    private val webModel: WebLayerModel = webModel
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return URLBarModel(webModel) as T
    }
}
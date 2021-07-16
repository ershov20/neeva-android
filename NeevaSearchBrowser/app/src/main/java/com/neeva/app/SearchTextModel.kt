package com.neeva.app

import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel


class SearchTextModel: ViewModel() {
    private val _text = MutableLiveData("")
    val text: LiveData<String> = _text

    private val _isEditing = MutableLiveData(false)
    val isEditing: LiveData<Boolean> = _isEditing

    val focusRequester = FocusRequester()

    fun onSearchTextChanged(newText: String) {
        _text.value = newText
    }

    fun onFocusChanged(focus: FocusState) {
        _isEditing.value = focus.isFocused
    }

    fun onRequestFocus() {
        focusRequester.requestFocus()
    }
}
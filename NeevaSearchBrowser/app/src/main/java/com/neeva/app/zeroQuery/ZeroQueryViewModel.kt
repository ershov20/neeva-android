package com.neeva.app.zeroQuery

import androidx.lifecycle.*
import com.neeva.app.urlbar.URLBarModel

class ZeroQueryViewModel(private val urlBarModel: URLBarModel): ViewModel() {
    private val _isLazyTab = MutableLiveData(false)
    val isLazyTab: LiveData<Boolean> = _isLazyTab

    init {
        urlBarModel.isEditing.observeForever {
            _isLazyTab.value = _isLazyTab.value == true && it
        }
    }

    fun openLazyTab() {
        urlBarModel.onCurrentUrlChanged("")
        urlBarModel.onRequestFocus()
        _isLazyTab.value = true
    }
}

class ZeroQueryModelFactory(private val urlBarModel: URLBarModel) :
    ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ZeroQueryViewModel(urlBarModel) as T
    }
}
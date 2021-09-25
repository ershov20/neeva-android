package com.neeva.app.card

import androidx.compose.foundation.lazy.LazyListState
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.neeva.app.NeevaActivity
import com.neeva.app.browsing.BrowserPrimitive
import com.neeva.app.browsing.WebLayerModel
import com.neeva.app.history.HistoryViewModel
import com.neeva.app.storage.DomainViewModel

class CardViewModel(private val tabsList: LiveData<List<BrowserPrimitive>>): ViewModel() {
    private val _listState = MutableLiveData<LazyListState>()
    val listState: LiveData<LazyListState> = _listState

    init {
        tabsList.observeForever { tabList ->
            var index = tabList.indexOfFirst { it.isSelected }
            if (index == -1) {
                index = 0
            }
            _listState.value = LazyListState(index)
        }
    }
}

@Suppress("UNCHECKED_CAST")
class CardViewModelFactory(private val tabsList: LiveData<List<BrowserPrimitive>>) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return CardViewModel(tabsList) as T
    }
}
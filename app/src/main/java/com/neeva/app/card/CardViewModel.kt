package com.neeva.app.card

import androidx.compose.foundation.lazy.LazyListState
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.neeva.app.browsing.TabInfo

class CardViewModel(tabsList: LiveData<List<TabInfo>>): ViewModel() {
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

    companion object {
        class CardViewModelFactory(private val tabsList: LiveData<List<TabInfo>>) : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return CardViewModel(tabsList) as T
            }
        }
    }
}
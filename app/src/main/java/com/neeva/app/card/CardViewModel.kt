package com.neeva.app.card

import androidx.compose.foundation.lazy.LazyListState
import androidx.lifecycle.*
import com.neeva.app.browsing.TabInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class CardViewModel(tabsList: StateFlow<List<TabInfo>>): ViewModel() {
    private val _listState = MutableStateFlow(LazyListState())
    val listState: StateFlow<LazyListState> = _listState

    init {
        viewModelScope.launch {
            tabsList.collect { tabList ->
                var index = tabList.indexOfFirst { it.isSelected }
                if (index == -1) {
                    index = 0
                }
                _listState.value = LazyListState(index)
            }
        }
    }

    companion object {
        class CardViewModelFactory(private val tabsList: StateFlow<List<TabInfo>>) : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return CardViewModel(tabsList) as T
            }
        }
    }
}
package com.neeva.app

import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neeva.app.card.CardsContainer
import com.neeva.app.history.HistoryContainer
import com.neeva.app.history.HistoryUI
import com.neeva.app.history.HistoryViewModel
import com.neeva.app.neeva_menu.NeevaMenuSheet
import com.neeva.app.settings.SettingsContainer
import com.neeva.app.settings.SettingsMain
import com.neeva.app.spaces.AddToSpaceSheet
import com.neeva.app.storage.DomainViewModel
import com.neeva.app.storage.SpaceStore
import com.neeva.app.web.SelectedTabModel
import com.neeva.app.web.WebLayerModel
import com.neeva.app.zeroQuery.ZeroQueryViewModel
import kotlinx.coroutines.launch

class AppNavModel: ViewModel() {
    private val _state = MutableLiveData(AppNavState.HIDDEN)
    val state: LiveData<AppNavState> = _state

    lateinit var onOpenUrl: (Uri) -> Unit

    fun setContentState(state: AppNavState) {
        _state.value = state

        if(state == AppNavState.ADD_TO_SPACE) {
            viewModelScope.launch {
                SpaceStore.shared.refresh()
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppNav(
    model: AppNavModel,
    selectedTabModel: SelectedTabModel,
    historyViewModel: HistoryViewModel,
    domainViewModel: DomainViewModel,
    webLayerModel: WebLayerModel,
    zeroQueryViewModel: ZeroQueryViewModel
) {
    Box {
        AddToSpaceSheet(appNavModel = model, selectedTabModel = selectedTabModel)
        NeevaMenuSheet(appNavModel = model)
        SettingsContainer(appNavModel = model)
        HistoryContainer(
            appNavModel = model,
            historyViewModel = historyViewModel,
            domainViewModel = domainViewModel)
        CardsContainer(
            appNavModel = model,
            webLayerModel = webLayerModel,
            domainViewModel = domainViewModel,
            zeroQueryViewModel = zeroQueryViewModel
        )
    }
}

enum class AppNavState {
    HIDDEN,
    SETTINGS,
    ADD_TO_SPACE,
    NEEVA_MENU,
    HISTORY,
    CARD_GRID
}

package com.neeva.app

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.neeva.app.browsing.SelectedTabModel
import com.neeva.app.browsing.WebLayerModel
import com.neeva.app.card.CardViewModel
import com.neeva.app.card.CardsContainer
import com.neeva.app.history.HistoryContainer
import com.neeva.app.history.HistoryViewModel
import com.neeva.app.neeva_menu.NeevaMenuItemId
import com.neeva.app.neeva_menu.NeevaMenuSheet
import com.neeva.app.settings.SettingsContainer
import com.neeva.app.spaces.AddToSpaceSheet
import com.neeva.app.history.DomainViewModel
import com.neeva.app.storage.SpaceStore
import com.neeva.app.urlbar.URLBarModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AppNavModel(
    private val onOpenUrl: (Uri) -> Unit
): ViewModel() {
    companion object {
        @Suppress("UNCHECKED_CAST")
        class AppNavModelFactory(private val onOpenUrl: (Uri) -> Unit) : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return AppNavModel(onOpenUrl) as T
            }
        }
    }

    private val _state = MutableStateFlow(AppNavState.BROWSER)
    val state: StateFlow<AppNavState> = _state

    fun openUrl(uri: Uri) {
        onOpenUrl(uri)
        showBrowser()
    }

    private fun setContentState(state: AppNavState) {
        _state.value = state

        if (state == AppNavState.ADD_TO_SPACE) {
            viewModelScope.launch {
                SpaceStore.shared.refresh()
            }
        }
    }

    fun showBrowser() = setContentState(AppNavState.BROWSER)
    fun showCardGrid() = setContentState(AppNavState.CARD_GRID)
    fun showAddToSpace() = setContentState(AppNavState.ADD_TO_SPACE)
    fun showNeevaMenu() = setContentState(AppNavState.NEEVA_MENU)
    fun showSettings() = setContentState(AppNavState.SETTINGS)
    fun showHistory() = setContentState(AppNavState.HISTORY)

    fun onMenuItem(id: NeevaMenuItemId) {
        when (id) {
            NeevaMenuItemId.HOME -> {
                openUrl(Uri.parse(NeevaConstants.appURL))
                showBrowser()
            }

            NeevaMenuItemId.SPACES -> {
                openUrl(Uri.parse(NeevaConstants.appSpacesURL))
                showBrowser()
            }

            NeevaMenuItemId.SETTINGS -> {
                showSettings()
            }

            NeevaMenuItemId.HISTORY -> {
                showHistory()
            }

            else -> {
                // Unimplemented screens.
            }
        }
    }
}

@Composable
fun AppNav(
    model: AppNavModel,
    selectedTabModel: SelectedTabModel,
    historyViewModel: HistoryViewModel,
    domainViewModel: DomainViewModel,
    webLayerModel: WebLayerModel,
    urlBarModel: URLBarModel,
    cardViewModel: CardViewModel
) {
    Box {
        AddToSpaceSheet(
            appNavModel = model,
            selectedTabModel = selectedTabModel
        )

        NeevaMenuSheet(appNavModel = model)

        SettingsContainer(appNavModel = model)

        HistoryContainer(
            appNavModel = model,
            historyViewModel = historyViewModel,
            domainViewModel = domainViewModel
        )

        CardsContainer(
            appNavModel = model,
            webLayerModel = webLayerModel,
            domainViewModel = domainViewModel,
            urlBarModel = urlBarModel,
            cardViewModel = cardViewModel
        )
    }
}

enum class AppNavState {
    BROWSER,
    SETTINGS,
    ADD_TO_SPACE,
    NEEVA_MENU,
    HISTORY,
    CARD_GRID
}

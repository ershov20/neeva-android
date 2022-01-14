package com.neeva.app

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.neeva.app.browsing.BrowserWrapper
import com.neeva.app.browsing.WebLayerModel
import com.neeva.app.card.CardsContainer
import com.neeva.app.firstrun.FirstRunContainer
import com.neeva.app.history.HistoryContainer
import com.neeva.app.neeva_menu.NeevaMenuItemId
import com.neeva.app.neeva_menu.NeevaMenuSheet
import com.neeva.app.settings.SettingsContainer
import com.neeva.app.spaces.AddToSpaceSheet
import com.neeva.app.storage.Space
import com.neeva.app.storage.SpaceStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AppNavModel(
    private val webLayerModel: WebLayerModel,
    private val spaceStore: SpaceStore
) : ViewModel() {
    private val _state = MutableStateFlow(AppNavState.BROWSER)
    val state: StateFlow<AppNavState> = _state

    fun openUrl(uri: Uri) {
        webLayerModel.browserWrapperFlow.value.activeTabModel.loadUrl(uri, true)
        showBrowser()
    }

    private fun setContentState(state: AppNavState) {
        _state.value = state

        if (state == AppNavState.ADD_TO_SPACE) {
            viewModelScope.launch {
                spaceStore.refresh()
            }
        }
    }

    fun showTabSwitcher(useIncognito: Boolean) {
        setContentState(AppNavState.CARD_GRID)
        webLayerModel.switchToProfile(useIncognito = useIncognito)
    }

    fun showBrowser() = setContentState(AppNavState.BROWSER)
    fun showCardGrid() = setContentState(AppNavState.CARD_GRID)
    fun showAddToSpace() = setContentState(AppNavState.ADD_TO_SPACE)
    fun showNeevaMenu() = setContentState(AppNavState.NEEVA_MENU)
    fun showSettings() = setContentState(AppNavState.SETTINGS)
    fun showHistory() = setContentState(AppNavState.HISTORY)
    fun showFirstRun() = setContentState(AppNavState.FIRST_RUN)

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

    @Suppress("UNCHECKED_CAST")
    class AppNavModelFactory(
        private val webLayerModel: WebLayerModel,
        private val spaceStore: SpaceStore
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return AppNavModel(webLayerModel, spaceStore) as T
        }
    }
}

@Composable
fun AppNav(
    appNavModel: AppNavModel,
    browserWrapper: BrowserWrapper,
    spaceModifier: Space.Companion.SpaceModifier
) {
    Box {
        AddToSpaceSheet(
            appNavModel = appNavModel,
            activeTabModel = browserWrapper.activeTabModel,
            spaceModifier = spaceModifier
        )

        NeevaMenuSheet(appNavModel = appNavModel)

        SettingsContainer(appNavModel = appNavModel)

        // TODO(dan.alcantara): Should we be using the regular profile's favicon cache here?
        //                      The history UI always shows the regular profile's history.
        HistoryContainer(
            appNavModel = appNavModel,
            faviconCache = browserWrapper.faviconCache
        )

        CardsContainer(
            appNavModel = appNavModel,
            browserWrapper = browserWrapper
        )

        FirstRunContainer(appNavModel = appNavModel)
    }
}

enum class AppNavState {
    BROWSER,
    SETTINGS,
    ADD_TO_SPACE,
    NEEVA_MENU,
    HISTORY,
    CARD_GRID,
    FIRST_RUN
}

package com.neeva.app.browsing

import android.net.Uri
import androidx.lifecycle.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import org.chromium.weblayer.*
import kotlin.math.roundToInt

class SelectedTabModel(
    selectedTabFlow: MutableStateFlow<Pair<Tab?, Tab?>>,
    private val createTabFor: (Uri) -> Unit,
): ViewModel() {
    data class NavigationInfo(
        val canGoBackward: Boolean = false,
        val canGoForward: Boolean = false
    )

    companion object {
        class SelectedTabModelFactory(
            private val selectedTabFlow: MutableStateFlow<Pair<Tab?, Tab?>>,
            private val createTabFor: (Uri) -> Unit
        ) : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return SelectedTabModel(selectedTabFlow, createTabFor) as T
            }
        }
    }

    private val _urlFlow = MutableStateFlow(Uri.EMPTY)
    val urlFlow: StateFlow<Uri> = _urlFlow

    private val _titleFlow = MutableStateFlow("")
    val titleFlow: StateFlow<String> = _titleFlow

    private val _navigationInfoFlow = MutableStateFlow(NavigationInfo(false, false))
    val navigationInfoFlow: StateFlow<NavigationInfo> = _navigationInfoFlow

    private val _progressFlow = MutableStateFlow(0)
    val progressFlow: StateFlow<Int> = _progressFlow

    private var selectedTab: Tab? = null

    init {
        viewModelScope.launch {
            selectedTabFlow.filter { it.second != null }.collect { pair ->
                val previousTab = pair.first
                val activeTab = pair.second
                selectedTab = activeTab

                previousTab?.apply {
                    unregisterTabCallback(selectedTabCallback)
                    navigationController.unregisterNavigationCallback(selectedTabNavigationCallback)
                }

                activeTab?.apply {
                    registerTabCallback(selectedTabCallback)
                    navigationController.registerNavigationCallback(selectedTabNavigationCallback)
                    navigationController.apply {
                        val index = navigationListCurrentIndex
                        if (index != -1) {
                            _urlFlow.value = getNavigationEntryDisplayUri(index)
                            _titleFlow.value = getNavigationEntryTitle(index)
                        }
                    }
                }
            }
        }
    }

    fun reload() {
        selectedTab?.navigationController?.reload()
    }

    fun loadUrl(uri: Uri, newTab: Boolean = false) {
        if (newTab || selectedTab == null) {
            createTabFor(uri)
            return
        }

        // Disable intent processing for urls typed in. Allows the user to navigate to app urls.
        val navigateParamsBuilder = NavigateParams.Builder().disableIntentProcessing()
        selectedTab?.navigationController?.navigate(uri, navigateParamsBuilder.build())
    }

    private val selectedTabCallback: TabCallback = object : TabCallback() {
        override fun onVisibleUriChanged(uri: Uri) {
            _urlFlow.value = uri
        }

        override fun onTitleUpdated(title: String) {
            _titleFlow.value = title
        }
    }

    private val selectedTabNavigationCallback = object : NavigationCallback() {
        override fun onLoadProgressChanged(progress: Double) {
            _progressFlow.value = (100 * progress).roundToInt()
        }

        override fun onNavigationStarted(navigation: Navigation) {
            if (navigation.isSameDocument) return
            updateNavigationInfo()
        }

        override fun onNavigationCompleted(navigation: Navigation) {
            updateNavigationInfo()
        }
    }

    fun goBack() {
        selectedTab?.navigationController?.goBack()
        updateNavigationInfo()
    }

    fun goForward() {
        selectedTab?.navigationController?.goForward()
        updateNavigationInfo()
    }

    private fun updateNavigationInfo() {
        _navigationInfoFlow.value = NavigationInfo(
            selectedTab?.navigationController?.canGoBack() ?: false,
            selectedTab?.navigationController?.canGoForward() ?: false
        )
    }
}

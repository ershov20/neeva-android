package com.neeva.app.browsing

import android.net.Uri
import androidx.lifecycle.*
import com.neeva.app.NeevaConstants.appURL
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import org.chromium.weblayer.*
import kotlin.math.roundToInt

class SelectedTabModel(
    selectedTabFlow: MutableStateFlow<Pair<Tab?, Tab?>>,
    private val createTabFor: (Uri) -> Unit,
): ViewModel() {
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

    private val _currentUrl = MutableLiveData(Uri.parse(appURL))
    val currentUrl: LiveData<Uri> = _currentUrl

    private val _currentTitle = MutableLiveData("")
    val currentTitle: LiveData<String> = _currentTitle

    private val _canGoBack = MutableLiveData(false)
    val canGoBack: LiveData<Boolean> = _canGoBack

    private val _canGoForward = MutableLiveData(false)
    val canGoForward: LiveData<Boolean> = _canGoForward

    private val _progress = MutableLiveData(0)
    val progress: LiveData<Int> = _progress

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
                            _currentUrl.value = getNavigationEntryDisplayUri(index)
                            _currentTitle.value = getNavigationEntryTitle(index)
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
            _currentUrl.value = uri
        }

        override fun onTitleUpdated(title: String) {
            _currentTitle.value = title
        }
    }

    private val selectedTabNavigationCallback = object : NavigationCallback() {
        override fun onLoadProgressChanged(progress: Double) {
            _progress.value = (100 * progress).roundToInt()
        }

        override fun onNavigationStarted(navigation: Navigation) {
            if (navigation.isSameDocument) return

            _canGoBack.value = selectedTab?.navigationController?.canGoBack()
            _canGoForward.value = selectedTab?.navigationController?.canGoForward()
        }

        override fun onNavigationCompleted(navigation: Navigation) {
            _canGoBack.value = selectedTab?.navigationController?.canGoBack()
            _canGoForward.value = selectedTab?.navigationController?.canGoForward()
        }
    }

    fun goBack() {
        selectedTab?.navigationController?.goBack()

        _canGoBack.value = selectedTab?.navigationController?.canGoBack()
        _canGoForward.value = selectedTab?.navigationController?.canGoForward()
    }

    fun goForward() {
        selectedTab?.navigationController?.goForward()

        _canGoBack.value = selectedTab?.navigationController?.canGoBack()
        _canGoForward.value = selectedTab?.navigationController?.canGoForward()
    }
}

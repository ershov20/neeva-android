package com.neeva.app.browsing

import android.net.Uri
import androidx.lifecycle.*
import com.neeva.app.appURL
import kotlinx.coroutines.flow.*
import org.chromium.weblayer.*
import kotlin.math.roundToInt

class SelectedTabModel(
    selectedTabFlow: MutableStateFlow<Pair<Tab?, Tab?>>,
    private val registerTabCallbacks: (Tab) -> Unit,
    private val createTabFor: (Uri) -> Unit,
): ViewModel() {

    private val _currentUrl = MutableLiveData(Uri.parse(appURL)!!)
    val currentUrl: LiveData<Uri> = _currentUrl

    private val _currentTitle = MutableLiveData("")
    val currentTitle: LiveData<String> = _currentTitle

    private val _canGoBack = MutableLiveData(false)
    val canGoBack: LiveData<Boolean> = _canGoBack

    private val _canGoForward = MutableLiveData(false)
    val canGoForward: LiveData<Boolean> = _canGoForward

    private val _progress = MutableLiveData(0)
    val progress: LiveData<Int> = _progress

    private val _selectedTab: MutableLiveData<Tab?> = MutableLiveData(null)

    init {
        selectedTabFlow.filter { it.second != null }.asLiveData().observeForever { pair ->
            val previousTab = pair.first
            val activeTab = pair.second!!

            previousTab?.unregisterTabCallback(selectedTabCallback)
            previousTab?.navigationController?.unregisterNavigationCallback(selectedTabNavigationCallback)
            activeTab.registerTabCallback(selectedTabCallback)
            activeTab.navigationController.registerNavigationCallback(selectedTabNavigationCallback)
            activeTab.setNewTabCallback(newTabCallback)
            activeTab.setErrorPageCallback(errorPageCallback)

            val navController = activeTab.navigationController
            val index = navController.navigationListCurrentIndex

            if (index == -1) {
                return@observeForever
            }

            _currentUrl.value = navController.getNavigationEntryDisplayUri(index)
            _currentTitle.value = navController.getNavigationEntryTitle(index)

            _selectedTab.value = activeTab
        }
    }

    fun reload() {
        _selectedTab.value?.navigationController?.reload()
    }

    fun loadUrl(uri: Uri, newTab: Boolean = false) {
        if (newTab || _selectedTab.value == null) {
            createTabFor(uri)
            return
        }
        // Disable intent processing for urls typed in. Allows the user to navigate to app urls.
        val navigateParamsBuilder = NavigateParams.Builder().disableIntentProcessing()
        _selectedTab.value?.navigationController?.navigate(uri, navigateParamsBuilder.build())
    }

    private val newTabCallback: NewTabCallback = object : NewTabCallback() {
        override fun onNewTab(newTab: Tab, @NewTabType type: Int) {
            registerTabCallbacks(newTab)
        }
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

            _canGoBack.value = _selectedTab.value?.navigationController?.canGoBack()
            _canGoForward.value = _selectedTab.value?.navigationController?.canGoForward()
        }

        override fun onNavigationCompleted(navigation: Navigation) {
            _canGoBack.value = _selectedTab.value?.navigationController?.canGoBack()
            _canGoForward.value = _selectedTab.value?.navigationController?.canGoForward()
        }
    }

    private val errorPageCallback = object : ErrorPageCallback() {
        override fun onBackToSafety(): Boolean {
            goBack()
            return true
        }
    }

    fun goBack() {
        _selectedTab.value?.navigationController?.goBack()

        _canGoBack.value = _selectedTab.value?.navigationController?.canGoBack()
        _canGoForward.value = _selectedTab.value?.navigationController?.canGoForward()
    }

    fun goForward() {
        _selectedTab.value?.navigationController?.goForward()

        _canGoBack.value = _selectedTab.value?.navigationController?.canGoBack()
        _canGoForward.value = _selectedTab.value?.navigationController?.canGoForward()
    }
}


@Suppress("UNCHECKED_CAST")
class SelectedTabModelFactory(
    private val selectedTabFlow: MutableStateFlow<Pair<Tab?, Tab?>>,
    private val registerTabCallbacks: (Tab) -> Unit,
    private val createTabFor: (Uri) -> Unit
) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return SelectedTabModel(selectedTabFlow, registerTabCallbacks, createTabFor) as T
    }
}
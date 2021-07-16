package com.neeva.app

import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider


@Composable
fun WebPanel(webViewModel: WebViewModel) {
    AndroidView(
        factory = {
            webViewModel.webView
        },
    )
}

class WebViewModel(activity: AppCompatActivity): ViewModel() {
    private val _currentUrl = MutableLiveData("")
    val currentUrl: LiveData<String> = _currentUrl

    private val _currentTitle = MutableLiveData("")
    val currentTitle: LiveData<String> = _currentTitle

    private val _canGoBack = MutableLiveData(false)
    val canGoBack: LiveData<Boolean> = _canGoBack

    private val _canGoForward = MutableLiveData(false)
    val canGoForward: LiveData<Boolean> = _canGoForward

    private val webClient = WebClient(this)
    internal val webView by lazy {
        WebView(activity).apply {
            this.webViewClient = webClient
            this.settings.javaScriptEnabled = true
            this.loadUrl("https://neeva.com/")
        }
    }

    fun goBack() = webView.goBack()
    fun goForward() = webView.goForward()

    fun onCurrentPageChanged(url: String?, title: String?) {
        if (url?.isNotEmpty() == true) _currentUrl.value = url
        if (title?.isNotEmpty() == true) _currentTitle.value = title
        _canGoBack.value = webView.canGoBack()
        _canGoForward.value = webView.canGoForward()
    }
}

class WebViewModelFactory(activity: AppCompatActivity) :
    ViewModelProvider.Factory {
    private val activity: AppCompatActivity = activity
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return WebViewModel(activity) as T
    }
}
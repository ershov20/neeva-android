package com.neeva.app.web

import android.content.Context
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.neeva.app.appSearchURL
import com.neeva.app.storage.DomainViewModel


@Composable
fun WebPanel(webViewModel: WebViewModel) {
    AndroidView(
        factory = {
            webViewModel.webView
        },
    )
}

class WebViewModel(
        context: Context,
        private val domainViewModel: DomainViewModel
    ): ViewModel() {
    private val _currentUrl = MutableLiveData("")
    val currentUrl: LiveData<String> = _currentUrl

    private val _currentTitle = MutableLiveData("")
    val currentTitle: LiveData<String> = _currentTitle

    private val _canGoBack = MutableLiveData(false)
    val canGoBack: LiveData<Boolean> = _canGoBack

    private val _canGoForward = MutableLiveData(false)
    val canGoForward: LiveData<Boolean> = _canGoForward

    private val webClient = WebClient(this, domainViewModel)
    private val webNeevaClient = WebNeevaClient(domainViewModel)
    internal val webView by lazy {
        WebView(context).apply {
            this.webViewClient = webClient
            this.webChromeClient = webNeevaClient
            this.settings.javaScriptEnabled = true
            this.loadUrl("https://neeva.com/")
        }
    }

    fun goBack() = webView.goBack()
    fun goForward() = webView.goForward()

    fun loadUrl(url: String) {
        webView.loadUrl(url)
        webView.requestFocus()
    }

    fun onCurrentPageChanged(url: String?, title: String?) {
        if (url?.isNotEmpty() == true) _currentUrl.value = url
        if (title?.isNotEmpty() == true) _currentTitle.value = title
        _canGoBack.value = webView.canGoBack()
        _canGoForward.value = webView.canGoForward()
    }
}

@Suppress("UNCHECKED_CAST")
class WebViewModelFactory(activity: AppCompatActivity, domainViewModel: DomainViewModel) :
    ViewModelProvider.Factory {
    private val context: Context = activity.applicationContext
    private val domainModel = domainViewModel
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return WebViewModel(context, domainModel) as T
    }
}

fun String.toSearchUrl(): String {
    return "$appSearchURL?q=${this}&src=nvobar"
}
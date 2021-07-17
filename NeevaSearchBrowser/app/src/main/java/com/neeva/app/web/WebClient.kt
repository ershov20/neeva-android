package com.neeva.app.web

import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.neeva.app.User
import com.neeva.app.appURL

class WebClient(private val webViewModel: WebViewModel): WebViewClient() {

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        CookieManager.getInstance().getCookie(appURL)?.split("; ")?.forEach {
            if (it.split("=").first().equals("httpd~login")) {
                User.setToken(view!!.context.applicationContext, it.split("=").last())
            }
        }
        webViewModel.onCurrentPageChanged("", view?.title)
    }

    override fun onPageCommitVisible(view: WebView?, url: String?) {
        super.onPageCommitVisible(view, url)
        webViewModel.onCurrentPageChanged(url, view?.title)
    }
}
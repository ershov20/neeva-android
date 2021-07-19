package com.neeva.app.web

import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.neeva.app.User
import com.neeva.app.appURL
import com.neeva.app.storage.Domain
import com.neeva.app.storage.DomainViewModel
import java.util.*

class WebClient(
    private val webViewModel: WebViewModel,
    private val domainViewModel: DomainViewModel
    ): WebViewClient() {

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
        val domain = Uri.parse(url).authority ?: return

        domainViewModel.insert(Domain(
            domainName = domain, providerName = null, largestFavicon = null))
        webViewModel.onCurrentPageChanged(url, view?.title)
    }
}
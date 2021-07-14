package com.neeva.app

import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.apollographql.apollo.coroutines.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class WebClient: WebViewClient() {

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        CookieManager.getInstance().getCookie(appUrl).split("; ").forEach {
            if (it.split("=").first().equals("httpd~login")) {
                User.setToken(view!!.context.applicationContext, it.split("=").last())
            }
        }
    }
}
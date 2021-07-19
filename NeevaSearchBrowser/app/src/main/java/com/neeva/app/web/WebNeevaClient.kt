package com.neeva.app.web

import android.graphics.Bitmap
import android.webkit.WebChromeClient
import android.webkit.WebView
import com.neeva.app.storage.DomainViewModel
import com.neeva.app.storage.toFavicon

class WebNeevaClient(
        private val domainViewModel: DomainViewModel
    ): WebChromeClient() {
    override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
        super.onReceivedIcon(view, icon)
        val url = view?.url ?: return
        val favicon = icon ?: return

        domainViewModel.updateFaviconFor(url, favicon.toFavicon())
    }
}
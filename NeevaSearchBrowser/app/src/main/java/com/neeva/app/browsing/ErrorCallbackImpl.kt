package com.neeva.app.browsing

import org.chromium.weblayer.ErrorPage
import org.chromium.weblayer.ErrorPageCallback
import org.chromium.weblayer.Navigation
import org.chromium.weblayer.Tab

class ErrorCallbackImpl(val tab: Tab) : ErrorPageCallback() {
    // TODO(dan.alcantara): I don't know if we should be overriding this.
    override fun onBackToSafety(): Boolean {
        tab.navigationController.goBack()
        return true
    }

    // TODO(dan.alcantara): Although this should be showing the default error page, it
    //                      doesn't work.
    override fun getErrorPage(navigation: Navigation): ErrorPage? = null
}
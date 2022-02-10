package com.neeva.app.browsing

import org.chromium.weblayer.ErrorPage
import org.chromium.weblayer.ErrorPageCallback
import org.chromium.weblayer.Navigation

class ErrorCallbackImpl(
    private val activityCallbackProvider: () -> ActivityCallbacks?
) : ErrorPageCallback() {
    override fun onBackToSafety(): Boolean {
        activityCallbackProvider()?.onBackPressed()
        return true
    }

    // https://github.com/neevaco/neeva-android/issues/77
    // Even WebLayerShellActivity shows a blank white screen.
    override fun getErrorPage(navigation: Navigation): ErrorPage? = null
}

package com.neeva.app.browsing

import android.os.Handler
import android.os.Looper
import org.chromium.weblayer.Tab
import org.chromium.weblayer.TabCallback

/** Automatically reloads a tab when the renderer crashes. */
class CrashTabCallback(val tab: Tab) : TabCallback() {
    var consecutiveCrashes = 0

    override fun onRenderProcessGone() {
        consecutiveCrashes++

        if (consecutiveCrashes < 3 &&
            !tab.willAutomaticallyReloadAfterCrash() &&
            !tab.isDestroyed
        ) {
            // We have to delay the reload because onRenderProcessGone() is called synchronously.
            Handler(Looper.getMainLooper()).post {
                tab.navigationController.reload()
            }
        } else {
            consecutiveCrashes = 0
        }
    }
}

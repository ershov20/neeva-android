package com.neeva.app.browsing

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import org.chromium.weblayer.BuildConfig
import org.chromium.weblayer.Tab
import org.chromium.weblayer.TabCallback

/** Automatically reloads a tab when the renderer crashes. */
class CrashTabCallback(val tab: Tab, val appContext: Application) : TabCallback() {
    var consecutiveCrashes = 0

    override fun onRenderProcessGone() {
        consecutiveCrashes++

        if (consecutiveCrashes < 3 && !tab.willAutomaticallyReloadAfterCrash() && !tab.isDestroyed) {
            showDebugToast("Renderer crashed.  Automatically reloading")

            // We have to delay the reload because onRenderProcessGone() is called synchronously.
            Handler(Looper.getMainLooper()).post {
                tab.navigationController.reload()
            }
        } else {
            consecutiveCrashes = 0
            showDebugToast("Renderer crashed.  Must reload manually")
        }
    }

    private fun showDebugToast(text: String) {
        if (!BuildConfig.DEBUG) return
        Toast.makeText(appContext, text, Toast.LENGTH_SHORT).show()
    }
}

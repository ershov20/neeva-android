// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.contentfilter

import android.content.Context
import android.net.Uri
import androidx.annotation.WorkerThread
import com.neeva.app.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.chromium.weblayer.Tab
import org.chromium.weblayer.WebMessageCallback
import timber.log.Timber

class ScriptInjectionManager(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val dispatchers: Dispatchers
) {
    private var engineScript: Deferred<String?> = coroutineScope.async(dispatchers.io) {
        loadEngineScript()
    }

    fun initializeMessagePassing(tab: Tab, callbacks: WebMessageCallback) {
        if (tab.isDestroyed) return

        // Calling this method once registers the __neeva_broker object for all navigations in the
        // given [tab].
        try {
            tab.registerWebMessageCallback(callbacks, "__neeva_broker", listOf("*"))
        } catch (e: IllegalArgumentException) {
            // https://github.com/neevaco/neeva-android/issues/1003
            // It's not clear what we can do when this happens.  Play's crash logging doesn't
            // provide us the error message that WebLayer fires, and as far as I can tell, the
            // arguments that we're passing along are valid so the initial IllegalArgumentExceptions
            // in TabImpl.java shouldn't fire.
            // Because Cookie Cutter is optional, just drop the exception on the floor to avoid the
            // whole app going down.
            Timber.e("Failed to initialize Cookie Cutter", e)
        }
    }

    fun unregisterMessagePassing(tab: Tab) {
        if (tab.isDestroyed) return
        tab.unregisterWebMessageCallback("__neeva_broker")
    }

    fun injectNavigationCompletedScripts(
        uri: Uri,
        tab: Tab,
        tabContentFilterModel: TabContentFilterModel
    ) {
        coroutineScope.launch(dispatchers.main) {
            if (tab.isDestroyed) return@launch

            // If our preferences say we shouldn't activate cookie cutter, then don't. This depends
            // on whether or not cookie cutter is enabled globally, and on a per-site basis.
            if (!tabContentFilterModel.shouldInjectCookieEngine(uri.host ?: "")) {
                return@launch
            }

            val scriptText = withContext(dispatchers.io) {
                engineScript.await()
            } ?: return@launch

            // Note: if you are expecting this script to run when the document has loaded,
            // that would be incorrect. It actually runs like halfway though. Make sure to attach
            // event handlers to, e.g. DOMContentLoaded, so that logic only runs when it should
            if (tab.isDestroyed) return@launch
            tab.executeScript(scriptText, false, null)
        }
    }

    @WorkerThread
    private fun loadEngineScript(): String? {
        return try {
            context.assets.open("cookieCutterEngine.js")
                .bufferedReader()
                .use { it.readText() }
        } catch (e: Exception) {
            Timber.w("Error while fetching cookie cutter engine script, not injecting.", e)
            null
        }
    }
}

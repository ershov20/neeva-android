// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.settings.defaultbrowser

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.neeva.app.logging.ClientLogger
import com.neeva.app.logging.LogConfig
import java.lang.ref.WeakReference

@RequiresApi(Build.VERSION_CODES.Q)
class NeevaRoleManager(
    activity: AppCompatActivity,
    clientLogger: ClientLogger
) : SetDefaultAndroidBrowserManager() {
    private val androidDefaultBrowserRequester =
        activity.registerForActivityResult(DefaultAndroidBrowserRequester(this, clientLogger)) {
            onResultAvailable()
        }

    /** Tracks the callback that will fire when the dialog closes. */
    private var requestCallback: WeakReference<(Boolean) -> Unit> = WeakReference(null)

    private val roleManager = activity.getSystemService(Context.ROLE_SERVICE) as RoleManager

    override val isDefaultBrowser: MutableState<Boolean> =
        mutableStateOf(isNeevaTheDefaultBrowser())

    override fun isNeevaTheDefaultBrowser(): Boolean {
        return roleManager.isRoleHeld(RoleManager.ROLE_BROWSER)
    }

    override fun isRoleManagerAvailable(): Boolean {
        return true
    }

    override fun requestToBeDefaultBrowser(callback: (Boolean) -> Unit) {
        if (roleManager.isRoleAvailable(RoleManager.ROLE_BROWSER)) {
            requestCallback = WeakReference(callback)
            androidDefaultBrowserRequester.launch(null)
        }
    }

    internal fun makeRequestRoleIntent(): Intent {
        return roleManager.createRequestRoleIntent(RoleManager.ROLE_BROWSER)
    }

    private fun onResultAvailable() {
        requestCallback.get()?.invoke(isNeevaTheDefaultBrowser())
        requestCallback = WeakReference(null)
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
internal class DefaultAndroidBrowserRequester(
    private val neevaRoleManager: NeevaRoleManager,
    private val clientLogger: ClientLogger
) : ActivityResultContract<Void?, Boolean?>() {

    override fun createIntent(context: Context, input: Void?): Intent {
        return neevaRoleManager.makeRequestRoleIntent()
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Boolean? {
        neevaRoleManager.updateIsDefaultBrowser()
        if (neevaRoleManager.isNeevaTheDefaultBrowser()) {
            clientLogger.logCounter(LogConfig.Interaction.SET_DEFAULT_BROWSER, null)
        } else {
            clientLogger.logCounter(LogConfig.Interaction.SKIP_DEFAULT_BROWSER, null)
        }
        return null
    }
}

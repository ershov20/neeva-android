// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.settings.defaultbrowser

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.neeva.app.NeevaConstants
import com.neeva.app.logging.ClientLogger

/**
 * Used to check/set the Default Browser.
 * The subclass used here is dependent on the device's Android version.
 */
abstract class SetDefaultAndroidBrowserManager {
    abstract val isDefaultBrowser: MutableState<Boolean>

    fun updateIsDefaultBrowser() {
        isDefaultBrowser.value = isNeevaTheDefaultBrowser()
    }

    abstract fun isNeevaTheDefaultBrowser(): Boolean

    abstract fun isRoleManagerAvailable(): Boolean

    abstract fun requestToBeDefaultBrowser(callback: (neevaIsDefault: Boolean) -> Unit)

    companion object {
        fun create(
            activity: AppCompatActivity,
            neevaConstants: NeevaConstants,
            clientLogger: ClientLogger
        ): SetDefaultAndroidBrowserManager {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                NeevaRoleManager(activity, clientLogger)
            } else {
                PreQDefaultBrowserSetter(activity, neevaConstants)
            }
        }
    }
}

/** Used for Preview Testing */
class FakeSetDefaultAndroidBrowserManager : SetDefaultAndroidBrowserManager() {
    override val isDefaultBrowser: MutableState<Boolean> = mutableStateOf(true)

    override fun isNeevaTheDefaultBrowser(): Boolean {
        return true
    }

    override fun isRoleManagerAvailable(): Boolean {
        return true
    }

    override fun requestToBeDefaultBrowser(callback: (Boolean) -> Unit) {}
}

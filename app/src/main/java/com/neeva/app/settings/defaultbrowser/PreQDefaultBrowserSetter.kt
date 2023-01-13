// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.settings.defaultbrowser

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.neeva.app.NeevaConstants

/** Setting Android Default Browser for devices lower than Android Q. */
class PreQDefaultBrowserSetter(
    context: Context,
    private val neevaConstants: NeevaConstants
) : SetDefaultAndroidBrowserManager() {
    private val neevaPackageName = context.packageName
    private var packageManager: PackageManager = context.packageManager

    override val isDefaultBrowser: MutableState<Boolean> =
        mutableStateOf(isNeevaTheDefaultBrowser())

    override fun isNeevaTheDefaultBrowser(): Boolean {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(neevaConstants.appURL))

        // resolveActivity() is deprecated in API level 33, but we support down to API level 28.
        @Suppress("DEPRECATION")
        val resolveInfo: ResolveInfo? =
            packageManager.resolveActivity(browserIntent, PackageManager.MATCH_DEFAULT_ONLY)

        // Look at https://github.com/neevaco/neeva-android/issues/408 if it is not working the right way.
        if (resolveInfo != null) {
            val defaultBrowserPackageName: String = resolveInfo.activityInfo.packageName
            return defaultBrowserPackageName == neevaPackageName
        }
        return false
    }

    override fun isRoleManagerAvailable(): Boolean {
        return false
    }

    override fun requestToBeDefaultBrowser(callback: (Boolean) -> Unit) {}
}

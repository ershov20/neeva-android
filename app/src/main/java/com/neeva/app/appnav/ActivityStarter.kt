// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.appnav

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.util.Log
import com.neeva.app.R
import com.neeva.app.ui.PopupModel

class ActivityStarter(private val appContext: Context, private val popupModel: PopupModel) {
    fun safeStartActivityForIntent(intent: Intent) {
        try {
            appContext.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            popupModel.showSnackbar(appContext.getString(R.string.error_generic))
            Log.e(TAG, "Failed to start Activity for $intent")
        }
    }
    companion object {
        const val TAG = "ActivityStarter"
    }
}

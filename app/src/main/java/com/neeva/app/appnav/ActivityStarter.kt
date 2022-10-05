// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.appnav

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.neeva.app.R
import com.neeva.app.ui.PopupModel
import java.io.File

class ActivityStarter(private val appContext: Context, private val popupModel: PopupModel) {
    fun safeStartActivityForIntent(intent: Intent) {
        try {
            // Because we're using a non-Activity context, the Intent needs to start a new Task.
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            appContext.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            popupModel.showSnackbar(appContext.getString(R.string.error_generic))
            Log.e(TAG, "Failed to start Activity for $intent")
        }
    }

    fun safeOpenFile(fullFilePath: File, mimetype: String) {
        val uri = FileProvider.getUriForFile(
            appContext,
            appContext.packageName + ".provider",
            fullFilePath
        )
        safeOpenFile(uri, mimetype)
    }

    internal fun safeOpenFile(uri: Uri, mimetype: String) {
        try {
            val openFileIntent = Intent()
                .setAction(Intent.ACTION_VIEW)
                .setDataAndType(uri, mimetype)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            safeStartActivityForIntent(openFileIntent)
        } catch (e: Exception) {
            popupModel.showSnackbar(appContext.getString(R.string.error_generic))
            Log.e(TAG, "Failed to open file because of:", e)
        }
    }

    companion object {
        private const val TAG = "ActivityStarter"
    }
}

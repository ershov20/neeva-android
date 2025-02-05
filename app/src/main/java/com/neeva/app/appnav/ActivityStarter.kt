// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.appnav

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.FileProvider
import com.neeva.app.R
import com.neeva.app.ui.PopupModel
import java.io.File
import timber.log.Timber

class ActivityStarter(private val appContext: Context, private val popupModel: PopupModel) {

    fun safeStartActivityWithoutActivityTransition(intent: Intent) {
        safeStartActivityForIntent(
            intent = intent,
            // Nullify the transition animation to hide the fact that we're switching Activities.
            options = ActivityOptionsCompat.makeCustomAnimation(appContext, 0, 0).toBundle()
        )
    }

    fun safeStartActivityForIntent(
        intent: Intent,
        fallback: Intent? = null,
        options: Bundle? = null
    ) {
        // Because we're using a non-Activity context, the Intent needs to start a new Task.
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        try {
            appContext.startActivity(intent, options)
        } catch (e: ActivityNotFoundException) {
            if (fallback != null) {
                safeStartActivityForIntent(fallback, options = options)
            } else {
                popupModel.showSnackbar(appContext.getString(R.string.error_generic))
                Timber.e("Failed to start Activity for $intent")
            }
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
        } catch (throwable: Exception) {
            popupModel.showSnackbar(appContext.getString(R.string.error_generic))
            Timber.e(
                t = throwable,
                message = "Failed to open file because of:"
            )
        }
    }
}

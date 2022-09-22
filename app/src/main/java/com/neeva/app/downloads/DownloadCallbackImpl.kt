// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.downloads

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.ValueCallback
import com.neeva.app.R
import com.neeva.app.appnav.ActivityStarter
import com.neeva.app.ui.PopupModel
import org.chromium.weblayer.Download
import org.chromium.weblayer.DownloadCallback

class DownloadCallbackImpl(
    private val popupModel: PopupModel,
    private val appContext: Context,
    private val activityStarter: ActivityStarter
) : DownloadCallback() {
    override fun onInterceptDownload(
        uri: Uri,
        userAgent: String,
        contentDisposition: String,
        mimetype: String,
        contentLength: Long
    ): Boolean = false

    override fun allowDownload(
        uri: Uri,
        requestMethod: String,
        requestInitiator: Uri?,
        callback: ValueCallback<Boolean>
    ) { }

    override fun onDownloadStarted(download: Download) {
        super.onDownloadStarted(download)
        val downloadFileName = download.fileNameToReportToUser
        popupModel.showSnackbar(
            message = appContext.resources.getString(
                R.string.download_started,
                downloadFileName
            )
        )
    }

    override fun onDownloadFailed(download: Download) {
        super.onDownloadFailed(download)
        val downloadFileName = download.fileNameToReportToUser
        popupModel.showSnackbar(
            message = appContext.resources.getString(
                R.string.download_failed,
                downloadFileName
            )
        )
    }

    override fun onDownloadCompleted(download: Download) {
        super.onDownloadCompleted(download)
        // Storing as separate variables so onActionPerformed lambda does not store Download object
        // (which will get destroyed when Download is completed)
        val location = download.location
        val mimetype = download.mimeType
        val downloadFileName = download.fileNameToReportToUser
        popupModel.showSnackbar(
            message = appContext.resources.getString(R.string.download_completed, downloadFileName),
            actionLabel = appContext.resources.getString(R.string.download_view),
            onActionPerformed = {
                val openFileIntent = Intent()
                    .setAction(Intent.ACTION_VIEW)
                    .setDataAndType(Uri.fromFile(location), mimetype)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                activityStarter.safeStartActivityForIntent(openFileIntent)
            }
        )
    }

    companion object {
        private const val TAG = "DownloadCallbackImpl"
    }
}

// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.downloads

import android.content.Context
import android.net.Uri
import android.webkit.ValueCallback
import androidx.annotation.StringRes
import com.neeva.app.R
import com.neeva.app.appnav.ActivityStarter
import com.neeva.app.ui.PopupModel
import java.io.File
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
    ) {
        // This callback invocation actually provides us the opportunity to implement a dialog
        // asking the user to confirm if they want to download the file.
        callback.onReceiveValue(true)
    }

    private fun showSnackbar(downloadFilename: File, @StringRes messageStringId: Int) {
        popupModel.showSnackbar(
            message = appContext.resources.getString(
                messageStringId,
                downloadFilename
            )
        )
    }

    override fun onDownloadStarted(download: Download) {
        super.onDownloadStarted(download)
        val downloadFilename = download.fileNameToReportToUser
        showSnackbar(downloadFilename, R.string.download_started)
    }

    override fun onDownloadFailed(download: Download) {
        super.onDownloadFailed(download)
        val downloadFilename = download.fileNameToReportToUser
        showSnackbar(downloadFilename, R.string.download_failed)
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
                activityStarter.safeOpenFile(location, mimetype)
            }
        )
    }

    companion object {
        private const val TAG = "DownloadCallbackImpl"
    }
}

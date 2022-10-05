// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.browsing

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neeva.app.BaseTest
import com.neeva.app.R
import com.neeva.app.appnav.ActivityStarter
import com.neeva.app.downloads.DownloadCallbackImpl
import com.neeva.app.ui.PopupModel
import java.io.File
import org.chromium.weblayer.Download
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@RunWith(AndroidJUnit4::class)
class DownloadCallbackImplTest : BaseTest() {
    lateinit var downloadCallback: DownloadCallbackImpl

    // DownloadCallback dependencies
    private lateinit var application: Application
    @Mock private lateinit var popupModel: PopupModel
    @Mock private lateinit var activityStarter: ActivityStarter

    // Test inputs
    @Mock private lateinit var download: Download
    private val downloadFullPath = File("Downloads/test_file.png")
    private val downloadFileName = File(downloadFullPath.name)
    private val downloadMimeType = "image/png"

    override fun setUp() {
        super.setUp()
        application = ApplicationProvider.getApplicationContext()

        downloadCallback = DownloadCallbackImpl(
            popupModel = popupModel,
            appContext = application,
            activityStarter = activityStarter
        )
        download = mock {
            on { fileNameToReportToUser } doReturn downloadFileName
            on { location } doReturn downloadFullPath
            on { mimeType } doReturn downloadMimeType
        }
    }

    @Test
    fun onDownloadStarted_correctPopupShowsUp() {
        downloadCallback.onDownloadStarted(download)

        verify(popupModel, times(1)).showSnackbar(
            message = application.resources.getString(R.string.download_started, downloadFileName)
        )
    }

    @Test
    fun onDownloadCompleted_correctPopupShowsUp() {
        downloadCallback.onDownloadCompleted(download)
        val onActionPerformedLambda = argumentCaptor<() -> Unit>()
        verify(popupModel, times(1)).showSnackbar(
            message = eq(
                application.resources.getString(
                    R.string.download_completed,
                    downloadFileName
                )
            ),
            actionLabel = eq(
                application.resources.getString(
                    R.string.download_view
                )
            ),
            duration = any(),
            onActionPerformed = onActionPerformedLambda.capture(),
            onDismissed = any()
        )

        onActionPerformedLambda.lastValue.invoke()

        val capturedFileToOpen = argumentCaptor<File>()
        val capturedExpectedMimeType = argumentCaptor<String>()
        verify(activityStarter, times(1)).safeOpenFile(
            capturedFileToOpen.capture(),
            capturedExpectedMimeType.capture()
        )

        // Verify we are opening the right file
        expectThat(capturedFileToOpen.lastValue).isEqualTo(downloadFullPath)
        expectThat(capturedExpectedMimeType.lastValue).isEqualTo(downloadMimeType)
    }

    @Test
    fun onDownloadFailed_correctPopupShowsUp() {
        downloadCallback.onDownloadFailed(download)

        verify(popupModel, times(1)).showSnackbar(
            message = application.resources.getString(
                R.string.download_failed,
                downloadFileName
            )
        )
    }
}

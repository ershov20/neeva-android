// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.browsing

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neeva.app.BaseTest
import com.neeva.app.appnav.ActivityStarter
import com.neeva.app.ui.PopupModel
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.kotlin.argumentCaptor
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@RunWith(AndroidJUnit4::class)
class ActivityStarterTest : BaseTest() {
    private lateinit var activityStarter: ActivityStarter
    private lateinit var application: Application

    @Mock
    private lateinit var popupModel: PopupModel

    override fun setUp() {
        super.setUp()
        application = ApplicationProvider.getApplicationContext()
        activityStarter = Mockito.spy(ActivityStarter(application, popupModel))
    }

    @Test
    fun safeOpenFile_usesCorrectIntentParams() {
        val downloadFileMimeType = "png"
        // Not able to test if FileProvider creates the right uri because FileProvider.getUriForFile
        // doesn't work in unit tests (there's no real folder or uri path here).
        // Using a reflection and swapping out a fake FileProvider would be a workaround but
        // because that forces us to use reflections to access that static java method.
        val downloadUri = Uri.parse("someFile.png")
        activityStarter.safeOpenFile(downloadUri, downloadFileMimeType)

        val activityStarterIntentUsed = argumentCaptor<Intent>()
        verify(activityStarter).safeStartActivityForIntent(activityStarterIntentUsed.capture())

        activityStarterIntentUsed.lastValue.apply {
            expectThat(action).isEqualTo(Intent.ACTION_VIEW)
            expectThat(data).isEqualTo(downloadUri)
            expectThat(type).isEqualTo(downloadFileMimeType)
            expectThat(flags).isEqualTo(
                Intent.FLAG_ACTIVITY_NEW_TASK.or(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            )
        }
    }
}

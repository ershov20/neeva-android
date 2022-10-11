// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.browsing

import android.Manifest
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.lifecycle.Lifecycle
import androidx.test.rule.GrantPermissionRule
import com.neeva.app.BaseBrowserTest
import com.neeva.app.NeevaActivity
import com.neeva.app.R
import com.neeva.app.assertionToBoolean
import com.neeva.app.clickOnNodeWithText
import com.neeva.app.flakyLongPressOnBrowserView
import com.neeva.app.getString
import com.neeva.app.loadUrlByClickingOnBar
import com.neeva.app.waitForActivityStartup
import com.neeva.app.waitForBrowserState
import com.neeva.app.waitForNodeWithText
import com.neeva.testcommon.WebpageServingRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isTrue

@HiltAndroidTest
class DownloadFileTest : BaseBrowserTest() {
    @get:Rule(order = 10000)
    val androidComposeRule = createAndroidComposeRule<NeevaActivity>()

    @get:Rule
    val runtimePermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    @Test
    fun downloadImages_UniqueRenamingFrom1To100AndOnwards() {
        val imageLinkUrl = WebpageServingRule.urlFor("image_link_element.html")

        androidComposeRule.apply {
            activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
            downloadAndConfirmProperRenaming(
                linkUrl = imageLinkUrl,
                fileName = "image.png",
                filesToDownload = 110,
                openContextMenuAndDownload = {
                    flakyLongPressOnBrowserView {
                        return@flakyLongPressOnBrowserView assertionToBoolean {
                            waitForNodeWithText("$imageLinkUrl?page_index=2").assertExists()
                        }
                    }
                    waitForNodeWithText("Image alt title").assertExists()
                    waitForNodeWithText(getString(R.string.menu_download_image)).assertExists()
                    onNodeWithTag("MenuHeaderImage").assertExists()

                    // Start the first download
                    clickOnNodeWithText(getString(R.string.menu_download_image))
                    waitForIdle()
                }
            )
        }
    }

    @Test
    fun downloadLink_whenDownloadingOneFile_successfullySavesFile() {
        val linkUrl = WebpageServingRule.urlFor("big_link_element.html")

        androidComposeRule.apply {
            activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
            downloadAndConfirmProperRenaming(
                linkUrl = linkUrl,
                fileName = "big_link_element.html",
                filesToDownload = 1,
                openContextMenuAndDownload = {
                    flakyLongPressOnBrowserView {
                        return@flakyLongPressOnBrowserView assertionToBoolean {
                            waitForNodeWithText("$linkUrl?page_index=2").assertExists()
                        }
                    }
                    waitForNodeWithText(getString(R.string.menu_download_link)).assertExists()

                    // Start the download
                    clickOnNodeWithText(getString(R.string.menu_download_link))
                    waitForIdle()
                }
            )
        }
    }

    @Test
    fun downloadVideo_whenDownloadingOneFile_successfullySavesFile() {
        val videoLinkUrl = WebpageServingRule.urlFor("video_link_element.html")

        androidComposeRule.apply {
            activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
            downloadAndConfirmProperRenaming(
                linkUrl = videoLinkUrl,
                fileName = "video.mov",
                filesToDownload = 1,
                openContextMenuAndDownload = {
                    flakyLongPressOnBrowserView {
                        return@flakyLongPressOnBrowserView assertionToBoolean {
                            waitForNodeWithText("$videoLinkUrl?page_index=2").assertExists()
                        }
                    }
                    waitForNodeWithText(getString(R.string.menu_download_video)).assertExists()

                    // Start the download
                    clickOnNodeWithText(getString(R.string.menu_download_video))
                    waitForIdle()
                }
            )
        }
    }

    @Test
    fun downloadFile_openingFile_doesNotCrash() {
        val imageLinkUrl = WebpageServingRule.urlFor("image_link_element.html")
        androidComposeRule.apply {
            activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
            waitForActivityStartup()
            waitForBrowserState(isIncognito = false, expectedNumRegularTabs = 1)

            // Load the test webpage up in the existing tab.
            loadUrlByClickingOnBar(imageLinkUrl)

            flakyLongPressOnBrowserView {
                return@flakyLongPressOnBrowserView assertionToBoolean {
                    waitForNodeWithText("$imageLinkUrl?page_index=2").assertExists()
                }
            }

            waitForNodeWithText("Image alt title").assertExists()
            waitForNodeWithText(getString(R.string.menu_download_image)).assertExists()
            onNodeWithTag("MenuHeaderImage").assertExists()

            // Start the download
            clickOnNodeWithText(getString(R.string.menu_download_image))

            // Attempt to view the download by pressing "View" from the snackbar popup
            waitForNodeWithText(
                activity.getString(
                    R.string.download_completed,
                    "image.png"
                )
            ).assertExists()
            clickOnNodeWithText(getString(R.string.download_view))

            waitForIdle()
        }
    }

    /**
     * Downloads [filesToDownload] files from a given [linkUrl] and checks if they are named properly.
     * The first 100 downloaded files with duplicate names will get a unique number appended to their
     * name. The next 10 files will have a unique timestamp added to them.
     * For details on why, see: https://github.com/neevaco/neeva-android/pull/999
     */
    private fun downloadAndConfirmProperRenaming(
        linkUrl: String,
        fileName: String,
        filesToDownload: Int,
        openContextMenuAndDownload: () -> Unit
    ) {
        androidComposeRule.apply {
            waitForActivityStartup()
            waitForBrowserState(isIncognito = false, expectedNumRegularTabs = 1)

            // Load the test webpage up in the existing tab.
            loadUrlByClickingOnBar(linkUrl)

            for (i in 1..filesToDownload) {
                openContextMenuAndDownload()
            }

            val fileNameWithoutExtension = fileName.substring(0, fileName.lastIndexOf("."))
            val extension = fileName.substring(fileName.lastIndexOf("."))

            // This downloadDirectory should be set to the cache directory to reset after each test.
            val downloadDirectory = activity.neevaConstants.downloadDirectory
            val downloadedFilesNamesSet = downloadDirectory.listFiles()!!
                .filter { file -> file.isFile }
                .map { file -> file.name }
                .filter { name -> name.contains(fileNameWithoutExtension) }
                .toSet()

            // Confirm that each file is named properly
            expectThat(downloadedFilesNamesSet.contains(fileName)).isTrue()

            if (filesToDownload > 1) {
                for (suffix in 1..100) {
                    val expectedName = "$fileNameWithoutExtension ($suffix)$extension"
                    expectThat(downloadedFilesNamesSet.contains(expectedName))
                }
            }

            // If more than 101 files have been downloaded, the rest of them will have
            // unique timestamps appended to them.
            // Testing that they are unique is all that matters, this is covered by checking that
            // the filename set size is equal to the number of files downloaded.
            expectThat(downloadedFilesNamesSet.size == filesToDownload)
        }
    }
}

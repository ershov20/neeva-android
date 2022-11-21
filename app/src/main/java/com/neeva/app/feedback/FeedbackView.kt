// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.feedback

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import com.neeva.app.LocalAppNavModel
import com.neeva.app.LocalFeedbackViewModel
import com.neeva.app.LocalPopupModel
import com.neeva.app.R
import com.neeva.app.ui.AnimatedExpandShrink
import com.neeva.app.ui.FullScreenDialogTopBar
import com.neeva.app.ui.LandscapePreviews
import com.neeva.app.ui.NeevaSwitch
import com.neeva.app.ui.NeevaThemePreviewContainer
import com.neeva.app.ui.PortraitPreviews
import com.neeva.app.ui.SectionHeader
import com.neeva.app.ui.createCheckerboardBitmap
import com.neeva.app.ui.layouts.BaseRowLayout
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.ui.widgets.PartiallyClickableText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Composable
fun FeedbackView(currentURLFlow: StateFlow<Uri>) {
    val appNavModel = LocalAppNavModel.current
    val feedbackViewModel = LocalFeedbackViewModel.current
    val popupModel = LocalPopupModel.current

    val onDismiss = {
        appNavModel.popBackStack()
        feedbackViewModel.removeScreenshot()
    }

    val acknowledgementString = stringResource(R.string.submit_feedback_acknowledgement)

    FeedbackView(
        currentURLFlow = currentURLFlow,
        screenshot = feedbackViewModel.screenshot,
        onShowHelp = appNavModel::showHelp,
        onSubmitFeedback = { feedback, url, sendScreenshot ->
            feedbackViewModel.submitFeedback(
                userFeedback = feedback,
                url = url,
                screenshot = feedbackViewModel.screenshot?.takeIf { sendScreenshot }
            )

            popupModel.showSnackbar(acknowledgementString)

            onDismiss()
        },
        onDismiss = onDismiss
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackView(
    currentURLFlow: StateFlow<Uri>,
    screenshot: Bitmap?,
    onShowHelp: () -> Unit,
    onSubmitFeedback: (feedback: String, url: String?, sendScreenshot: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val currentURL by currentURLFlow.collectAsState()

    var feedback by rememberSaveable { mutableStateOf("") }
    var shareScreenshot by rememberSaveable { mutableStateOf(true) }
    var shareUrl by rememberSaveable { mutableStateOf(true) }

    // Update the URL that will be sent whenever the URL of the current page changes.
    val urlToSend = rememberSaveable(currentURL) { mutableStateOf(currentURL.toString()) }

    Scaffold(
        topBar = {
            FullScreenDialogTopBar(
                title = stringResource(R.string.feedback),
                onBackPressed = onDismiss,
                isButtonEnabled = feedback.isNotEmpty(),
                buttonPainter = painterResource(id = R.drawable.ic_send),
                contentDescription = stringResource(R.string.send),
                onButtonPressed = {
                    onSubmitFeedback(
                        feedback,
                        urlToSend.value.takeIf { shareUrl },
                        shareScreenshot
                    )
                }
            )
        }
    ) {
        Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(it)
        ) {
            Column {
                Box(
                    Modifier.padding(
                        horizontal = Dimensions.PADDING_LARGE,
                        vertical = Dimensions.PADDING_MEDIUM
                    )
                ) {
                    PartiallyClickableText {
                        Span(stringResource(R.string.submit_feedback_help_center_title))
                        ClickableSpan(
                            stringResource(R.string.submit_feedback_help_center_link),
                            onClick = onShowHelp
                        )
                    }
                }
                SectionHeader()
                Column {
                    BaseRowLayout {
                        Text(
                            stringResource(R.string.submit_feedback_textfield_placeholder),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    OutlinedTextField(
                        value = feedback,
                        onValueChange = { newValue -> feedback = newValue },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = Dimensions.PADDING_LARGE,
                                vertical = Dimensions.PADDING_SMALL
                            )
                            .testTag("feedbackField")
                    )

                    NeevaSwitch(
                        primaryLabel = stringResource(R.string.submit_feedback_view_share_url),
                        isChecked = shareUrl,
                        onCheckedChange = { shareUrl = it }
                    )

                    AnimatedExpandShrink(isVisible = shareUrl) {
                        OutlinedTextField(
                            value = urlToSend.value,
                            onValueChange = { urlToSend.value = it },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = Dimensions.PADDING_LARGE,
                                    vertical = Dimensions.PADDING_SMALL
                                )
                                .semantics { testTag = "Feedback URL" }
                        )
                    }

                    if (screenshot != null) {
                        NeevaSwitch(
                            primaryLabel = stringResource(R.string.submit_feedback_screenshot),
                            isChecked = shareScreenshot,
                            onCheckedChange = { shareScreenshot = it }
                        )

                        AnimatedExpandShrink(isVisible = shareScreenshot) {
                            ScreenshotThumbnail(
                                bitmap = screenshot,
                                modifier = Modifier.padding(
                                    horizontal = Dimensions.PADDING_LARGE,
                                    vertical = Dimensions.PADDING_SMALL
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@PortraitPreviews
@LandscapePreviews
@Composable
fun FeedbackViewPreview_LightTheme() {
    val bitmap = createCheckerboardBitmap(false)

    NeevaThemePreviewContainer(useDarkTheme = false) {
        val currentURLFlow = MutableStateFlow(Uri.parse("https://www.example.com"))

        FeedbackView(
            currentURLFlow = currentURLFlow,
            screenshot = bitmap,
            onShowHelp = {},
            onSubmitFeedback = { _, _, _ -> },
            onDismiss = {}
        )
    }
}

@PortraitPreviews
@LandscapePreviews
@Composable
fun FeedbackViewPreview_DarkTheme() {
    val bitmap = createCheckerboardBitmap(false)

    NeevaThemePreviewContainer(useDarkTheme = true) {
        val currentURLFlow = MutableStateFlow(Uri.parse("https://www.example.com"))

        FeedbackView(
            currentURLFlow = currentURLFlow,
            screenshot = bitmap,
            onShowHelp = {},
            onSubmitFeedback = { _, _, _ -> },
            onDismiss = {}
        )
    }
}

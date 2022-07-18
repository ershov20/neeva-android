package com.neeva.app.feedback

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.style.TextDecoration
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
import com.neeva.app.ui.createCheckerboardBitmap
import com.neeva.app.ui.theme.Dimensions
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

    val feedback = rememberSaveable { mutableStateOf("") }
    val shareScreenshot = rememberSaveable { mutableStateOf(true) }
    val shareUrl = rememberSaveable { mutableStateOf(true) }

    // Update the URL that will be sent whenever the URL of the current page changes.
    val urlToSend = rememberSaveable(currentURL) { mutableStateOf(currentURL.toString()) }

    Scaffold(
        topBar = {
            FullScreenDialogTopBar(
                title = stringResource(R.string.feedback),
                onBackPressed = onDismiss,
                buttonTitle = stringResource(R.string.send),
                onButtonPressed = {
                    onSubmitFeedback(
                        feedback.value,
                        urlToSend.value.takeIf { shareUrl.value },
                        shareScreenshot.value
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
                Spacer(Modifier.height(Dimensions.PADDING_LARGE))

                Text(
                    text = stringResource(R.string.submit_feedback_help_center_title),
                    modifier = Modifier.padding(horizontal = Dimensions.PADDING_LARGE)
                )

                Text(
                    text = stringResource(R.string.submit_feedback_help_center_link),
                    style = LocalTextStyle.current.copy(
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline
                    ),
                    modifier = Modifier
                        .clickable { onShowHelp() }
                        .padding(horizontal = Dimensions.PADDING_LARGE)
                )

                Spacer(Modifier.height(Dimensions.PADDING_LARGE))

                // Feedback text box
                OutlinedTextField(
                    value = feedback.value,
                    onValueChange = { newValue -> feedback.value = newValue },
                    placeholder = {
                        Text(stringResource(R.string.submit_feedback_textfield_placeholder))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimensions.PADDING_LARGE)
                )

                Spacer(Modifier.height(Dimensions.PADDING_LARGE))

                // Share URL text box
                NeevaSwitch(
                    primaryLabel = stringResource(R.string.submit_feedback_view_share_url),
                    isChecked = shareUrl.value,
                    onCheckedChange = { shareUrl.value = it }
                )

                AnimatedExpandShrink(isVisible = shareUrl.value) {
                    OutlinedTextField(
                        value = urlToSend.value,
                        onValueChange = { urlToSend.value = it },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Dimensions.PADDING_LARGE)
                            .semantics { testTag = "Feedback URL" }
                    )
                }

                if (screenshot != null) {
                    Spacer(Modifier.height(Dimensions.PADDING_LARGE))

                    NeevaSwitch(
                        primaryLabel = stringResource(R.string.submit_feedback_share_screenshot),
                        isChecked = shareScreenshot.value,
                        onCheckedChange = { shareScreenshot.value = it }
                    )

                    AnimatedExpandShrink(isVisible = shareScreenshot.value) {
                        ScreenshotThumbnail(
                            bitmap = screenshot,
                            modifier = Modifier.padding(horizontal = Dimensions.PADDING_LARGE)
                        )
                    }
                }

                Spacer(Modifier.height(Dimensions.PADDING_LARGE))
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

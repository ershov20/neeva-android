package com.neeva.app.feedback

import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.rememberImagePainter
import com.neeva.app.LocalEnvironment
import com.neeva.app.R
import com.neeva.app.ui.FullScreenDialogTopBar
import com.neeva.app.ui.theme.NeevaTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Composable
fun FeedbackView(
    feedbackViewModel: FeedbackViewModel,
    currentURL: StateFlow<Uri>
) {
    var text = rememberSaveable { mutableStateOf("") }

    val imageUri: MutableState<Uri?> = rememberSaveable { mutableStateOf(null) }
    val imageGalleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) {
        imageUri.value = it
    }

    val shouldShareURL = remember { mutableStateOf(true) }
    val url = remember { mutableStateOf(currentURL.value.toString()) }

    val apolloWrapper = LocalEnvironment.current.apolloWrapper
    val dispatchers = LocalEnvironment.current.dispatchers
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .fillMaxSize()
    ) {
        FullScreenDialogTopBar(
            title = stringResource(R.string.feedback),
            onBackPressed = { feedbackViewModel.onBackPressed() },
            buttonTitle = stringResource(R.string.submit_feedback),
            buttonPressed = {
                feedbackViewModel.onSubmitFeedbackPressed(
                    feedback = text.value,
                    imageUri = imageUri.value,
                    url = if (shouldShareURL.value) {
                        url.value
                    } else {
                        null
                    },
                    apolloWrapper = apolloWrapper,
                    dispatchers = dispatchers,
                    contentResolver = context.contentResolver
                )
            }
        )

        Column(
            modifier = Modifier
                .padding(top = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Help center text
            val annotatedString = buildAnnotatedString {
                // Need help CTA
                withStyle(
                    style = SpanStyle(
                        color = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    append(stringResource(R.string.submit_feedback_help_center_title))
                }

                append("\n")

                // Link to Neeva help center
                pushStringAnnotation(tag = "helpCenterLink", annotation = "https://help.neeva.com/")
                withStyle(
                    style = SpanStyle(
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline
                    )
                ) {
                    append(stringResource(R.string.submit_feedback_help_center_link))
                }

                pop()
            }

            // Displays the attributed text, and allows use to click on help center URL
            ClickableText(
                text = annotatedString,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 20.dp),
                onClick = { offset ->
                    annotatedString.getStringAnnotations(
                        tag = "helpCenterLink",
                        start = offset,
                        end = offset
                    ).firstOrNull()?.let {
                        feedbackViewModel.onOpenHelpCenterPressed()
                    }
                }
            )

            TextField(
                modifier = Modifier
                    .height(IntrinsicSize.Min)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                value = text.value,
                textStyle = MaterialTheme.typography.bodyLarge,
                placeholder = {
                    Text(
                        text = stringResource(R.string.submit_feedback_textfield_placeholder),
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                },
                onValueChange = { text.value = it },
                singleLine = false,
                colors = TextFieldDefaults.textFieldColors(
                    textColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                    cursorColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(10.dp)
            )

            ShareScreenshotView(
                feedbackViewModel,
                imageUri,
                imageGalleryLauncher
            )

            ShareURLView(
                url,
                shouldShareURL
            )
        }
    }
}

@Composable
private fun ShareScreenshotView(
    feedbackViewModel: FeedbackViewModel,
    imageUri: MutableState<Uri?>,
    imageGalleryLauncher: ManagedActivityResultLauncher<String, Uri?>
) {
    FeedbackOptionBox(
        bottomPadding = if (imageUri.value != null) {
            16.dp
        } else {
            0.dp
        },
        header = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(
                            top = if (imageUri.value == null) {
                                8.dp
                            } else {
                                0.dp
                            }
                        )
                ) {
                    Text(
                        stringResource(
                            R.string.submit_feedback_share_screenshot
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (imageUri.value != null) {
                    TextButton(
                        modifier = Modifier
                            .defaultMinSize(minWidth = 1.dp, minHeight = 1.dp),
                        contentPadding = PaddingValues(0.dp),
                        onClick = {
                            imageUri.value = null
                        }
                    ) {
                        Text(
                            stringResource(
                                R.string.submit_feedback_remove_screenshot
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    ) {
        if (imageUri.value == null) {
            TextButton(
                modifier = Modifier
                    .padding(
                        bottom = if (imageUri.value != null) {
                            8.dp
                        } else {
                            0.dp
                        }
                    )
                    .defaultMinSize(minWidth = 1.dp),
                contentPadding = PaddingValues(0.dp),
                onClick = {
                    imageGalleryLauncher.launch("image/*")
                }
            ) {
                Text(
                    text = stringResource(R.string.submit_feedback_upload_screenshot),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        } else {
            Image(
                modifier = Modifier
                    .height(200.dp)
                    .clickable {
                        feedbackViewModel.showImagePreview(imageUri.value ?: Uri.EMPTY)
                    },
                painter = rememberImagePainter(imageUri.value),
                contentDescription = stringResource(
                    R.string.submit_feedback_share_screenshot_preview
                )
            )
        }
    }
}

@Composable
private fun ShareURLView(
    url: MutableState<String>,
    toggleState: MutableState<Boolean>
) {
    FeedbackOptionBox(
        modifier = Modifier.padding(bottom = 12.dp),
        header = {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 8.dp)
                ) {
                    Text(
                        stringResource(
                            R.string.submit_feedback_view_share_url
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }

                Switch(
                    checked = toggleState.value,
                    modifier = Modifier
                        .height(IntrinsicSize.Min),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurface,
                        checkedTrackColor = MaterialTheme.colorScheme.inversePrimary,
                        uncheckedTrackColor = MaterialTheme.colorScheme.outline,
                        disabledCheckedThumbColor = MaterialTheme.colorScheme.inverseOnSurface,
                        disabledUncheckedThumbColor = MaterialTheme.colorScheme.inverseOnSurface,
                        disabledCheckedTrackColor = MaterialTheme.colorScheme.onSurface,
                        disabledUncheckedTrackColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    onCheckedChange = { toggleState.value = it }
                )
            }
        }
    ) {
        TextField(
            modifier = Modifier
                .padding(top = 7.dp)
                .padding(bottom = 14.dp)
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            value = url.value,
            textStyle = MaterialTheme.typography.bodyLarge,
            onValueChange = { url.value = it },
            singleLine = false,
            colors = TextFieldDefaults.textFieldColors(
                textColor = MaterialTheme.colorScheme.onBackground,
                backgroundColor = MaterialTheme.colorScheme.surface,
                cursorColor = MaterialTheme.colorScheme.onBackground,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            ),
            shape = RoundedCornerShape(10.dp)
        )
    }
}

@Composable
private fun FeedbackOptionBox(
    modifier: Modifier = Modifier,
    verticalSpacing: Dp = -8.dp,
    bottomPadding: Dp = 0.dp,
    header: @Composable (() -> Unit),
    content: @Composable (() -> Unit)
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(horizontal = 20.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 14.dp)
                .padding(bottom = bottomPadding),
            verticalArrangement = Arrangement.spacedBy(verticalSpacing)
        ) {
            header()
            content()
        }
    }
}

@Preview(name = "Feedback View, 1x font size", locale = "en")
@Preview(name = "Feedback View, 2x font size", locale = "en", fontScale = 2.0f)
@Preview(name = "Feedback View, RTL, 1x font size", locale = "he")
@Preview(name = "Feedback View, RTL, 2x font size", locale = "he", fontScale = 2.0f)
@Composable
fun FeedbackView_Preview() {
    NeevaTheme {
        FeedbackView(
            feedbackViewModel = getFakeFeedbackViewModel(),
            currentURL = MutableStateFlow(Uri.EMPTY)
        )
    }
}

@Preview(name = "Feedback View Dark, 1x font size", locale = "en")
@Preview(name = "Feedback View Dark, 2x font size", locale = "en", fontScale = 2.0f)
@Preview(name = "Feedback View Dark, RTL, 1x font size", locale = "he")
@Preview(name = "Feedback View Dark, RTL, 2x font size", locale = "he", fontScale = 2.0f)
@Composable
fun FeedbackView_Dark_Preview() {
    NeevaTheme(useDarkTheme = true) {
        FeedbackView(
            feedbackViewModel = getFakeFeedbackViewModel(),
            currentURL = MutableStateFlow(Uri.EMPTY)
        )
    }
}

package com.neeva.app.feedback

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.neeva.app.LocalEnvironment
import com.neeva.app.R
import com.neeva.app.ui.theme.FullScreenDialogTopBar
import com.neeva.app.ui.theme.NeevaTheme

@Composable
fun FeedbackView(
    feedbackViewModel: FeedbackViewModel
) {
    var text = remember { mutableStateOf("") }
    val shouldShareScreenshot = remember { mutableStateOf(true) }
    val shouldShareURL = remember { mutableStateOf(true) }
    val apolloWrapper = LocalEnvironment.current.apolloWrapper
    val dispatchers = LocalEnvironment.current.dispatchers

    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .fillMaxSize()
    ) {
        FullScreenDialogTopBar(
            title = stringResource(R.string.feedback),
            onBackPressed = { feedbackViewModel.onBackPressed() },
            buttonTitle = stringResource(R.string.submit_feedback),
            buttonPressed = {
                feedbackViewModel.onSubmitFeedbackPressed(
                    text.value,
                    shouldShareScreenshot.value,
                    shouldShareURL.value,
                    apolloWrapper = apolloWrapper,
                    dispatchers = dispatchers
                )
            }
        )

        Column(
            modifier = Modifier
                .padding(top = 20.dp)
                .scrollable(rememberScrollState(), orientation = Orientation.Vertical),
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                },
                onValueChange = { text.value = it },
                singleLine = false,
                colors = TextFieldDefaults.textFieldColors(
                    textColor = MaterialTheme.colorScheme.onBackground,
                    backgroundColor = MaterialTheme.colorScheme.background,
                    cursorColor = MaterialTheme.colorScheme.onBackground,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(10.dp)
            )

            ShareScreenshotView(shouldShareScreenshot)

            ShareURLView(
                feedbackViewModel.getCurrentURL(),
                shouldShareURL
            )
        }
    }
}

@Composable
private fun ShareScreenshotView(
    toggleState: MutableState<Boolean>
) {
    FeedbackOptionBox(
        toggleState = toggleState
    ) {
        Text(
            stringResource(
                R.string.submit_feedback_share_screenshot
            ),
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )

        val annotatedString = buildAnnotatedString {
            pushStringAnnotation(tag = "viewEditScreenshot", annotation = "")
            withStyle(style = SpanStyle(color = Color.Gray)) {
                append(stringResource(R.string.submit_feedback_view_edit_screenshot))
            }

            pop()
        }

        ClickableText(text = annotatedString, onClick = { offset ->
            annotatedString.getStringAnnotations(
                tag = "viewEditScreenshot",
                start = offset, end = offset
            ).firstOrNull()?.let {
                // Evan (TODO): Handle edit screenshot
            }
        })
    }
}

@Composable
private fun ShareURLView(
    url: String,
    toggleState: MutableState<Boolean>
) {
    FeedbackOptionBox(
        toggleState = toggleState
    ) {
        Text(
            stringResource(
                R.string.submit_feedback_view_share_url
            ),
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )

        val annotatedString = buildAnnotatedString {
            withStyle(style = SpanStyle(color = Color.Gray)) {
                append(url)
            }

            append("   ")

            pushStringAnnotation(tag = "viewEditScreenshot", annotation = "")
            withStyle(style = SpanStyle(color = Color.Gray)) {
                append(stringResource(R.string.edit_content_description))
            }

            pop()
        }

        ClickableText(text = annotatedString, onClick = { offset ->
            annotatedString.getStringAnnotations(
                tag = "viewEditScreenshot",
                start = offset,
                end = offset
            ).firstOrNull()?.let {
                // Evan (TODO): Handle edit URL
            }
        })
    }
}

@Composable
private fun FeedbackOptionBox(
    toggleState: MutableState<Boolean>,
    content: @Composable (() -> Unit)
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(horizontal = 20.dp)
            .background(
                color = MaterialTheme.colorScheme.background,
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(all = 14.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                content()
            }

            Switch(
                checked = toggleState.value,
                modifier = Modifier
                    .size(48.dp),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurface,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
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
}

@Preview(name = "Feedback View, 1x font size", locale = "en")
@Preview(name = "Feedback View, 2x font size", locale = "en", fontScale = 2.0f)
@Preview(name = "Feedback View, RTL, 1x font size", locale = "he")
@Preview(name = "Feedback View, RTL, 2x font size", locale = "he", fontScale = 2.0f)
@Composable
fun SettingsMain_Preview() {
    NeevaTheme {
        FeedbackView(
            feedbackViewModel = getFakeFeedbackViewModel()
        )
    }
}

@Preview(name = "Feedback View Dark, 1x font size", locale = "en")
@Preview(name = "Feedback View Dark, 2x font size", locale = "en", fontScale = 2.0f)
@Preview(name = "Feedback View Dark, RTL, 1x font size", locale = "he")
@Preview(name = "Feedback View Dark, RTL, 2x font size", locale = "he", fontScale = 2.0f)
@Composable
fun SettingsMain_Dark_Preview() {
    NeevaTheme(useDarkTheme = true) {
        FeedbackView(
            feedbackViewModel = getFakeFeedbackViewModel()
        )
    }
}

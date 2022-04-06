package com.neeva.app.firstrun.widgets.texts

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.ClickableText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.neeva.app.NeevaConstants.appPrivacyURL
import com.neeva.app.NeevaConstants.appTermsURL
import com.neeva.app.R
import com.neeva.app.firstrun.FirstRunConstants.getSubtextStyle
import com.neeva.app.ui.LightDarkPreviewContainer

@Composable
fun AcknowledgementText(openInCustomTabs: (Uri) -> Unit) {
    // TODO(kobec): how do i make this work for any language, because they can have different grammar...
    val annotatedString = buildAnnotatedString {
        withStyle(
            getSubtextStyle().toSpanStyle()
        ) {
            append(stringResource(R.string.acknowledge))
            append(" ")

            pushStringAnnotation(tag = "terms of service", annotation = appTermsURL)
            withStyle(style = SpanStyle(textDecoration = TextDecoration.Underline)) {
                append(stringResource(R.string.terms_of_service))
            }
            pop()

            append(" ")
            append(stringResource(R.string.and))
            append(" ")

            pushStringAnnotation(tag = "privacy policy", annotation = appPrivacyURL)
            withStyle(style = SpanStyle(textDecoration = TextDecoration.Underline)) {
                append(stringResource(R.string.privacy_policy))
            }
            pop()
        }
    }
    ClickableText(
        text = annotatedString,
        style = TextStyle(textAlign = TextAlign.Center, lineHeight = 20.sp),
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentSize(align = Alignment.Center),
        onClick = { offset ->
            annotatedString.getStringAnnotations(
                tag = "terms of service",
                start = offset,
                end = offset
            ).firstOrNull()?.let {
                openInCustomTabs(Uri.parse(it.item))
                return@ClickableText
            }

            annotatedString.getStringAnnotations(
                tag = "privacy policy",
                start = offset,
                end = offset
            ).firstOrNull()?.let {
                openInCustomTabs(Uri.parse(it.item))
                return@ClickableText
            }
        }
    )
}

@Preview("AcknowledgementText LTR 1x scale", locale = "en")
@Preview("AcknowledgementText LTR 2x scale", locale = "en", fontScale = 2.0f)
@Preview("AcknowledgementText RTL 1x scale", locale = "he")
@Composable
fun AcknowledgementTextPreview() {
    LightDarkPreviewContainer {
        AcknowledgementText { }
    }
}

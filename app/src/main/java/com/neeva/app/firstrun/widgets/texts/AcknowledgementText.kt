package com.neeva.app.firstrun.widgets.texts

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.neeva.app.NeevaConstants
import com.neeva.app.R
import com.neeva.app.firstrun.FirstRunConstants
import com.neeva.app.ui.LightDarkPreviewContainer
import com.neeva.app.ui.widgets.AnnotatedSpannable

@Composable
fun AcknowledgementText(
    appTermsURL: String,
    appPrivacyURL: String,
    openInCustomTabs: (Uri) -> Unit
) {
    AnnotatedSpannable(
        rawHtml = stringResource(R.string.acknowledge_agreement, appTermsURL, appPrivacyURL),
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentSize(align = Alignment.Center),
        textStyle = FirstRunConstants.getSubtextStyle().copy(
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
    ) { annotatedString, offset ->
        annotatedString.getStringAnnotations(
            tag = appTermsURL,
            start = offset,
            end = offset
        ).firstOrNull()?.let {
            openInCustomTabs(Uri.parse(it.item))
            return@AnnotatedSpannable
        }

        annotatedString.getStringAnnotations(
            tag = appPrivacyURL,
            start = offset,
            end = offset
        ).firstOrNull()?.let {
            openInCustomTabs(Uri.parse(it.item))
            return@AnnotatedSpannable
        }
    }
}

@Preview("AcknowledgementText LTR 1x scale", locale = "en")
@Preview("AcknowledgementText LTR 2x scale", locale = "en", fontScale = 2.0f)
@Preview("AcknowledgementText RTL 1x scale", locale = "he")
@Composable
fun AcknowledgementTextPreview() {
    LightDarkPreviewContainer {
        val neevaConstants = NeevaConstants()
        AcknowledgementText(
            appTermsURL = neevaConstants.appTermsURL,
            appPrivacyURL = neevaConstants.appPrivacyURL
        ) { }
    }
}

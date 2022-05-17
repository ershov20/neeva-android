package com.neeva.app.firstrun

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.neeva.app.R
import com.neeva.app.ui.OneBooleanPreviewContainer

@Composable
fun ToggleSignUpText(signup: Boolean, onClick: () -> Unit) {
    Text(
        style = FirstRunConstants.getSubtextStyle(),
        textAlign = TextAlign.Center,
        text = buildAnnotatedString {
            append(
                stringResource(
                    id = if (signup) {
                        R.string.already_have_account
                    } else {
                        R.string.dont_have_account
                    }
                )
            )

            append(" ")

            withStyle(style = SpanStyle(textDecoration = TextDecoration.Underline)) {
                append(
                    stringResource(
                        id = if (signup) {
                            R.string.sign_in
                        } else {
                            R.string.sign_up
                        }
                    )
                )
            }
        },
        modifier = Modifier
            .padding(vertical = 24.dp)
            .clickable { onClick() }
            .fillMaxWidth()
            .wrapContentSize(align = Alignment.Center)
    )
}

@Preview("ToggleSignUpText LTR 1x scale", locale = "en")
@Preview("ToggleSignUpText LTR 2x scale", locale = "en", fontScale = 2.0f)
@Preview("ToggleSignUpText RTL 1x scale", locale = "he")
@Composable
fun ToggleSignUpTextPreview() {
    OneBooleanPreviewContainer { signup ->
        ToggleSignUpText(signup) { }
    }
}

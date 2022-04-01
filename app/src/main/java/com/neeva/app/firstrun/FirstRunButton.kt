package com.neeva.app.firstrun

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.neeva.app.R
import com.neeva.app.ui.OneBooleanPreviewContainer
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.ui.theme.Roobert

/** Non-standard button used for First Run using Neeva's Roobert font. */
@Composable
fun FirstRunButton(
    enabled: Boolean,
    stringResID: Int,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .padding(horizontal = Dimensions.PADDING_LARGE, vertical = Dimensions.PADDING_SMALL)
            .fillMaxWidth()
    ) {
        Text(
            text = stringResource(id = stringResID),
            style = TextStyle(
                fontFamily = Roobert,
                fontWeight = FontWeight.W400,
                fontSize = 16.sp,
                lineHeight = 22.sp
            ),
            textAlign = TextAlign.Center
        )
    }
}

@Preview("LTR 1x scale", locale = "en")
@Preview("LTR 2x scale", locale = "en", fontScale = 2.0f)
@Preview("RTL 1x scale", locale = "he")
@Composable
fun FirstRunButtonPreview() {
    OneBooleanPreviewContainer { isEnabled ->
        FirstRunButton(enabled = isEnabled, stringResID = R.string.sign_in_with_google) {}
    }
}

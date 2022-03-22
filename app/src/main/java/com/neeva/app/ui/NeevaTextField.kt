package com.neeva.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.neeva.app.R
import com.neeva.app.ui.theme.Dimensions

/**
 * UI widget that can show a placeholder and an editable text field.
 *
 * Meant to be used until Material3 actually releases an editable text field.  This implementation
 * takes the colors that are set for Material3 and passes them along into the Material1 TextField.
 */
@Composable
fun NeevaTextField(
    text: String,
    onTextChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholderText: String? = null,
    tonalElevation: Dp = 1.dp,
    singleLine: Boolean = false,
    minLines: Int = 1,
    maxLines: Int = Int.MAX_VALUE,
) {
    Surface(
        shape = RoundedCornerShape(Dimensions.RADIUS_LARGE),
        tonalElevation = tonalElevation,
        modifier = modifier
    ) {
        // Ensure that the minimum size of the text boxes fits
        val lineHeight = with(LocalDensity.current) {
            LocalTextStyle.current.lineHeight.toDp()
        }

        val textModifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = lineHeight * minLines)

        Box(modifier = Modifier.padding(Dimensions.PADDING_LARGE)) {
            if (text.isEmpty() && placeholderText != null) {
                Text(
                    text = placeholderText,
                    style = LocalTextStyle.current.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = textModifier
                )
            }

            BasicTextField(
                value = text,
                onValueChange = onTextChanged,
                singleLine = singleLine,
                maxLines = maxLines,
                textStyle = LocalTextStyle.current.copy(
                    color = LocalContentColor.current
                ),
                cursorBrush = SolidColor(LocalContentColor.current),
                modifier = textModifier
            )
        }
    }
}

@Preview("Placeholders LTR 1x font scale", locale = "en")
@Preview("Placeholders LTR 2x font scale", locale = "en", fontScale = 2.0f)
@Preview("Placeholders RTL 1x font scale", locale = "he")
@Composable
private fun NeevaTextFieldPreview_Placeholder() {
    OneBooleanPreviewContainer { showPlaceholder ->
        val text = remember {
            if (showPlaceholder) {
                mutableStateOf("")
            } else {
                mutableStateOf("Real text that hides the placeholder")
            }
        }
        NeevaTextField(
            text = text.value,
            onTextChanged = { text.value = it },
            placeholderText = stringResource(R.string.debug_long_string_primary),
            minLines = 3
        )
    }
}

@Preview("Single line LTR 1x font scale", locale = "en")
@Preview("Single line LTR 2x font scale", locale = "en", fontScale = 2.0f)
@Preview("Single line RTL 1x font scale", locale = "he")
@Composable
private fun NeevaTextFieldPreview_SingleLine() {
    LightDarkPreviewContainer {
        val text = remember { mutableStateOf("This is just some random text") }
        NeevaTextField(
            text = text.value,
            onTextChanged = { text.value = it },
            singleLine = true
        )
    }
}

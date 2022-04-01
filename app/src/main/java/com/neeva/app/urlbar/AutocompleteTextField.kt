package com.neeva.app.urlbar

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.neeva.app.R
import com.neeva.app.ui.LightDarkPreviewContainer
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.ui.widgets.FaviconView

@Composable
fun AutocompleteTextField(
    textFieldValue: TextFieldValue,
    faviconBitmap: Bitmap?,
    onLocationEdited: (TextFieldValue) -> Unit,
    onLocationReplaced: (String) -> Unit,
    onLoadUrl: () -> Unit,
    placeholderColor: Color,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(start = Dimensions.PADDING_MEDIUM)
    ) {
        FaviconView(
            bitmap = faviconBitmap,
            drawContainer = false
        )

        Spacer(Modifier.width(Dimensions.PADDING_SMALL))

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.weight(1.0f)
        ) {
            // [BasicTextField]s don't have support for Placeholders, and [TextField] isn't
            // styled in a way we can use.  Instead, just add a Text that disappears as soon as
            // the user starts typing something.
            if (textFieldValue.text.isEmpty()) {
                Text(
                    text = stringResource(R.string.url_bar_placeholder),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = placeholderColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            BasicTextField(
                value = textFieldValue,
                onValueChange = onLocationEdited,
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .onPreviewKeyEvent {
                        if (it.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_ENTER) {
                            // If we're seeing a hardware enter key, intercept it to prevent
                            // adding a newline to the URL.
                            onLoadUrl()
                            true
                        } else {
                            false
                        }
                    }
                    .fillMaxWidth(),
                singleLine = true,
                textStyle = TextStyle(
                    color = LocalContentColor.current,
                    fontSize = MaterialTheme.typography.bodyLarge.fontSize
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Go,
                    autoCorrect = false
                ),
                keyboardActions = KeyboardActions(
                    onGo = { onLoadUrl() }
                ),
                cursorBrush = SolidColor(LocalContentColor.current)
            )
        }

        AnimatedVisibility(
            visible = textFieldValue.text.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            IconButton(
                onClick = { onLocationReplaced("") }
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = stringResource(id = R.string.clear),
                    tint = LocalContentColor.current
                )
            }
        }
    }

    LaunchedEffect(true) {
        focusRequester.requestFocus()
    }
}

@Preview("Autocompleted text, LTR, 1x scale", locale = "en")
@Preview("Autocompleted text, LTR, 2x scale", locale = "en", fontScale = 2.0f)
@Preview("Autocompleted text, RTL, 1x scale", locale = "he")
@Preview("Autocompleted text, RTL, 2x scale", locale = "he", fontScale = 2.0f)
@Composable
private fun AutocompleteTextFieldPreview_AutocompletedText() {
    LightDarkPreviewContainer {
        val text = stringResource(id = R.string.debug_long_string_primary)
        val textFieldValue = TextFieldValue(
            text = text,
            selection = TextRange(7, text.length)
        )

        Surface {
            AutocompleteTextField(
                textFieldValue = textFieldValue,
                faviconBitmap = null,
                onLocationEdited = {},
                onLocationReplaced = {},
                onLoadUrl = {},
                placeholderColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview("Placeholder, LTR, 1x scale", locale = "en")
@Preview("Placeholder, LTR, 2x scale", locale = "en", fontScale = 2.0f)
@Preview("Placeholder, RTL, 1x scale", locale = "he")
@Preview("Placeholder, RTL, 2x scale", locale = "he", fontScale = 2.0f)
@Composable
private fun AutocompleteTextFieldPreview_Placeholder() {
    LightDarkPreviewContainer {
        val textFieldValue = TextFieldValue()
        Surface {
            AutocompleteTextField(
                textFieldValue = textFieldValue,
                faviconBitmap = null,
                onLocationEdited = {},
                onLocationReplaced = {},
                onLoadUrl = {},
                placeholderColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview("No autocomplete, LTR, 1x scale", locale = "en")
@Preview("No autocomplete, LTR, 2x scale", locale = "en", fontScale = 2.0f)
@Preview("No autocomplete, RTL, 1x scale", locale = "he")
@Preview("No autocomplete, RTL, 2x scale", locale = "he", fontScale = 2.0f)
@Composable
private fun AutocompleteTextFieldPreview_NoAutocomplete() {
    LightDarkPreviewContainer {
        val text = "something else"
        val textFieldValue = TextFieldValue(
            text = text,
            selection = TextRange(text.length)
        )

        Surface {
            AutocompleteTextField(
                textFieldValue = textFieldValue,
                faviconBitmap = null,
                onLocationEdited = {},
                onLocationReplaced = {},
                onLoadUrl = {},
                placeholderColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

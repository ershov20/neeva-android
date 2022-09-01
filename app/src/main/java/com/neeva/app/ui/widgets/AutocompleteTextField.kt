// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.ui.widgets

import android.graphics.Bitmap
import android.view.KeyEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import com.neeva.app.R
import com.neeva.app.ui.KeyboardFocusEffect
import com.neeva.app.ui.LightDarkPreviewContainer
import com.neeva.app.ui.theme.Dimensions

private class AutocompleteOffsetMapping(private val originalText: AnnotatedString) : OffsetMapping {
    override fun originalToTransformed(offset: Int): Int {
        return offset
    }

    override fun transformedToOriginal(offset: Int): Int {
        // The AutocompleteTextField only adds suffixes.  Make it so that any offset after the
        // length of the user's text points back at the end of the user's text.
        return offset.coerceAtMost(originalText.length)
    }
}

@Composable
fun AutocompleteTextField(
    textFieldValue: TextFieldValue,
    suggestionText: String? = null,
    imageVector: ImageVector? = null,
    faviconBitmap: Bitmap? = null,
    onTextEdited: (TextFieldValue) -> Unit,
    onTextCleared: () -> Unit,
    onSubmitted: () -> Unit,
    onAcceptSuggestion: () -> Unit = {},
    placeholderColor: Color,
    focusImmediately: Boolean = true,
    placeholderText: String = stringResource(R.string.url_bar_placeholder),
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Spacer(Modifier.width(Dimensions.PADDING_MEDIUM))

        when {
            imageVector != null -> {
                Icon(
                    imageVector = imageVector,
                    contentDescription = null
                )
            }

            else -> {
                FaviconView(
                    bitmap = faviconBitmap,
                    drawContainer = false
                )
            }
        }

        Spacer(Modifier.width(Dimensions.PADDING_SMALL))

        val selectionBackground = LocalTextSelectionColors.current.backgroundColor

        // Append additional text after what the user _actually_ typed in.
        val visualTransformation = VisualTransformation { text ->
            val builder = AnnotatedString.Builder(text)

            when {
                text.isEmpty() -> {
                    // Show placeholder text if the user hasn't typed anything in.
                    builder.append(
                        AnnotatedString(
                            text = placeholderText,
                            spanStyle = SpanStyle(color = placeholderColor)
                        )
                    )
                }

                suggestionText != null -> {
                    // Add the autocomplete suggestion as a highlighted suffix to the user's text.
                    builder.append(
                        AnnotatedString(
                            text = suggestionText,
                            spanStyle = SpanStyle(background = selectionBackground)
                        )
                    )
                }
            }

            TransformedText(
                text = builder.toAnnotatedString(),
                offsetMapping = AutocompleteOffsetMapping(text)
            )
        }

        Box(
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier
                .weight(1f)
                .height(IntrinsicSize.Min)
        ) {
            val focusRequester = remember { FocusRequester() }
            KeyboardFocusEffect(focusRequester.takeIf { focusImmediately })

            // [BasicTextField]s don't have support for Material 3 colors, so we have to manually
            // provide them.
            BasicTextField(
                value = textFieldValue,
                onValueChange = onTextEdited,
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .onPreviewKeyEvent {
                        // If we're seeing a hardware enter key, intercept it to prevent adding a
                        // newline to the URL.
                        if (it.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_ENTER) {
                            // Compose will _sometimes_ trigger this code on both a keydown and
                            // keyup event.  To avoid creating two tabs, explicitly look for the
                            // keydown event and ignore the keyup.
                            if (it.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                                onSubmitted()
                            }
                            true
                        } else {
                            false
                        }
                    }
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = placeholderText
                        testTag = "AutocompleteTextField"
                    },
                singleLine = true,
                visualTransformation = visualTransformation,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = LocalContentColor.current
                ),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    keyboardType = if (textFieldValue.text.contains(' ')) {
                        KeyboardType.Text
                    } else {
                        KeyboardType.Uri
                    },
                    imeAction = ImeAction.Go,
                    autoCorrect = false
                ),
                keyboardActions = KeyboardActions(
                    onGo = { onSubmitted() }
                ),
                cursorBrush = SolidColor(LocalContentColor.current)
            )

            // Add an invisible tap target across the whole box that allows the user to accept the
            // autocomplete suggestion.
            if (!suggestionText.isNullOrEmpty()) {
                val completedSuggestion = textFieldValue.text + suggestionText
                Button(
                    onClick = onAcceptSuggestion,
                    modifier = Modifier.fillMaxSize().alpha(0f)
                ) {
                    Text(
                        stringResource(
                            R.string.url_bar_accept_autocomplete,
                            completedSuggestion
                        )
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = textFieldValue.text.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            IconButton(
                onClick = { onTextCleared() }
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = stringResource(id = R.string.clear),
                    tint = LocalContentColor.current
                )
            }
        }
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
        val userTypedLength = 7
        Surface {
            AutocompleteTextField(
                textFieldValue = TextFieldValue(
                    text.take(userTypedLength),
                    selection = TextRange(userTypedLength)
                ),
                suggestionText = text.drop(userTypedLength),
                faviconBitmap = null,
                onTextEdited = {},
                onTextCleared = {},
                onSubmitted = {},
                onAcceptSuggestion = {},
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
        Surface {
            AutocompleteTextField(
                textFieldValue = TextFieldValue(),
                suggestionText = null,
                faviconBitmap = null,
                onTextEdited = {},
                onTextCleared = {},
                onSubmitted = {},
                onAcceptSuggestion = {},
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

        Surface {
            AutocompleteTextField(
                textFieldValue = TextFieldValue(text),
                suggestionText = null,
                faviconBitmap = null,
                onTextEdited = {},
                onTextCleared = {},
                onSubmitted = {},
                onAcceptSuggestion = {},
                placeholderColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

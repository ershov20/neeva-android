package com.neeva.app.urlbar

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.neeva.app.LocalBrowserWrapper
import com.neeva.app.R
import com.neeva.app.browsing.BrowserWrapper
import com.neeva.app.suggestions.SuggestionsModel
import com.neeva.app.ui.BooleanPreviewParameterProvider
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.ui.theme.NeevaTheme
import com.neeva.app.widgets.FaviconView

@Composable
fun AutocompleteTextField(
    urlBarModel: URLBarModel,
    suggestionsModel: SuggestionsModel?,
    urlBarModelState: URLBarModelState,
    foregroundColor: Color,
    placeholderColor: Color,
    modifier: Modifier
) {
    val browserWrapper: BrowserWrapper = LocalBrowserWrapper.current

    val focusRequester = remember { FocusRequester() }
    urlBarModel.focusRequester = focusRequester

    val focusManager = LocalFocusManager.current

    AutocompleteTextField(
        textFieldValue = urlBarModelState.textFieldValue,
        faviconBitmap = urlBarModelState.faviconBitmap,
        focusRequester = focusRequester,
        onLocationEdited = { urlBarModel.onLocationBarTextChanged(it) },
        onLocationReplaced = { urlBarModel.replaceLocationBarText(it) },
        onFocusChanged = { urlBarModel.onFocusChanged(it.isFocused) },
        onLoadUrl = {
            browserWrapper.loadUrl(urlBarModelState.uriToLoad)
            focusManager.clearFocus()
            suggestionsModel?.logSuggestionTap(urlBarModelState.getSuggestionType(), null)
        },
        foregroundColor = foregroundColor,
        placeholderColor = placeholderColor,
        modifier = modifier
    )
}

@Composable
fun AutocompleteTextField(
    textFieldValue: TextFieldValue,
    faviconBitmap: Bitmap?,
    focusRequester: FocusRequester,
    onLocationEdited: (TextFieldValue) -> Unit,
    onLocationReplaced: (String) -> Unit,
    onFocusChanged: (FocusState) -> Unit,
    onLoadUrl: () -> Unit,
    foregroundColor: Color,
    placeholderColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        CompositionLocalProvider(LocalContentColor provides foregroundColor) {
            FaviconView(
                bitmap = faviconBitmap,
                bordered = false
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
                        color = placeholderColor
                    )
                }

                BasicTextField(
                    value = textFieldValue,
                    onValueChange = onLocationEdited,
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .onFocusChanged(onFocusChanged)
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
                        keyboardType = KeyboardType.Uri,
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
    }
}

class AutocompleteTextFieldPreviews :
    BooleanPreviewParameterProvider<AutocompleteTextFieldPreviews.Params>(3) {
    data class Params(
        val darkTheme: Boolean,
        val useLongText: Boolean,
        val showPlaceholder: Boolean
    )

    override fun createParams(booleanArray: BooleanArray): Params {
        return Params(
            darkTheme = booleanArray[0],
            useLongText = booleanArray[1],
            showPlaceholder = booleanArray[2]
        )
    }

    @Preview("1x scale", locale = "en")
    @Preview("2x scale", locale = "en", fontScale = 2.0f)
    @Preview("RTL, 1x scale", locale = "he")
    @Preview("RTL, 2x scale", locale = "he", fontScale = 2.0f)
    @Composable
    fun Default(@PreviewParameter(AutocompleteTextFieldPreviews::class) params: Params) {
        val textFieldValue = when {
            params.showPlaceholder -> {
                TextFieldValue()
            }

            params.useLongText -> {
                val text = stringResource(id = R.string.debug_long_string_primary)
                TextFieldValue(
                    text = text,
                    selection = TextRange(7, text.length)
                )
            }

            else -> {
                val text = "something else"
                TextFieldValue(
                    text = text,
                    selection = TextRange(text.length)
                )
            }
        }

        NeevaTheme(useDarkTheme = params.darkTheme) {
            AutocompleteTextField(
                textFieldValue = textFieldValue,
                faviconBitmap = null,
                focusRequester = FocusRequester(),
                onLocationEdited = {},
                onLocationReplaced = {},
                onFocusChanged = {},
                onLoadUrl = {},
                foregroundColor = MaterialTheme.colorScheme.onSurface,
                placeholderColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

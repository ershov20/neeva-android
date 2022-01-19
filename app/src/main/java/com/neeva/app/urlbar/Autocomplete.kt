package com.neeva.app.urlbar

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.neeva.app.R
import com.neeva.app.storage.FaviconCache
import com.neeva.app.ui.theme.NeevaTheme
import com.neeva.app.widgets.FaviconView

@Composable
fun AutocompleteTextField(
    urlBarModel: URLBarModel,
    textFieldValue: TextFieldValue,
    textFieldValueMutator: (TextFieldValue) -> Unit,
    faviconCache: FaviconCache,
    backgroundColor: Color,
    foregroundColor: Color
) {
    val focusRequester = remember { FocusRequester() }
    urlBarModel.focusRequester = focusRequester

    val url = URLBarModel.getUrlToLoad(textFieldValue.text)
    val faviconBitmap: Bitmap? by faviconCache.getFaviconAsync(url, generateFavicon = false)

    AutocompleteTextField(
        textFieldValue = textFieldValue,
        faviconBitmap = faviconBitmap,
        onLocationEdited = { newValue ->
            textFieldValueMutator(newValue)
            urlBarModel.onLocationBarTextChanged(newValue.text)
        },
        onLocationReplaced = {
            textFieldValueMutator(
                TextFieldValue(
                    text = it,
                    selection = TextRange(it.length)
                )
            )
            urlBarModel.replaceLocationBarText(it)
        },
        focusRequester = focusRequester,
        onFocusChanged = { urlBarModel.onFocusChanged(it.isFocused) },
        onLoadUrl = { urlBarModel.loadUrl(url) },
        backgroundColor = backgroundColor,
        foregroundColor = foregroundColor
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
    backgroundColor: Color,
    foregroundColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .defaultMinSize(minHeight = 40.dp)
            .fillMaxWidth()
            .background(backgroundColor)
    ) {
        FaviconView(
            bitmap = faviconBitmap,
            bordered = false,
            modifier = Modifier.padding(start = 8.dp)
        )

        BasicTextField(
            value = textFieldValue,
            onValueChange = onLocationEdited,
            modifier = Modifier
                .weight(1.0f)
                .padding(start = 8.dp)
                .focusRequester(focusRequester)
                .onFocusChanged(onFocusChanged)
                .onPreviewKeyEvent {
                    if (it.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_ENTER) {
                        // If we're seeing a hardware enter key, intercept it to prevent adding
                        // a newline to the URL.
                        onLoadUrl.invoke()
                        true
                    } else {
                        false
                    }
                },
            singleLine = true,
            textStyle = TextStyle(
                color = foregroundColor,
                fontSize = MaterialTheme.typography.bodyLarge.fontSize
            ),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Go
            ),
            keyboardActions = KeyboardActions(
                onGo = { onLoadUrl.invoke() }
            )
        )

        IconButton(
            onClick = { onLocationReplaced("") },
            modifier = Modifier
                .size(40.dp)
                .padding(8.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_baseline_cancel_24),
                contentDescription = stringResource(id = R.string.clear),
                colorFilter = ColorFilter.tint(foregroundColor)
            )
        }
    }
}

@Preview("Default, 1x scale", locale = "en")
@Preview("Default, 2x scale", locale = "en", fontScale = 2.0f)
@Preview("Default, RTL, 1x scale", locale = "he")
@Preview("Default, RTL, 2x scale", locale = "he", fontScale = 2.0f)
@Composable
fun AutocompleteTextField_Preview() {
    NeevaTheme {
        val text = stringResource(id = R.string.debug_long_string_primary)
        AutocompleteTextField(
            textFieldValue = TextFieldValue(
                text = text,
                selection = TextRange(7, text.length)
            ),
            faviconBitmap = null,
            focusRequester = FocusRequester(),
            onLocationEdited = {},
            onLocationReplaced = {},
            onFocusChanged = {},
            onLoadUrl = {},
            backgroundColor = MaterialTheme.colorScheme.background,
            foregroundColor = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Preview("Not editing, 1x scale", locale = "en")
@Preview("Not editing, 2x scale", locale = "en", fontScale = 2.0f)
@Composable
fun AutocompleteTextField_PreviewNoSuggestion() {
    NeevaTheme {
        val text = "something else"
        AutocompleteTextField(
            textFieldValue = TextFieldValue(
                text = text,
                selection = TextRange(text.length)
            ),
            faviconBitmap = null,
            focusRequester = FocusRequester(),
            onLocationEdited = {},
            onLocationReplaced = {},
            onFocusChanged = {},
            onLoadUrl = {},
            backgroundColor = MaterialTheme.colorScheme.background,
            foregroundColor = MaterialTheme.colorScheme.onSurface
        )
    }
}

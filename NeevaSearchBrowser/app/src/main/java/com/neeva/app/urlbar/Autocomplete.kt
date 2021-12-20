package com.neeva.app.urlbar

import android.graphics.Bitmap
import android.net.Uri
import android.util.Patterns
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.neeva.app.R
import com.neeva.app.appURL
import com.neeva.app.browsing.toSearchUri
import com.neeva.app.suggestions.NavSuggestion
import com.neeva.app.ui.theme.NeevaTheme
import com.neeva.app.widgets.FaviconView

@Composable
fun AutocompleteTextField(
    urlBarModel: URLBarModel, getFaviconFor: (Uri) -> LiveData<Bitmap?>
) {
    val autocompletedSuggestion by urlBarModel.autocompletedSuggestion.observeAsState(null)
    val value: TextFieldValue by urlBarModel.text.observeAsState(TextFieldValue("", TextRange.Zero))
    var lastEditWasDeletion by remember { mutableStateOf(false) }

    val showingAutocomplete: Boolean by urlBarModel.text.map {
        val autocompleteText =
            urlBarModel.autocompletedSuggestion.value?.secondaryLabel ?: return@map false
        it.text.isNotEmpty()
                && autocompleteText.isNotEmpty()
                && autocompleteText.startsWith(it.text)
                && !lastEditWasDeletion
                && autocompletedSuggestion!!.secondaryLabel.length != value.text.length
    }.observeAsState(false)

    val url = autocompletedSuggestion?.url ?: Uri.parse(value.text) ?: Uri.parse(appURL)
    val bitmap: Bitmap? by getFaviconFor(url).observeAsState()

    AutocompleteTextField(
        autocompletedSuggestion = autocompletedSuggestion?.secondaryLabel?.takeIf { showingAutocomplete },
        value = value,
        bitmap = bitmap,
        onLocationEdited = { textFieldValue ->
            lastEditWasDeletion = textFieldValue.text.length < value.text.length
            urlBarModel.onLocationBarTextChanged(textFieldValue)
        },
        onLocationReplaced = { textFieldValue ->
            urlBarModel.onLocationBarTextChanged(textFieldValue)
        },
        focusRequester = urlBarModel.focusRequester,
        onFocusChanged = urlBarModel::onFocusChanged,
        onLoadUrl = { urlBarModel.loadUrl(getUrlToLoad(autocompletedSuggestion, value.text)) }
    )
}

@Composable
fun AutocompleteTextField(
    autocompletedSuggestion: String?,
    value: TextFieldValue,
    bitmap: Bitmap?,
    focusRequester: FocusRequester,
    onLocationEdited: (TextFieldValue) -> Unit,
    onLocationReplaced: (TextFieldValue) -> Unit,
    onFocusChanged: (FocusState) -> Unit,
    onLoadUrl: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .defaultMinSize(minHeight = 40.dp)
            .fillMaxWidth()
            .clickable {
                val newValue = autocompletedSuggestion ?: value.text
                onLocationReplaced.invoke(
                    TextFieldValue(
                        newValue,
                        TextRange(newValue.length, newValue.length),
                        TextRange(newValue.length, newValue.length)
                    )
                )
            }
    ) {
        FaviconView(bitmap = bitmap, bordered = false)

        // TODO(dan.alcantara): If you have a really long autocomplete suggestion, this layout
        //                      breaks because it isn't scrollable.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1.0f)
        ) {
            BasicTextField(
                value,
                onValueChange = onLocationEdited,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .width(IntrinsicSize.Min)
                    .adjustIntrinsicWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged(onFocusChanged)
                    .onPreviewKeyEvent {
                        if (it.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_ENTER) {
                            // If we're seeing a hardware enter key, intercept it to prevent adding a newline to the URL.
                            onLoadUrl.invoke()
                            true
                        } else {
                            false
                        }
                    },
                singleLine = true,
                textStyle = TextStyle(
                    color = if (value.text.isEmpty()) {
                        MaterialTheme.colors.onSecondary
                    } else {
                        MaterialTheme.colors.onPrimary
                    },
                    fontSize = MaterialTheme.typography.body1.fontSize
                ),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Go
                ),
                keyboardActions = KeyboardActions(
                    onGo = { onLoadUrl.invoke() }
                ),
                cursorBrush = SolidColor(
                    if (autocompletedSuggestion != null) {
                        // Hide the cursor while the autocomplete value is being shown.
                        Color.Unspecified
                    } else {
                        // TODO(dan.alcantara): This is not the right value to use in a dark theme.
                        Color.Black
                    }
                ),
            )

            autocompletedSuggestion?.substring(value.text.length)?.let { suggestion ->
                Text(
                    text = suggestion,
                    modifier = Modifier.background(Color(R.color.selection_highlight)),
                    style = MaterialTheme.typography.body1,
                    softWrap = false,
                    maxLines = 1,
                    color = MaterialTheme.colors.onPrimary,
                    textAlign = TextAlign.Start
                )
            }

            Spacer(modifier = Modifier.weight(1.0f))
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clickable { onLocationReplaced.invoke(TextFieldValue("")) }
                .padding(8.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_baseline_cancel_24),
                contentDescription = stringResource(id = R.string.cancel),
                colorFilter = ColorFilter.tint(MaterialTheme.colors.onSecondary)
            )
        }
    }
}

@Preview("Default, 1x scale")
@Preview("Default, 2x scale", fontScale = 2.0f)
@Composable
fun AutocompleteTextField_Preview() {
    NeevaTheme {
        AutocompleteTextField(
            autocompletedSuggestion = "something else comes after this",
            value = TextFieldValue(text = "something else"),
            bitmap = null,
            focusRequester = FocusRequester(),
            onLocationEdited = {},
            onLocationReplaced = {},
            onFocusChanged = {},
            onLoadUrl = {}
        )
    }
}

@Preview("Not editing, 1x scale")
@Preview("Not editing, 2x scale", fontScale = 2.0f)
@Composable
fun AutocompleteTextField_PreviewNoSuggestion() {
    NeevaTheme {
        AutocompleteTextField(
            autocompletedSuggestion = null,
            value = TextFieldValue(text = "something else"),
            bitmap = null,
            focusRequester = FocusRequester(),
            onLocationEdited = {},
            onLocationReplaced = {},
            onFocusChanged = {},
            onLoadUrl = {}
        )
    }
}

internal fun getUrlToLoad(autocompletedSuggestion: NavSuggestion?, urlBarContents: String): Uri = when {
    autocompletedSuggestion?.url != null -> {
        autocompletedSuggestion.url
    }

    // Try to figure out if the user typed in a query or a URL.
    // TODO(dan.alcantara): This won't always work, especially if the site doesn't have
    //                      an https equivalent.  We should either figure out something
    //                      more robust or do what iOS does (for consistency).
    Patterns.WEB_URL.matcher(urlBarContents).matches() -> {
        var uri = Uri.parse(urlBarContents)
        if (uri.scheme.isNullOrEmpty()) {
            uri = uri.buildUpon().scheme("https").build()
        }
        uri
    }

    else -> {
        urlBarContents.toSearchUri()
    }
}

// Workaround for Compose bug around intrinsic min width being off by 1dp.
fun Modifier.adjustIntrinsicWidth() = this.then(object : LayoutModifier {
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        val width = constraints.constrainWidth(placeable.width + 1.dp.roundToPx())
        val height = constraints.constrainHeight(placeable.height)
        return layout(width, height) {
            placeable.place(1.dp.roundToPx(), 0)
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int
    ): Int =
        measurable.minIntrinsicWidth(height) + 1.dp.roundToPx()
})
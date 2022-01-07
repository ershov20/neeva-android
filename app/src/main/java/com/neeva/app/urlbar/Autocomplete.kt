package com.neeva.app.urlbar

import android.net.Uri
import android.util.Patterns
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import com.neeva.app.NeevaConstants.appURL
import com.neeva.app.R
import com.neeva.app.browsing.toSearchUri
import com.neeva.app.storage.Favicon
import com.neeva.app.suggestions.NavSuggestion
import com.neeva.app.suggestions.SuggestionsModel
import com.neeva.app.ui.theme.NeevaTheme
import com.neeva.app.ui.theme.SelectionHighlight
import com.neeva.app.widgets.FaviconView
import kotlinx.coroutines.flow.Flow

@Composable
fun AutocompleteTextField(
    suggestionsModel: SuggestionsModel,
    urlBarModel: URLBarModel,
    getFaviconFlow: (Uri) -> Flow<Favicon?>,
    urlBarIsBeingEdited: Boolean
) {
    val autocompletedSuggestion by suggestionsModel.autocompleteSuggestion.collectAsState()
    val urlBarText: TextFieldValue by urlBarModel.userInputText.collectAsState()

    var lastEditWasDeletion by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    urlBarModel.focusRequester = focusRequester

    // Don't bother calculating what the suggestion should be if the bar isn't visible.
    val autocompleteText = if (urlBarIsBeingEdited) {
        getAutocompleteText(autocompletedSuggestion, urlBarText.text)
    } else {
        null
    }
    val autocompleteIsValid: Boolean =
        urlBarText.text.isNotBlank() &&
            !autocompleteText.isNullOrBlank() &&
            !lastEditWasDeletion &&
            autocompleteText.length > urlBarText.text.length

    val url = autocompletedSuggestion?.takeIf { autocompleteIsValid }?.url
        ?: Uri.parse(urlBarText.text)
        ?: Uri.parse(appURL)
    val favicon: Favicon? by getFaviconFlow(url).collectAsState(null)

    AutocompleteTextField(
        autocompletedSuggestion = autocompleteText.takeIf { autocompleteIsValid },
        value = urlBarText,
        favicon = favicon,
        onLocationEdited = { textFieldValue ->
            lastEditWasDeletion = textFieldValue.text.length < urlBarText.text.length
            urlBarModel.onLocationBarTextChanged(textFieldValue)
        },
        onLocationReplaced = { urlBarModel.replaceLocationBarText(it) },
        focusRequester = focusRequester,
        onFocusChanged = { urlBarModel.onFocusChanged(it.isFocused) },
        onLoadUrl = {
            urlBarModel.loadUrl(
                getUrlToLoad(
                    autocompletedSuggestion.takeIf { autocompleteIsValid },
                    urlBarText.text
                )
            )
        }
    )
}

@Composable
fun AutocompleteTextField(
    autocompletedSuggestion: String?,
    value: TextFieldValue,
    favicon: Favicon?,
    focusRequester: FocusRequester,
    onLocationEdited: (TextFieldValue) -> Unit,
    onLocationReplaced: (String) -> Unit,
    onFocusChanged: (FocusState) -> Unit,
    onLoadUrl: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .defaultMinSize(minHeight = 40.dp)
            .fillMaxWidth()
            .then(
                // If the user clicks on the URL bar while a suggestion is displayed, make it the
                // new current query.
                autocompletedSuggestion?.let {
                    Modifier.clickable { onLocationReplaced(it) }
                } ?: Modifier
            )
    ) {
        FaviconView(favicon = favicon, bordered = false)

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
                    modifier = Modifier.background(SelectionHighlight),
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
                .clickable { onLocationReplaced("") }
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

@Preview("Default, 1x scale", locale = "en")
@Preview("Default, 2x scale", locale = "en", fontScale = 2.0f)
@Composable
fun AutocompleteTextField_Preview() {
    NeevaTheme {
        AutocompleteTextField(
            autocompletedSuggestion = "something else comes after this",
            value = TextFieldValue(text = "something else"),
            favicon = null,
            focusRequester = FocusRequester(),
            onLocationEdited = {},
            onLocationReplaced = {},
            onFocusChanged = {},
            onLoadUrl = {}
        )
    }
}

@Preview("Default, RTL, 1x scale", locale = "he")
@Preview("Default, RTL, 2x scale", locale = "he", fontScale = 2.0f)
@Composable
fun AutocompleteTextField_PreviewHebrew() {
    NeevaTheme {
        AutocompleteTextField(
            autocompletedSuggestion = "עשרה שתים עשרה שלוש עשרה ארבע עשרה חמש עשרה",
            value = TextFieldValue(text = "חמש עשרה"),
            favicon = null,
            focusRequester = FocusRequester(),
            onLocationEdited = {},
            onLocationReplaced = {},
            onFocusChanged = {},
            onLoadUrl = {}
        )
    }
}

@Preview("Not editing, 1x scale", locale = "en")
@Preview("Not editing, 2x scale", locale = "en", fontScale = 2.0f)
@Composable
fun AutocompleteTextField_PreviewNoSuggestion() {
    NeevaTheme {
        AutocompleteTextField(
            autocompletedSuggestion = null,
            value = TextFieldValue(text = "something else"),
            favicon = null,
            focusRequester = FocusRequester(),
            onLocationEdited = {},
            onLocationReplaced = {},
            onFocusChanged = {},
            onLoadUrl = {}
        )
    }
}

/**
 * Determines if what is in the URL bar can in any way be related to the autocomplete suggestion.
 *
 * This applies a bunch of hand wavy heuristics, including chopping off "https://www" and checking
 * if there's a straight match.
 */
internal fun getAutocompleteText(
    autocompletedSuggestion: NavSuggestion?,
    urlBarContents: String
): String? {
    if (urlBarContents.isBlank()) return null

    // Check if we have a direct match.
    val autocompleteText =
        autocompletedSuggestion?.secondaryLabel?.takeIf { it.isNotBlank() } ?: return null
    if (autocompleteText.startsWith(urlBarContents)) {
        return autocompleteText
    }

    // Check if the URL could possibly start with what has already been typed in.
    val autocompleteUri = autocompletedSuggestion.url.toString()

    val withoutScheme = autocompleteUri
        .takeIf { it.startsWith("https://") }
        ?.replaceFirst("https://", "")
        ?.takeIf { it.startsWith(urlBarContents) }
    if (withoutScheme != null) return withoutScheme

    val withoutWww = autocompleteUri
        .takeIf { it.startsWith("https://www.") }
        ?.replaceFirst("https://www.", "")
        ?.takeIf { it.startsWith(urlBarContents) }
    if (withoutWww != null) return withoutWww

    return null
}

/**
 * Returns which URL should be loaded when the user submits their text.
 *
 * This always prioritizes any provided autocompleted suggestion, so callers should ensure that what
 * is provided is a valid suggestion for the current query.
 */
internal fun getUrlToLoad(
    autocompletedSuggestion: NavSuggestion?,
    urlBarContents: String
): Uri {
    return when {
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

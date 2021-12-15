package com.neeva.app.urlbar

import android.graphics.Bitmap
import android.net.Uri
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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.*
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.neeva.app.R
import com.neeva.app.appURL
import com.neeva.app.browsing.toSearchUri
import com.neeva.app.widgets.FaviconView

@Composable
fun AutocompleteTextField(
    urlBarModel: URLBarModel, getFaviconFor: (Uri) -> LiveData<Bitmap?>
) {
    val autocompletedSuggestion by urlBarModel.autocompletedSuggestion.observeAsState(null)
    val value: TextFieldValue by urlBarModel.text.observeAsState(TextFieldValue("", TextRange.Zero))
    val isEditing: Boolean by urlBarModel.isEditing.observeAsState(false)
    var lastEditWasDeletion by remember { mutableStateOf(false) }
    val showingAutocomplete: Boolean by urlBarModel.text.map {
        val autocompleteText = urlBarModel.autocompletedSuggestion.value?.secondaryLabel ?: return@map false
        isEditing && it.text.isNotEmpty() && autocompleteText.isNotEmpty()
                && autocompleteText.startsWith(it.text) && !lastEditWasDeletion
                && autocompletedSuggestion!!.secondaryLabel.length != value.text.length
    }.observeAsState(false)

    val onGoLambda = {
        urlBarModel.onGo(
            autocompletedSuggestion?.url ?: Uri.parse(value.text) ?: value.text.toSearchUri()
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(40.dp)
            .fillMaxWidth()
            .clickable(isEditing) {
                if (showingAutocomplete) {
                    val completed = autocompletedSuggestion!!.secondaryLabel
                    urlBarModel.onLocationBarTextChanged(
                        value.copy(
                            completed,
                            TextRange(completed.length, completed.length),
                            TextRange(completed.length, completed.length)
                        )
                    )
                } else {
                    urlBarModel.onLocationBarTextChanged(
                        value.copy(
                            value.text,
                            TextRange(value.text.length, value.text.length),
                            TextRange(value.text.length, value.text.length)
                        )
                    )
                }
            }
    ) {
        val url = autocompletedSuggestion?.url ?: Uri.parse(value.text) ?: Uri.parse(appURL)
        val bitmap: Bitmap? by getFaviconFor(url).observeAsState()
        FaviconView(bitmap = bitmap, bordered = false)

        BasicTextField(
            value,
            onValueChange = { inside: TextFieldValue ->
                lastEditWasDeletion = inside.text.length < value.text.length
                urlBarModel.onLocationBarTextChanged(inside)
            },
            modifier = Modifier
                .padding(start = 8.dp)
                .wrapContentSize(if (isEditing) Alignment.CenterStart else Alignment.Center)
                .width(IntrinsicSize.Min)
                .adjustIntrinsicWidth()
                .onFocusChanged(urlBarModel::onFocusChanged)
                .focusRequester(urlBarModel.focusRequester)
                .onPreviewKeyEvent {
                    if (it.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_ENTER) {
                        // If we're seeing a hardware enter key, intercept it to prevent adding a newline to the URL.
                        onGoLambda.invoke()
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
            keyboardOptions = KeyboardOptions (
                imeAction = ImeAction.Go,
            ),
            keyboardActions = KeyboardActions (
                onGo = { onGoLambda.invoke() },
            ),
            cursorBrush = SolidColor(if (showingAutocomplete) Color.Unspecified else Color.Black),
        )
        Text(
            text = if (showingAutocomplete) {
                autocompletedSuggestion!!.secondaryLabel.substring(value.text.length)
            } else {
                ""
            },
            modifier = Modifier.background(Color(R.color.selection_highlight)),
            style = MaterialTheme.typography.body1,
            maxLines = 1,
            color = MaterialTheme.colors.onPrimary,
            textAlign = TextAlign.Start
        )
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
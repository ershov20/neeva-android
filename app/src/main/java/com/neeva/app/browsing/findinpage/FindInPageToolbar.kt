package com.neeva.app.browsing.findinpage

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import com.neeva.app.R
import com.neeva.app.ui.LightDarkPreviewContainer
import com.neeva.app.ui.theme.Dimensions

@Composable
fun FindInPageToolbar(
    findInPageInfo: FindInPageInfo,
    onUpdateQuery: (String?) -> Unit,
    onScrollToResult: (forward: Boolean) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val dismissLambda = {
        onUpdateQuery(null)
    }
    BackHandler(onBack = dismissLambda)

    val findInPageText = findInPageInfo.text ?: ""
    val currentIndex = if (findInPageText.isEmpty()) {
        0
    } else {
        findInPageInfo.activeMatchIndex + 1
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Default.Search,
            contentDescription = null,
            modifier = Modifier.padding(horizontal = Dimensions.PADDING_MEDIUM)
        )

        BasicTextField(
            value = findInPageText,
            onValueChange = { onUpdateQuery(it) },
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
            singleLine = true,
            textStyle = TextStyle(
                color = LocalContentColor.current,
                fontSize = MaterialTheme.typography.bodyLarge.fontSize
            ),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Go
            ),
            keyboardActions = KeyboardActions(
                onGo = { }
            ),
            cursorBrush = SolidColor(LocalContentColor.current)
        )

        Spacer(modifier = Modifier.width(Dimensions.PADDING_MEDIUM))

        Text(
            text = stringResource(
                id = R.string.find_in_page_index,
                currentIndex,
                findInPageInfo.numberOfMatches
            ),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        IconButton(onClick = { onScrollToResult(false) }) {
            Icon(
                Icons.Default.KeyboardArrowUp,
                contentDescription = stringResource(id = R.string.previous)
            )
        }
        IconButton(onClick = { onScrollToResult(true) }) {
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = stringResource(id = R.string.next)
            )
        }
        IconButton(onClick = dismissLambda) {
            Icon(
                Icons.Default.Close,
                contentDescription = stringResource(id = R.string.close)
            )
        }
    }

    // Focuses the TextField when the FindInPageUI appears
    LaunchedEffect(true) {
        focusRequester.requestFocus()
    }
}

@Preview("1x font scale", locale = "en")
@Preview("2x font scale", locale = "en", fontScale = 2.0f)
@Preview("RTL, 1x font scale", locale = "he")
@Preview("RTL, 2x font scale", locale = "he", fontScale = 2.0f)
@Composable
fun FindInPageToolbarPreview() {
    LightDarkPreviewContainer {
        Surface(color = MaterialTheme.colorScheme.background) {
            FindInPageToolbar(
                findInPageInfo = FindInPageInfo(text = "preview"),
                onUpdateQuery = {},
                onScrollToResult = {}
            )
        }
    }
}

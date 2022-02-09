package com.neeva.app.urlbar

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import com.neeva.app.R
import com.neeva.app.browsing.ActiveTabModel
import com.neeva.app.ui.theme.NeevaTheme

@Composable
fun FindInPageToolbar(
    findInPageInfo: ActiveTabModel.FindInPageInfo,
    findInPageText: String,
    onFindInPageUpdate: (String?, Boolean) -> Unit
) {
    val dismissLambda = {
        onFindInPageUpdate(null, true)
    }

    TopAppBar(
        backgroundColor = MaterialTheme.colorScheme.background
    ) {
        TextField(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            value = findInPageText,
            onValueChange = { onFindInPageUpdate(it, true) },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Go
            ),
            keyboardActions = KeyboardActions(
                onGo = { }
            ),
            colors = TextFieldDefaults.textFieldColors(
                textColor = MaterialTheme.colorScheme.onBackground,
                backgroundColor = Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.onBackground,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        )
        val currentIndex = if (findInPageText.isEmpty()) 0 else findInPageInfo.activeMatchIndex + 1
        Text(
            text = stringResource(
                id = R.string.find_in_page_index, currentIndex, findInPageInfo.numberOfMatches
            ),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        IconButton(onClick = { onFindInPageUpdate(findInPageText, false) }) {
            Icon(
                Icons.Default.KeyboardArrowUp,
                contentDescription = stringResource(id = R.string.previous),
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
        IconButton(onClick = { onFindInPageUpdate(findInPageText, true) }) {
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = stringResource(id = R.string.next),
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
        IconButton(onClick = dismissLambda) {
            Icon(
                Icons.Default.Close,
                contentDescription = stringResource(id = R.string.close),
                tint = MaterialTheme.colorScheme.onBackground
            )
        }

        BackHandler(onBack = dismissLambda)
    }
}

@Preview("1x font scale", locale = "en")
@Preview("2x font scale", locale = "en", fontScale = 2.0f)
@Preview("RTL, 1x font scale", locale = "he")
@Preview("RTL, 2x font scale", locale = "he", fontScale = 2.0f)
@Composable
fun FindInPagePreview() {
    NeevaTheme(useDarkTheme = false) {
        FindInPageToolbar(
            findInPageInfo = ActiveTabModel.FindInPageInfo(),
            findInPageText = "preview",
            onFindInPageUpdate = { _: String?, _: Boolean -> }
        )
    }
}

@Preview("1x font scale", locale = "en")
@Preview("2x font scale", locale = "en", fontScale = 2.0f)
@Preview("RTL, 1x font scale", locale = "he")
@Preview("RTL, 2x font scale", locale = "he", fontScale = 2.0f)
@Composable
fun FindInPagePreview_DarkTheme() {
    NeevaTheme(useDarkTheme = true) {
        FindInPageToolbar(
            findInPageInfo = ActiveTabModel.FindInPageInfo(),
            findInPageText = "preview",
            onFindInPageUpdate = { _: String?, _: Boolean -> }
        )
    }
}

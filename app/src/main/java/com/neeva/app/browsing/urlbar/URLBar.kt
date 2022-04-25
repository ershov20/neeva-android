package com.neeva.app.browsing.urlbar

import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.neeva.app.LocalAppNavModel
import com.neeva.app.LocalBrowserWrapper
import com.neeva.app.ui.theme.Dimensions

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun URLBar(
    modifier: Modifier = Modifier,
    endComposable: @Composable () -> Unit
) {
    val appNavModel = LocalAppNavModel.current

    val browserWrapper = LocalBrowserWrapper.current
    val urlBarModel = browserWrapper.urlBarModel
    val suggestionsModel = browserWrapper.suggestionsModel

    val urlBarModelState = urlBarModel.state.collectAsState()
    val isEditing: Boolean by urlBarModel.isEditing.collectAsState(false)

    val isIncognito: Boolean = browserWrapper.isIncognito
    val backgroundColor = if (isIncognito) {
        MaterialTheme.colorScheme.inverseSurface
    } else {
        MaterialTheme.colorScheme.surface
    }

    val foregroundColor = if (isIncognito) {
        MaterialTheme.colorScheme.inverseOnSurface
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val placeholderColor = if (isIncognito) {
        MaterialTheme.colorScheme.inverseOnSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(modifier = modifier) {
        Surface(
            color = backgroundColor,
            contentColor = foregroundColor,
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 2.dp,
            modifier = Modifier
                .fillMaxSize()
                .padding(Dimensions.PADDING_SMALL)
        ) {
            val childModifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 40.dp)

            if (isEditing) {
                AutocompleteTextField(
                    textFieldValue = urlBarModelState.value.textFieldValue,
                    suggestionText = urlBarModelState.value.autocompleteSuggestionText,
                    faviconBitmap = urlBarModelState.value.faviconBitmap,
                    placeholderColor = placeholderColor,
                    onLocationEdited = { urlBarModel.onLocationBarTextChanged(it) },
                    onLocationReplaced = { urlBarModel.replaceLocationBarText(it) },
                    onLoadUrl = {
                        browserWrapper.loadUrl(urlBarModelState.value.uriToLoad)
                        suggestionsModel?.logSuggestionTap(
                            urlBarModelState.value.getSuggestionType(),
                            null
                        )
                    },
                    onAcceptAutocompleteSuggestion = urlBarModel::acceptAutocompleteSuggestion,
                    modifier = childModifier
                )
            } else {
                LocationLabel(
                    endComposable = endComposable,
                    placeholderColor = placeholderColor,
                    modifier = childModifier.clickable { urlBarModel.requestFocus() }
                )
            }
        }
    }

    val localFocusManager = LocalFocusManager.current
    BackHandler(enabled = isEditing) {
        localFocusManager.clearFocus()
        appNavModel.showBrowser()
    }
}

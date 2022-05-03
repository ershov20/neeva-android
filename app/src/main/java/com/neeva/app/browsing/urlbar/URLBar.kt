package com.neeva.app.browsing.urlbar

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.neeva.app.LocalAppNavModel
import com.neeva.app.LocalBrowserWrapper
import com.neeva.app.R
import com.neeva.app.browsing.urlbar.trackingprotection.ShieldIconButton
import com.neeva.app.ui.theme.Dimensions
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun URLBar(
    modifier: Modifier = Modifier,
    endComposable: @Composable (modifier: Modifier) -> Unit
) {
    val appNavModel = LocalAppNavModel.current

    val browserWrapper = LocalBrowserWrapper.current
    val urlBarModel = browserWrapper.urlBarModel
    val suggestionsModel = browserWrapper.suggestionsModel

    val urlBarModelState = urlBarModel.state.collectAsState()
    val isEditing: Boolean by urlBarModel.isEditing.collectAsState(false)

    val isIncognito: Boolean = browserWrapper.isIncognito

    val placeholderColor = if (isIncognito) {
        MaterialTheme.colorScheme.inverseOnSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val childModifier = Modifier
        .fillMaxWidth()
        .defaultMinSize(minHeight = 40.dp)

    val iconModifier = Modifier.padding(vertical = Dimensions.PADDING_TINY)

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        AnimatedVisibility(visible = !isEditing) {
            val showIncognitoBadge = browserWrapper.isIncognito

            UrlBarStartComposable(
                showIncognitoBadge = showIncognitoBadge,
                trackersBlocked = browserWrapper.activeTabModel.trackersFlow,
                modifier = iconModifier.padding(start = Dimensions.PADDING_SMALL)
            )
        }

        UrlBarContainer(
            isIncognito = isIncognito,
            modifier = Modifier.weight(1f)
        ) {
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
                    placeholderColor = placeholderColor,
                    modifier = childModifier.clickable { urlBarModel.requestFocus() }
                )
            }
        }
        AnimatedVisibility(visible = !isEditing) {
            endComposable(modifier = iconModifier.padding(end = Dimensions.PADDING_SMALL))
        }
    }

    val localFocusManager = LocalFocusManager.current
    BackHandler(enabled = isEditing) {
        localFocusManager.clearFocus()
        appNavModel.showBrowser()
    }
}

@Composable
private fun UrlBarStartComposable(
    showIncognitoBadge: Boolean,
    trackersBlocked: StateFlow<Int>,
    modifier: Modifier
) {
    when {
        // TODO(kobec): add onClick functionality so that both open up Tracking Protection UI.
        showIncognitoBadge -> {
            IconButton(
                onClick = { /*TODO*/ },
                modifier = modifier
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_incognito),
                    contentDescription = stringResource(R.string.incognito),
                )
            }
        }
        else -> {
            ShieldIconButton(
                trackersBlocked = trackersBlocked.collectAsState().value,
                modifier = modifier
            )
        }
    }
}

@Composable
fun UrlBarContainer(
    isIncognito: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
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

    Surface(
        color = backgroundColor,
        contentColor = foregroundColor,
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 2.dp,
        modifier = modifier.padding(Dimensions.PADDING_SMALL)
    ) {
        content()
    }
}

package com.neeva.app.browsing.urlbar

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.neeva.app.R
import com.neeva.app.browsing.toolbar.BrowserToolbarModel
import com.neeva.app.browsing.urlbar.trackingprotection.ShieldIconButton
import com.neeva.app.ui.theme.Dimensions

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun URLBar(
    browserToolbarModel: BrowserToolbarModel,
    modifier: Modifier = Modifier,
    endComposable: @Composable (modifier: Modifier) -> Unit
) {
    val urlBarModel = browserToolbarModel.urlBarModel
    val urlBarModelState = urlBarModel.stateFlow.collectAsState()
    val isEditing = urlBarModelState.value.isEditing

    val trackerDataFlow = browserToolbarModel.cookieCutterModel?.trackingDataFlow?.collectAsState()
    val trackerData = trackerDataFlow?.value

    val iconModifier = Modifier.padding(vertical = Dimensions.PADDING_TINY)

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        AnimatedVisibility(visible = !isEditing) {
            UrlBarStartComposable(
                showIncognitoBadge = browserToolbarModel.isIncognito,
                trackersBlocked = trackerData?.numTrackers ?: 0,
                modifier = iconModifier.padding(start = Dimensions.PADDING_SMALL)
            )
        }

        UrlBarContainer(
            isIncognito = browserToolbarModel.isIncognito,
            modifier = Modifier.weight(1f)
        ) { placeholderColor ->
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
                    onLoadUrl = { browserToolbarModel.onLoadUrl(urlBarModelState.value) },
                    onAcceptAutocompleteSuggestion = urlBarModel::acceptAutocompleteSuggestion,
                    modifier = childModifier.then(
                        if (isEditing) {
                            Modifier.padding(start = Dimensions.PADDING_MEDIUM)
                        } else {
                            Modifier.padding(horizontal = Dimensions.PADDING_MEDIUM)
                        }
                    )
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
}

@Composable
fun UrlBarStartComposable(
    showIncognitoBadge: Boolean,
    trackersBlocked: Int,
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
                trackersBlocked = trackersBlocked,
                modifier = modifier
            )
        }
    }
}

@Composable
fun UrlBarContainer(
    isIncognito: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable (placeholderColor: Color) -> Unit
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

    val placeholderColor = if (isIncognito) {
        MaterialTheme.colorScheme.inverseOnSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        color = backgroundColor,
        contentColor = foregroundColor,
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 2.dp,
        modifier = modifier.padding(Dimensions.PADDING_SMALL)
    ) {
        content(placeholderColor)
    }
}

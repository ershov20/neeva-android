package com.neeva.app.urlbar

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.neeva.app.LocalAppNavModel
import com.neeva.app.LocalBrowserWrapper
import com.neeva.app.R
import com.neeva.app.neeva_menu.OverflowMenu
import com.neeva.app.ui.theme.Dimensions

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun URLBar() {
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
        MaterialTheme.colorScheme.surfaceVariant
    }

    val foregroundColor = if (isIncognito) {
        MaterialTheme.colorScheme.inverseOnSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    TopAppBar(
        backgroundColor = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .weight(1.0f)
                .padding(Dimensions.PADDING_SMALL)
        ) {
            val childModifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 40.dp)
                .background(
                    color = backgroundColor,
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(start = 12.dp)

            AutocompleteTextField(
                urlBarModel = urlBarModel,
                suggestionsModel = suggestionsModel,
                urlBarModelState = urlBarModelState.value,
                backgroundColor = backgroundColor,
                foregroundColor = foregroundColor,
                modifier = childModifier
            )

            // We need to have both the AutocompleteTextField and the LocationLabel in the URLBar
            // at the same time because the AutocompleteTextField is the thing that must be focused
            // when the LocationLabel is clicked.
            if (!isEditing) {
                LocationLabel(
                    foregroundColor = foregroundColor,
                    showIncognitoBadge = isIncognito,
                    modifier = childModifier.clickable { urlBarModel.onRequestFocus() }
                )
            }
        }

        AnimatedVisibility(
            visible = !isEditing,
            enter = expandHorizontally().plus(fadeIn()),
            exit = shrinkHorizontally().plus(fadeOut())
        ) {
            OverflowMenu(onMenuItem = { id -> appNavModel.onMenuItem(id) })
        }

        AnimatedVisibility(
            visible = isEditing,
            enter = expandHorizontally().plus(fadeIn()),
            exit = shrinkHorizontally().plus(fadeOut())
        ) {
            val localFocusManager = LocalFocusManager.current
            val cancelLambda = {
                localFocusManager.clearFocus()
                appNavModel.showBrowser()
            }

            TextButton(onClick = cancelLambda) {
                Text(
                    text = stringResource(id = R.string.cancel),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            BackHandler(onBack = cancelLambda)
        }
    }
}

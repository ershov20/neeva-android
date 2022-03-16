package com.neeva.app.urlbar

import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.TopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.neeva.app.LocalAppNavModel
import com.neeva.app.LocalBrowserWrapper
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
        MaterialTheme.colorScheme.onSurface
    }

    val placeholderColor = if (isIncognito) {
        MaterialTheme.colorScheme.inverseOnSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    TopAppBar(
        backgroundColor = MaterialTheme.colorScheme.background
    ) {
        Surface(
            color = backgroundColor,
            contentColor = foregroundColor,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .weight(1.0f)
                .padding(Dimensions.PADDING_SMALL)
        ) {
            val childModifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 40.dp)

            // We need to have both the AutocompleteTextField and the LocationLabel in the
            // URLBar at the same time because the AutocompleteTextField is the thing that must
            // be focused when the LocationLabel is clicked.
            // TODO(dan.alcantara): Fix this by making the UrlBarModel keep track of what needs
            //                      to be focused and at what time.
            AutocompleteTextField(
                urlBarModel = urlBarModel,
                suggestionsModel = suggestionsModel,
                urlBarModelState = urlBarModelState.value,
                placeholderColor = placeholderColor,
                modifier = childModifier.alpha(if (isEditing) 1.0f else 0.0f)
            )

            if (!isEditing) {
                LocationLabel(
                    showIncognitoBadge = isIncognito,
                    onMenuItem = appNavModel::onMenuItem,
                    placeholderColor = placeholderColor,
                    modifier = childModifier.clickable { urlBarModel.onRequestFocus() }
                )
            }
        }

        val localFocusManager = LocalFocusManager.current
        BackHandler(enabled = isEditing) {
            localFocusManager.clearFocus()
            appNavModel.showBrowser()
        }
    }
}

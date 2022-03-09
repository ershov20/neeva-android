package com.neeva.app.urlbar

import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.TopAppBar
import androidx.compose.material3.MaterialTheme
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
                foregroundColor = foregroundColor,
                placeholderColor = placeholderColor,
                modifier = childModifier
            )

            // We need to have both the AutocompleteTextField and the LocationLabel in the URLBar
            // at the same time because the AutocompleteTextField is the thing that must be focused
            // when the LocationLabel is clicked.
            if (!isEditing) {
                LocationLabel(
                    foregroundColor = foregroundColor,
                    showIncognitoBadge = isIncognito,
                    onMenuItem = appNavModel::onMenuItem,
                    onEditUrl = urlBarModel::onRequestFocus,
                    modifier = childModifier
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

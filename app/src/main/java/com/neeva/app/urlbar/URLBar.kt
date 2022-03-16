package com.neeva.app.urlbar

import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import com.neeva.app.LocalAppNavModel
import com.neeva.app.LocalBrowserWrapper
import com.neeva.app.R
import com.neeva.app.ui.theme.Dimensions

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun URLBar() {
    val appNavModel = LocalAppNavModel.current

    val browserWrapper = LocalBrowserWrapper.current
    val activeTabModel = browserWrapper.activeTabModel
    val urlBarModel = browserWrapper.urlBarModel
    val suggestionsModel = browserWrapper.suggestionsModel

    val progress: Int by activeTabModel.progressFlow.collectAsState()
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

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(id = R.dimen.top_toolbar_height))
    ) {
        Box {
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

            if (progress != 100) {
                LinearProgressIndicator(
                    progress = progress / 100.0f,
                    modifier = Modifier
                        .height(2.dp)
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer
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

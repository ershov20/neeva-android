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
import androidx.compose.material.TopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.neeva.app.LocalEnvironment
import com.neeva.app.R
import com.neeva.app.ui.theme.md_theme_dark_shadow

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun URLBar() {
    val browserWrapper = LocalEnvironment.current.browserWrapper
    val urlBarModel = browserWrapper.urlBarModel
    val activeTabModel = browserWrapper.activeTabModel
    val faviconCache = browserWrapper.faviconCache

    val isEditing: Boolean by urlBarModel.isEditing.collectAsState(false)

    // TODO(kobec): figure out how to map incognito to color scheme?
    val isIncognito: Boolean = urlBarModel.isIncognito
    val backgroundColor = if (isIncognito) {
        md_theme_dark_shadow
    } else {
        MaterialTheme.colorScheme.background
    }

    val foregroundColor = if (isIncognito) {
        Color.White
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    TopAppBar(
        backgroundColor = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .weight(1.0f)
                .padding(8.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.primary)
        ) {
            val textFieldValue = urlBarModel.textFieldValue.collectAsState()

            AutocompleteTextField(
                urlBarModel = urlBarModel,
                textFieldValue = textFieldValue.value,
                textFieldValueMutator = urlBarModel::setTextFieldValue,
                faviconCache = faviconCache,
                backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                foregroundColor = foregroundColor
            )

            // We need to have both the AutocompleteTextField and the LocationLabel in the URLBar
            // at the same time because the AutocompleteTextField is the thing that must be focused
            // when the LocationLabel is clicked.
            if (!isEditing) {
                val displayedLocation by activeTabModel.displayedText.collectAsState()
                val locationInfoResource: Int? by
                activeTabModel.locationInfoResource.collectAsState()

                LocationLabel(
                    urlBarValue = displayedLocation,
                    showIncognitoBadge = isIncognito,
                    backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                    foregroundColor = foregroundColor,
                    locationInfoResource = locationInfoResource,
                    onReload = urlBarModel::reload,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { urlBarModel.onRequestFocus() }
                )
            }
        }

        AnimatedVisibility(
            visible = isEditing,
            enter = expandHorizontally().plus(fadeIn()),
            exit = shrinkHorizontally().plus(fadeOut())
        ) {
            val localFocusManager = LocalFocusManager.current
            val appNavModel = LocalEnvironment.current.appNavModel
            val cancelLambda = {
                localFocusManager.clearFocus()
                appNavModel.showBrowser()
            }

            Box(
                Modifier
                    .clickable(
                        onClickLabel = stringResource(id = R.string.cancel),
                        onClick = cancelLambda
                    )
                    .defaultMinSize(minHeight = 40.dp)
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
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

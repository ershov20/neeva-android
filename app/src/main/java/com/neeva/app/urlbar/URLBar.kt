package com.neeva.app.urlbar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.neeva.app.R
import com.neeva.app.browsing.ActiveTabModel
import com.neeva.app.suggestions.SuggestionsModel
import com.neeva.app.ui.theme.LightModeBlue
import com.neeva.app.widgets.ComposableSingletonEntryPoint
import dagger.hilt.EntryPoints

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun URLBar(
    suggestionsModel: SuggestionsModel,
    activeTabModel: ActiveTabModel,
    urlBarModel: URLBarModel
) {
    val historyManager = EntryPoints
        .get(LocalContext.current.applicationContext, ComposableSingletonEntryPoint::class.java)
        .historyManager()

    val isEditing: Boolean by urlBarModel.isEditing.collectAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colors.primary)
            .padding(horizontal = 8.dp)
            .padding(vertical = 8.dp)
            .height(40.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1.0f)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colors.primaryVariant)
        ) {
            AutocompleteTextField(
                suggestionsModel = suggestionsModel,
                urlBarModel = urlBarModel,
                getFaviconFlow = {
                    historyManager.getFaviconFlow(uri = it, allowFallbackIcon = false)
                },
                urlBarIsBeingEdited = isEditing
            )

            // We need to have both the AutocompleteTextField and the LocationLabel in the URLBar
            // at the same time because the AutocompleteTextField is the thing that must be focused
            // when the LocationLabel is clicked.
            if (!isEditing) {
                val displayedDomain by activeTabModel.displayedDomain.collectAsState()
                val showLock: Boolean by activeTabModel.showLock.collectAsState()
                LocationLabel(
                    urlBarValue = displayedDomain,
                    showLock = showLock,
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
            Box(
                Modifier
                    .clickable(onClickLabel = stringResource(id = R.string.cancel)) {
                        localFocusManager.clearFocus()
                    }
                    .defaultMinSize(minHeight = 40.dp)
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(id = R.string.cancel),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    color = LightModeBlue
                )
            }
        }
    }
}

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
import com.neeva.app.R
import com.neeva.app.browsing.ActiveTabModel
import com.neeva.app.storage.FaviconCache
import com.neeva.app.suggestions.SuggestionsModel
import com.neeva.app.ui.theme.md_theme_dark_shadow

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun URLBar(
    suggestionsModel: SuggestionsModel?,
    activeTabModel: ActiveTabModel,
    urlBarModel: URLBarModel,
    faviconCache: FaviconCache
) {
    val isEditing: Boolean by urlBarModel.isEditing.collectAsState()

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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 8.dp)
            .padding(vertical = 8.dp)
            .height(40.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1.0f)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.primary)
        ) {
            AutocompleteTextField(
                suggestionsModel = suggestionsModel,
                urlBarModel = urlBarModel,
                faviconCache = faviconCache,
                urlBarIsBeingEdited = isEditing,
                backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                foregroundColor = foregroundColor
            )

            // We need to have both the AutocompleteTextField and the LocationLabel in the URLBar
            // at the same time because the AutocompleteTextField is the thing that must be focused
            // when the LocationLabel is clicked.
            if (!isEditing) {
                val displayedDomain by activeTabModel.displayedDomain.collectAsState()
                val showLock: Boolean by activeTabModel.showLock.collectAsState()

                LocationLabel(
                    urlBarValue = displayedDomain,
                    showIncognitoBadge = isIncognito,
                    backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                    foregroundColor = foregroundColor,
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
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

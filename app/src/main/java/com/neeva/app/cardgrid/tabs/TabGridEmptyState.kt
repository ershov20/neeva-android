package com.neeva.app.cardgrid.tabs

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.neeva.app.R
import com.neeva.app.ui.OneBooleanPreviewContainer

@Composable
fun TabGridEmptyState(
    isIncognito: Boolean,
    modifier: Modifier = Modifier
) {
    val emptyLogoId: Int
    val emptyStringId: Int
    if (isIncognito) {
        // TODO(dan.alcantara): Material3 doesn't seem to have a MaterialTheme.colors.isLight().
        emptyLogoId = if (isSystemInDarkTheme()) {
            R.drawable.ic_empty_incognito_tabs_dark
        } else {
            R.drawable.ic_empty_incognito_tabs_light
        }
        emptyStringId = R.string.empty_incognito_tabs_title
    } else {
        emptyLogoId = if (isSystemInDarkTheme()) {
            R.drawable.ic_empty_regular_tabs_dark
        } else {
            R.drawable.ic_empty_regular_tabs_light
        }
        emptyStringId = R.string.empty_regular_tabs_title
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(emptyLogoId),
                contentDescription = null
            )

            Text(
                text = stringResource(emptyStringId),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = stringResource(R.string.empty_tab_hint),
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview
@Composable
fun TabGridEmptyStatePreview() {
    OneBooleanPreviewContainer { isIncognito ->
        TabGridEmptyState(
            isIncognito = isIncognito,
            modifier = Modifier.fillMaxSize()
        )
    }
}

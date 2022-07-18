package com.neeva.app.cardgrid.tabs

import androidx.compose.foundation.Image
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
import com.neeva.app.LocalIsDarkTheme
import com.neeva.app.R
import com.neeva.app.ui.OneBooleanPreviewContainer

@Composable
fun TabGridEmptyState(
    isIncognito: Boolean,
    modifier: Modifier = Modifier
) {
    val isNeevaThemeUsingDarkColors = LocalIsDarkTheme.current
    val emptyLogoId: Int
    val emptyStringId: Int
    if (isIncognito) {
        emptyLogoId = if (isNeevaThemeUsingDarkColors) {
            R.drawable.ic_empty_incognito_tabs_dark
        } else {
            R.drawable.ic_empty_incognito_tabs_light
        }
        emptyStringId = R.string.tab_switcher_no_incognito_tabs
    } else {
        emptyLogoId = if (isNeevaThemeUsingDarkColors) {
            R.drawable.ic_empty_regular_tabs_dark
        } else {
            R.drawable.ic_empty_regular_tabs_light
        }
        emptyStringId = R.string.tab_switcher_no_tabs
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
                text = stringResource(R.string.tab_switcher_create_tab_hint),
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
fun TabGridEmptyStatePreview_Light() {
    OneBooleanPreviewContainer(useDarkTheme = false) { isIncognito ->
        TabGridEmptyState(
            isIncognito = isIncognito,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Preview
@Composable
fun TabGridEmptyStatePreview_Dark() {
    OneBooleanPreviewContainer(useDarkTheme = true) { isIncognito ->
        TabGridEmptyState(
            isIncognito = isIncognito,
            modifier = Modifier.fillMaxSize()
        )
    }
}

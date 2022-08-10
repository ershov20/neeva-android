package com.neeva.app.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.neeva.app.R
import com.neeva.app.ui.FullScreenDialogTopBar
import com.neeva.app.ui.NeevaThemePreviewContainer
import com.neeva.app.ui.widgets.AssetsText
import com.neeva.app.ui.widgets.NavigationRow
import com.neeva.app.ui.widgets.collapsingsection.CollapsingSectionState
import com.neeva.app.ui.widgets.collapsingsection.collapsingSection
import com.neeva.app.ui.widgets.collapsingsection.setNextState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesPane(
    onShowAdditionalLicenses: () -> Unit,
    onClose: () -> Unit
) {
    Scaffold(
        topBar = @Composable {
            FullScreenDialogTopBar(
                title = stringResource(R.string.settings_licenses),
                onBackPressed = onClose
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        val chromiumCollapsingSectionState = remember {
            mutableStateOf(CollapsingSectionState.COLLAPSED)
        }

        LazyColumn(modifier = Modifier.padding(paddingValues)) {
            item {
                NavigationRow(
                    primaryLabel = stringResource(R.string.settings_additional_licenses),
                    onClick = onShowAdditionalLicenses
                )
            }

            collapsingSection(
                label = R.string.chromium,
                collapsingSectionState = chromiumCollapsingSectionState.value,
                onUpdateCollapsingSectionState = chromiumCollapsingSectionState::setNextState
            ) {
                item {
                    AssetsText(assetFilename = "licenses/chromium_license.txt")
                }
            }
        }
    }
}

@Preview(locale = "en", fontScale = 1.0f)
@Preview(locale = "en", fontScale = 2.0f)
@Composable
fun LicensesPane_Preview_Light() {
    NeevaThemePreviewContainer(useDarkTheme = false) {
        LicensesPane(
            onShowAdditionalLicenses = {},
            onClose = {}
        )
    }
}

@Preview(locale = "en", fontScale = 1.0f)
@Composable
fun LicensesPane_Preview_Dark() {
    NeevaThemePreviewContainer(useDarkTheme = true) {
        LicensesPane(
            onShowAdditionalLicenses = {},
            onClose = {}
        )
    }
}

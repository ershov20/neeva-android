package com.neeva.app.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.neeva.app.R
import com.neeva.app.settings.sharedComposables.subcomponents.SettingsNavigationRow
import com.neeva.app.ui.FullScreenDialogTopBar
import com.neeva.app.ui.widgets.AssetsText
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
    ) {
        val chromiumCollapsingSectionState = remember {
            mutableStateOf(CollapsingSectionState.COLLAPSED)
        }

        LazyColumn {
            item {
                SettingsNavigationRow(
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

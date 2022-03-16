package com.neeva.app.settings.setDefaultAndroidBrowser

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.neeva.app.R
import com.neeva.app.settings.SettingsViewModel
import com.neeva.app.ui.FullScreenDialogTopBar

/**
 * If the user opened this Settings Pane, the phone we are running on has a
 * system image lower than Android Q. RoleManager dialog will be unavailable here.
 */
@Composable
fun SetDefaultAndroidBrowserPane(
    settingsViewModel: SettingsViewModel
) {
    Surface {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .fillMaxSize()
        ) {
            FullScreenDialogTopBar(
                title = stringResource(R.string.settings_default_browser),
                onBackPressed = settingsViewModel::onBackPressed
            )
            Spacer(Modifier.height(28.dp))

            Text(
                text = stringResource(id = R.string.switch_default_browser),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Text(
                text = stringResource(id = R.string.switch_default_browser_instructions),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(24.dp)
            )

            Button(
                onClick = {
                    settingsViewModel.openAndroidDefaultBrowserSettings()
                },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(stringResource(id = R.string.go_to_settings))
            }
        }
    }
}

package com.neeva.app.settings.setDefaultAndroidBrowser

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.neeva.app.R
import com.neeva.app.settings.SettingsController
import com.neeva.app.settings.mockSettingsControllerImpl
import com.neeva.app.ui.FullScreenDialogTopBar
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.ui.theme.NeevaTheme

/**
 * If the user opened this Settings Pane, the phone we are running on has a
 * system image lower than Android Q. RoleManager dialog will be unavailable here.
 */
@Composable
fun SetDefaultAndroidBrowserPane(
    settingsController: SettingsController
) {
    Surface {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FullScreenDialogTopBar(
                title = stringResource(R.string.settings_default_browser),
                onBackPressed = settingsController::onBackPressed
            )

            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(Dimensions.PADDING_LARGE)
            ) {
                Spacer(Modifier.height(28.dp))

                Text(
                    text = stringResource(id = R.string.switch_default_browser),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = stringResource(id = R.string.switch_default_browser_promo),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.outline
                )

                Spacer(Modifier.height(28.dp))

                Text(
                    text = stringResource(id = R.string.switch_default_browser_follow_3_steps),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.outline
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = stringResource(id = R.string.switch_default_browser_instructions),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.height(28.dp))

                Button(
                    onClick = {
                        settingsController.openAndroidDefaultBrowserSettings()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(id = R.string.go_to_settings))
                }
            }
        }
    }
}

@Preview(name = "SetDefaultAndroidBrowserPane, 1x font size", locale = "en")
@Preview(name = "SetDefaultAndroidBrowserPane, 2x font size", locale = "en", fontScale = 2.0f)
@Preview(name = "SetDefaultAndroidBrowserPane, RTL, 1x font size", locale = "he")
@Preview(name = "SetDefaultAndroidBrowserPane, RTL, 2x font size", locale = "he", fontScale = 2.0f)
@Composable
fun SettingsDefaultAndroidBrowser_Preview() {
    NeevaTheme {
        SetDefaultAndroidBrowserPane(
            mockSettingsControllerImpl
        )
    }
}

@Preview(name = "SetDefaultAndroidBrowserPane Dark, 1x font size", locale = "en")
@Preview(name = "SetDefaultAndroidBrowserPane Dark, 2x font size", locale = "en", fontScale = 2.0f)
@Preview(name = "SetDefaultAndroidBrowserPane Dark, RTL, 1x font size", locale = "he")
@Preview(
    name = "SetDefaultAndroidBrowserPane Dark, RTL, 2x font size",
    locale = "he",
    fontScale = 2.0f
)
@Composable
fun SettingsDefaultAndroidBrowser_Dark_Preview() {
    NeevaTheme(useDarkTheme = true) {
        SetDefaultAndroidBrowserPane(
            mockSettingsControllerImpl
        )
    }
}

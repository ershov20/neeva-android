package com.neeva.app.settings.setDefaultAndroidBrowser

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.neeva.app.LocalAppNavModel
import com.neeva.app.R
import com.neeva.app.firstrun.widgets.buttons.OnboardingButton
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
    settingsController: SettingsController,
    fromWelcomeScreen: Boolean
) {
    val appNavModel = LocalAppNavModel.current

    SetDefaultAndroidBrowserPane(
        settingsController = settingsController,
        fromWelcomeScreen = fromWelcomeScreen
    ) {
        appNavModel.openLazyTab(false)
    }
}

@Composable
fun SetDefaultAndroidBrowserPane(
    settingsController: SettingsController,
    fromWelcomeScreen: Boolean,
    showZeroQuery: () -> Unit
) {
    val setDefaultAndroidBrowserManager = settingsController.getSetDefaultAndroidBrowserManager()
    val isRoleManagerAvailable = setDefaultAndroidBrowserManager.isRoleManagerAvailable()

    Surface {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FullScreenDialogTopBar(
                title = stringResource(R.string.settings_default_browser),
                onBackPressed = settingsController::onBackPressed
            )

            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(Dimensions.PADDING_LARGE)
                    .weight(1.0f),
                verticalArrangement = Arrangement.SpaceAround
            ) {

                Column(
                    verticalArrangement = Arrangement.spacedBy(Dimensions.PADDING_MEDIUM)
                ) {
                    Text(
                        text = stringResource(id = R.string.switch_default_browser),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = stringResource(id = R.string.switch_default_browser_promo),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                if (!isRoleManagerAvailable) {
                    // Because RoleManager is not available, we need to spell out the instructions
                    // to the user on how to change the default browser, since they will need to
                    // leave the app and manually make the change in Settings.

                    Column(
                        verticalArrangement = Arrangement.spacedBy(Dimensions.PADDING_MEDIUM)
                    ) {
                        Text(
                            text = stringResource(
                                id = R.string.switch_default_browser_follow_3_steps
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.outline
                        )

                        Text(
                            text = stringResource(
                                id = R.string.switch_default_browser_instructions
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                val resourceId = if (isRoleManagerAvailable) {
                    R.string.set_neeva_as_default_browser
                } else {
                    R.string.go_to_settings
                }

                OnboardingButton(
                    text = stringResource(id = resourceId),
                    onClick = {
                        if (fromWelcomeScreen && isRoleManagerAvailable) {
                            // If user is on the welcome screen and RoleManager is available, this
                            // button shows a dialog while navigating the browser to zero query. No
                            // matter the choice on the dialog, when the dialog is gone, the user
                            // lands on the zero query screen.

                            setDefaultAndroidBrowserManager.requestToBeDefaultBrowser()
                            showZeroQuery()
                        } else {
                            settingsController.openAndroidDefaultBrowserSettings(fromWelcomeScreen)
                        }
                    }
                )
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
            mockSettingsControllerImpl,
            fromWelcomeScreen = false
        ) {}
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
            mockSettingsControllerImpl,
            fromWelcomeScreen = false
        ) {}
    }
}

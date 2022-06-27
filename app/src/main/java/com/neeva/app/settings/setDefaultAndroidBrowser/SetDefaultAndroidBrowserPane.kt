package com.neeva.app.settings.setDefaultAndroidBrowser

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.neeva.app.LocalAppNavModel
import com.neeva.app.LocalEnvironment
import com.neeva.app.LocalIsDarkTheme
import com.neeva.app.R
import com.neeva.app.logging.LogConfig
import com.neeva.app.settings.SettingsController
import com.neeva.app.ui.FullScreenDialogTopBar
import com.neeva.app.ui.theme.ColorPalette
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.ui.theme.NeevaTheme
import com.neeva.app.ui.widgets.FilledButton

/**
 * If the user opened this Settings Pane, the phone we are running on has a
 * system image lower than Android Q. RoleManager dialog will be unavailable here.
 */
@Composable
fun SetDefaultAndroidBrowserPane(
    settingsController: SettingsController,
    showAsDialog: Boolean
) {
    val appNavModel = LocalAppNavModel.current

    SetDefaultAndroidBrowserPane(
        settingsController = settingsController,
        showAsDialog = showAsDialog,
        showZeroQuery = { appNavModel.openLazyTab(false) }
    )
}

@Composable
fun SetDefaultAndroidBrowserPane(
    settingsController: SettingsController,
    showAsDialog: Boolean,
    showZeroQuery: () -> Unit
) {
    val clientLogger = LocalEnvironment.current.clientLogger
    val setDefaultAndroidBrowserManager = settingsController.getSetDefaultAndroidBrowserManager()
    val isRoleManagerAvailable = setDefaultAndroidBrowserManager.isRoleManagerAvailable()

    SetDefaultAndroidBrowserPane(
        mustOpenSettings = !isRoleManagerAvailable,
        showAsDialog = showAsDialog,
        onBackPressed = settingsController::onBackPressed,
        onMaybeLater = {
            clientLogger.logCounter(
                LogConfig.Interaction.DEFAULT_BROWSER_ONBOARDING_INTERSTITIAL_REMIND,
                null
            )
            showZeroQuery()
        },
        onOpenSettings = {
            clientLogger.logCounter(
                LogConfig.Interaction.DEFAULT_BROWSER_ONBOARDING_INTERSTITIAL_OPEN,
                null
            )
            if (showAsDialog && isRoleManagerAvailable) {
                // If user is on the welcome screen and RoleManager is available, this
                // button shows a dialog while navigating the browser to zero query. No
                // matter the choice on the dialog, when the dialog is gone, the user
                // lands on the zero query screen.
                setDefaultAndroidBrowserManager.requestToBeDefaultBrowser()
                showZeroQuery()
            } else {
                settingsController.openAndroidDefaultBrowserSettings(showAsDialog)
            }
        }
    )
}

@Composable
fun SetDefaultAndroidBrowserPane(
    mustOpenSettings: Boolean,
    showAsDialog: Boolean,
    onBackPressed: () -> Unit,
    onMaybeLater: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val backgroundColor = when {
        !showAsDialog -> MaterialTheme.colorScheme.surface
        LocalIsDarkTheme.current -> MaterialTheme.colorScheme.background
        else -> ColorPalette.Brand.Offwhite
    }

    Surface(
        color = backgroundColor,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (!showAsDialog) {
                FullScreenDialogTopBar(
                    title = stringResource(R.string.settings_default_browser),
                    onBackPressed = onBackPressed
                )
            }

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
                        text = stringResource(id = R.string.switch_default_browser_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = stringResource(id = R.string.switch_default_browser_promo),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                // Because RoleManager is not available, we need to spell out the instructions
                // to the user on how to change the default browser, since they will need to
                // leave the app and manually make the change in Settings.
                if (mustOpenSettings) {
                    InstructionsForAndroidSettings()
                }

                val resourceId = if (mustOpenSettings) {
                    R.string.go_to_settings
                } else {
                    R.string.set_neeva_as_default_browser
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(Dimensions.PADDING_LARGE))

                    FilledButton(
                        text = stringResource(id = resourceId),
                        onClick = onOpenSettings
                    )

                    Spacer(modifier = Modifier.height(Dimensions.PADDING_LARGE))

                    if (showAsDialog) {
                        // Add a dismiss button.
                        TextButton(onClick = onMaybeLater) {
                            Text(stringResource(id = R.string.maybe_later))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Show instructions that will let the user navigate through Android's settings to set our app as
 * the default browser.
 */
@Composable
fun InstructionsForAndroidSettings() {
    Column(
        verticalArrangement = Arrangement.spacedBy(Dimensions.PADDING_MEDIUM),
    ) {
        Text(
            text = stringResource(
                id = R.string.switch_default_browser_follow_3_steps
            ),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.outline
        )

        Surface(
            shape = RoundedCornerShape(Dimensions.RADIUS_LARGE),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(Dimensions.RADIUS_LARGE)
                )
        ) {
            Text(
                text = stringResource(
                    id = R.string.switch_default_browser_instructions
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(Dimensions.PADDING_MEDIUM)
            )
        }
    }
}

@Preview(name = "Dialog, 1x font size", locale = "en")
@Preview(name = "Dialog, 2x font size", locale = "en", fontScale = 2.0f)
@Composable
fun SettingsDefaultAndroidBrowser_PreviewAsDialog() {
    NeevaTheme {
        SetDefaultAndroidBrowserPane(
            mustOpenSettings = false,
            showAsDialog = true,
            onBackPressed = {},
            onMaybeLater = {},
            onOpenSettings = {}
        )
    }
}

@Preview(name = "Dark Dialog, 1x font size", locale = "en")
@Preview(name = "Dark dialog, 2x font size", locale = "en", fontScale = 2.0f)
@Composable
fun SettingsDefaultAndroidBrowser_PreviewAsDialog_Dark() {
    NeevaTheme(useDarkTheme = true) {
        SetDefaultAndroidBrowserPane(
            mustOpenSettings = false,
            showAsDialog = true,
            onBackPressed = {},
            onMaybeLater = {},
            onOpenSettings = {}
        )
    }
}

@Preview(name = "Dialog mustOpenSettings, 1x font size", locale = "en")
@Preview(name = "Dialog mustOpenSettings, 2x font size", locale = "en", fontScale = 2.0f)
@Composable
fun SettingsDefaultAndroidBrowser_PreviewAsDialog_MustOpenSettings() {
    NeevaTheme {
        SetDefaultAndroidBrowserPane(
            mustOpenSettings = true,
            showAsDialog = true,
            onBackPressed = {},
            onMaybeLater = {},
            onOpenSettings = {}
        )
    }
}

@Preview(name = "Dark dialog mustOpenSettings, 1x font size", locale = "en")
@Preview(name = "Dark dialog mustOpenSettings, 2x font size", locale = "en", fontScale = 2.0f)
@Composable
fun SettingsDefaultAndroidBrowser_Dark_PreviewAsDialog_MustOpenSettings() {
    NeevaTheme(useDarkTheme = true) {
        SetDefaultAndroidBrowserPane(
            mustOpenSettings = true,
            showAsDialog = true,
            onBackPressed = {},
            onMaybeLater = {},
            onOpenSettings = {}
        )
    }
}

@Preview(name = "Light pane, 1x font size", locale = "en")
@Preview(name = "Light pane, 2x font size", locale = "en", fontScale = 2.0f)
@Composable
fun SettingsDefaultAndroidBrowser_Preview() {
    NeevaTheme {
        SetDefaultAndroidBrowserPane(
            mustOpenSettings = false,
            showAsDialog = false,
            onBackPressed = {},
            onMaybeLater = {},
            onOpenSettings = {}
        )
    }
}

@Preview(name = "Dark pane, 1x font size", locale = "en")
@Preview(name = "Dark pane, 2x font size", locale = "en", fontScale = 2.0f)
@Composable
fun SettingsDefaultAndroidBrowser_Dark_Preview() {
    NeevaTheme(useDarkTheme = true) {
        SetDefaultAndroidBrowserPane(
            mustOpenSettings = false,
            showAsDialog = false,
            onBackPressed = {},
            onMaybeLater = {},
            onOpenSettings = {}
        )
    }
}

@Preview(name = "mustOpenSettings, 1x font size", locale = "en")
@Preview(name = "mustOpenSettings, 2x font size", locale = "en", fontScale = 2.0f)
@Composable
fun SettingsDefaultAndroidBrowser_Preview_MustOpenSettings() {
    NeevaTheme {
        SetDefaultAndroidBrowserPane(
            mustOpenSettings = true,
            showAsDialog = false,
            onBackPressed = {},
            onMaybeLater = {},
            onOpenSettings = {}
        )
    }
}

@Preview(name = "Dark mustOpenSettings, 1x font size", locale = "en")
@Preview(name = "Dark mustOpenSettings, 2x font size", locale = "en", fontScale = 2.0f)
@Composable
fun SettingsDefaultAndroidBrowser_Dark_Preview_MustOpenSettings() {
    NeevaTheme(useDarkTheme = true) {
        SetDefaultAndroidBrowserPane(
            mustOpenSettings = true,
            showAsDialog = false,
            onBackPressed = {},
            onMaybeLater = {},
            onOpenSettings = {}
        )
    }
}

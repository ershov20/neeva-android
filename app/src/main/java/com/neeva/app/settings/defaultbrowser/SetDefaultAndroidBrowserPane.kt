package com.neeva.app.settings.defaultbrowser

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.neeva.app.LocalAppNavModel
import com.neeva.app.LocalClientLogger
import com.neeva.app.LocalIsDarkTheme
import com.neeva.app.R
import com.neeva.app.logging.ClientLogger
import com.neeva.app.logging.LogConfig
import com.neeva.app.settings.SettingsController
import com.neeva.app.ui.FullScreenDialogTopBar
import com.neeva.app.ui.PortraitPreviews
import com.neeva.app.ui.PortraitPreviewsDark
import com.neeva.app.ui.theme.ColorPalette
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.ui.theme.NeevaTheme
import com.neeva.app.ui.widgets.StackedButtons

@Composable
fun SetDefaultAndroidBrowserPane(
    settingsController: SettingsController
) {
    val appNavModel = LocalAppNavModel.current
    val clientLogger = LocalClientLogger.current
    val setDefaultAndroidBrowserManager = settingsController.getSetDefaultAndroidBrowserManager()

    val showZeroQuery = { appNavModel.openLazyTab(false) }

    SetDefaultAndroidBrowserPane(
        clientLogger = clientLogger,
        onBackPressed = settingsController::onBackPressed,
        openAndroidDefaultBrowserSettings = settingsController::openAndroidDefaultBrowserSettings,
        setDefaultAndroidBrowserManager = setDefaultAndroidBrowserManager,
        showAsDialog = false,
        onActivityResultCallback = { showZeroQuery() },
        showZeroQuery = showZeroQuery
    )
}

@Composable
fun SetDefaultAndroidBrowserPane(
    clientLogger: ClientLogger,
    onBackPressed: () -> Unit,
    openAndroidDefaultBrowserSettings: () -> Unit,
    setDefaultAndroidBrowserManager: SetDefaultAndroidBrowserManager,
    showAsDialog: Boolean,
    onActivityResultCallback: () -> Unit,
    showZeroQuery: () -> Unit
) {
    val isRoleManagerAvailable = setDefaultAndroidBrowserManager.isRoleManagerAvailable()

    SetDefaultAndroidBrowserPane(
        mustOpenSettings = !isRoleManagerAvailable,
        showAsDialog = showAsDialog,
        onBackPressed = onBackPressed,
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
            if (isRoleManagerAvailable) {
                setDefaultAndroidBrowserManager.requestToBeDefaultBrowser(onActivityResultCallback)
            } else {
                openAndroidDefaultBrowserSettings()
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
            verticalArrangement = Arrangement.Top
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
                verticalArrangement = if (showAsDialog) {
                    Arrangement.Center
                } else {
                    Arrangement.Top
                }
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(space = Dimensions.PADDING_MEDIUM),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = stringResource(id = R.string.switch_default_browser_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = stringResource(id = R.string.switch_default_browser_promo),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (mustOpenSettings) {
                        // Because RoleManager is not available, we need to spell out the
                        // instructions to the user on how to change the default browser, since they
                        // will need to leave the app and manually make the change in Settings.
                        InstructionsForAndroidSettings()
                    } else {
                        // To avoid having a big empty space, show a promo image.
                        Image(
                            painter = painterResource(R.drawable.cookie_cutter_promo),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .align(Alignment.CenterHorizontally)
                                .clip(RoundedCornerShape(Dimensions.RADIUS_LARGE))
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(Dimensions.RADIUS_LARGE)
                                )
                        )
                    }
                }
            }

            SetDefaultAndroidBrowserButtons(
                mustOpenSettings = mustOpenSettings,
                onOpenSettings = onOpenSettings,
                showMaybeLater = showAsDialog,
                onMaybeLater = onMaybeLater
            )
        }
    }
}

@Composable
fun SetDefaultAndroidBrowserButtons(
    mustOpenSettings: Boolean,
    onOpenSettings: () -> Unit,
    showMaybeLater: Boolean,
    onMaybeLater: () -> Unit
) {
    val buttonResource = if (mustOpenSettings) {
        R.string.go_to_settings
    } else {
        R.string.switch_default_browser_title_confirm_button
    }
    StackedButtons(
        primaryLabel = stringResource(buttonResource),
        onPrimaryButton = onOpenSettings,
        secondaryLabel = if (showMaybeLater) {
            stringResource(id = R.string.maybe_later)
        } else {
            null
        },
        onSecondaryButton = onMaybeLater,
        modifier = Modifier.padding(
            horizontal = Dimensions.PADDING_HUGE,
            vertical = Dimensions.PADDING_MEDIUM
        )
    )
}

/**
 * Show instructions that will let the user navigate through Android's settings to set our app as
 * the default browser.
 */
@Composable
fun InstructionsForAndroidSettings() {
    Column(
        verticalArrangement = Arrangement.spacedBy(Dimensions.PADDING_MEDIUM),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(R.string.switch_default_browser_follow_3_steps),
            style = MaterialTheme.typography.bodyLarge
        )

        Surface(
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline
            ),
            shape = RoundedCornerShape(Dimensions.RADIUS_LARGE),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(
                    id = R.string.switch_default_browser_instructions
                ),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(Dimensions.PADDING_MEDIUM)
            )
        }
    }
}

@PortraitPreviews
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

@PortraitPreviewsDark
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

@PortraitPreviews
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

@PortraitPreviewsDark
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

@PortraitPreviews
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

@PortraitPreviewsDark
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

@PortraitPreviews
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

@PortraitPreviewsDark
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

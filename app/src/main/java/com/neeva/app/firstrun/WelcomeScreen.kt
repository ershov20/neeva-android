package com.neeva.app.firstrun

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.neeva.app.LocalIsDarkTheme
import com.neeva.app.R
import com.neeva.app.firstrun.widgets.buttons.OnboardingButton
import com.neeva.app.firstrun.widgets.texts.WelcomeHeader
import com.neeva.app.settings.SettingsController
import com.neeva.app.settings.mockSettingsControllerImpl
import com.neeva.app.ui.theme.ColorPalette
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.ui.theme.NeevaTheme

@Composable
fun WelcomeScreen(
    settingsController: SettingsController,
    navigateToZeroQuery: () -> Unit
) {
    val backgroundColor = if (LocalIsDarkTheme.current) {
        MaterialTheme.colorScheme.background
    } else {
        ColorPalette.Brand.Offwhite
    }

    val setDefaultAndroidBrowserManager = settingsController
        .getSetDefaultAndroidBrowserManager()

    Surface(color = backgroundColor) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(Dimensions.PADDING_LARGE)
        ) {
            WelcomeHeader(
                primaryLabel = stringResource(id = R.string.first_run_intro),
                secondaryLabel = stringResource(id = R.string.first_run_ad_free),
                modifier = Modifier
                    .padding(top = dimensionResource(id = R.dimen.sign_up_landing_padding_top))
            )

            Spacer(modifier = Modifier.height(28.dp))

            Image(
                painter = painterResource(id = R.drawable.default_browser_prompt),
                contentDescription = null
            )

            Spacer(modifier = Modifier.height(28.dp))

            when (setDefaultAndroidBrowserManager.isRoleManagerAvailable()) {
                true -> OnboardingButton(
                    text = stringResource(id = R.string.get_started),
                    onClick = {
                        setDefaultAndroidBrowserManager.requestToBeDefaultBrowser()
                        navigateToZeroQuery()
                    }
                )
                else -> OnboardingButton(
                    text = stringResource(id = R.string.get_started),
                    onClick = settingsController
                        .getOnClickMap(true)[R.string.settings_default_browser]!!
                )
            }
        }
    }
}

@Preview("1x scale", locale = "en")
@Preview("2x font size", locale = "en", fontScale = 2.0f)
@Composable
fun WelcomeScreen_Light_Preview() {
    NeevaTheme {
        WelcomeScreen(
            mockSettingsControllerImpl,
            navigateToZeroQuery = {}
        )
    }
}

@Preview("Dark 1x scale", locale = "en")
@Preview("Dark 2x scale", locale = "en", fontScale = 2.0f)
@Composable
fun WelcomeScreen_Dark_Preview() {
    NeevaTheme(useDarkTheme = true) {
        WelcomeScreen(
            mockSettingsControllerImpl,
            navigateToZeroQuery = {}
        )
    }
}

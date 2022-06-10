package com.neeva.app.firstrun

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
) {
    val backgroundColor = if (LocalIsDarkTheme.current) {
        MaterialTheme.colorScheme.background
    } else {
        ColorPalette.Brand.Offwhite
    }

    Surface(color = backgroundColor) {
        BoxWithConstraints {
            if (constraints.maxWidth > constraints.maxHeight) {
                // Landscape
                Row(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.weight(1.0f).fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.default_browser_prompt),
                            contentDescription = null,
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1.0f)
                            .fillMaxHeight()
                            .padding(horizontal = Dimensions.PADDING_LARGE),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.Center
                    ) {
                        WelcomeHeader(
                            primaryLabel = stringResource(id = R.string.first_run_intro),
                            secondaryLabel = stringResource(id = R.string.first_run_ad_free)
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        WelcomeScreenGetStartedButton(settingsController)
                    }
                }
            } else {
                // Portrait
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(Dimensions.PADDING_LARGE),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    WelcomeHeader(
                        primaryLabel = stringResource(id = R.string.first_run_intro),
                        secondaryLabel = stringResource(id = R.string.first_run_ad_free),
                        modifier = Modifier
                            .padding(
                                top = dimensionResource(id = R.dimen.sign_up_landing_padding_top)
                            )
                            .fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    Image(
                        painter = painterResource(id = R.drawable.default_browser_prompt),
                        contentDescription = null
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    WelcomeScreenGetStartedButton(settingsController)
                }
            }
        }
    }
}

@Composable
fun WelcomeScreenGetStartedButton(
    settingsController: SettingsController,
) {
    OnboardingButton(
        text = stringResource(id = R.string.get_started),
        onClick = settingsController
            .getOnClickMap(true)[R.string.settings_default_browser]!!
    )
}

@Preview("1x scale", locale = "en")
@Preview("2x font size", locale = "en", fontScale = 2.0f)
@Preview("Pixel 2 landscape, 1x scale", widthDp = 731, heightDp = 390, locale = "en")
@Composable
fun WelcomeScreen_Light_Preview() {
    NeevaTheme {
        WelcomeScreen(
            mockSettingsControllerImpl,
        )
    }
}

@Preview("Dark 1x scale", locale = "en")
@Preview("Dark 2x scale", locale = "en", fontScale = 2.0f)
@Preview("Pixel 2 landscape, 1x scale", widthDp = 731, heightDp = 390, locale = "en")
@Composable
fun WelcomeScreen_Dark_Preview() {
    NeevaTheme(useDarkTheme = true) {
        WelcomeScreen(
            mockSettingsControllerImpl,
        )
    }
}

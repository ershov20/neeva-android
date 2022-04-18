package com.neeva.app.firstrun

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices.PIXEL_C
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.neeva.app.R
import com.neeva.app.firstrun.widgets.OrSeparator
import com.neeva.app.firstrun.widgets.buttons.ToggleOnboardingButtons
import com.neeva.app.firstrun.widgets.texts.OnboardingTextField
import com.neeva.app.firstrun.widgets.texts.WelcomeHeader
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.ui.theme.NeevaTheme
import com.neeva.app.userdata.NeevaUser

@Composable
fun SignInScreenContainer(
    launchLoginIntent: (LaunchLoginIntentParams) -> Unit,
    onClose: () -> Unit,
    navigateToSignUp: () -> Unit,
    useDarkThemeForPreviews: Boolean? = null
) {
    val useDarkTheme = useDarkThemeForPreviews ?: isSystemInDarkTheme()

    OnboardingContainer(
        showBrowser = onClose,
        stickyFooterOnClick = navigateToSignUp,
        useDarkThemeForPreviews = useDarkTheme
    ) { modifier ->
        SignInScreen(
            launchLoginIntent = launchLoginIntent,
            useDarkThemeForPreviews = useDarkTheme,
            modifier = modifier
        )
    }
}

@Composable
fun SignInScreen(
    launchLoginIntent: (LaunchLoginIntentParams) -> Unit,
    useDarkThemeForPreviews: Boolean,
    modifier: Modifier
) {
    val email = rememberSaveable { mutableStateOf("") }

    Column(modifier = modifier) {
        WelcomeHeader(
            primaryLabel = stringResource(id = R.string.sign_in),
            modifier = Modifier
                .padding(top = dimensionResource(id = R.dimen.sign_up_landing_padding_top))
        )

        Spacer(modifier = Modifier.height(32.dp))

        OnboardingTextField(
            text = email.value,
            onTextChanged = { email.value = it },
            label = stringResource(id = R.string.email_label),
        )

        Spacer(modifier = Modifier.height(28.dp))

        OnboardingButton(
            emailProvided = email.value,
            signup = false,
            provider = NeevaUser.SSOProvider.OKTA,
            launchLoginIntent = launchLoginIntent,
            useDarkTheme = useDarkThemeForPreviews
        )

        Spacer(modifier = Modifier.height(16.dp))

        OrSeparator()

        Spacer(modifier = Modifier.height(16.dp))

        ToggleOnboardingButtons(
            signup = false,
            emailProvided = email.value,
            launchLoginIntent = launchLoginIntent,
            useDarkThemeForPreviews = useDarkThemeForPreviews
        )

        Spacer(modifier = Modifier.height(Dimensions.PADDING_SMALL))
    }
}

@Preview("1x scale", locale = "en")
@Preview("RTL, 1x scale", locale = "he")
@Composable
fun SignUpOther_Light_Preview() {
    NeevaTheme {
        SignInScreenContainer(
            launchLoginIntent = {},
            onClose = {},
            navigateToSignUp = {},
            useDarkThemeForPreviews = false
        )
    }
}

@Preview("Dark 1x scale", locale = "en")
@Preview("Dark RTL, 1x scale", locale = "he")
@Composable
fun SignUpOther_Dark_Preview() {
    NeevaTheme(useDarkTheme = true) {
        SignInScreenContainer(
            launchLoginIntent = {},
            onClose = {},
            navigateToSignUp = {},
            useDarkThemeForPreviews = true
        )
    }
}

@Preview("Light Landscape, 1x scale", heightDp = 400, locale = "en", device = PIXEL_C)
@Preview("Light Landscape, RTL, 1x scale", heightDp = 400, locale = "he", device = PIXEL_C)
@Composable
fun SignUpOther_Landscape_Preview() {
    NeevaTheme {
        SignInScreenContainer(
            launchLoginIntent = {},
            onClose = {},
            navigateToSignUp = {},
            useDarkThemeForPreviews = false
        )
    }
}

@Preview("Dark Landscape, 1x scale", heightDp = 400, locale = "en", device = PIXEL_C)
@Preview("Dark Landscape, RTL, 1x scale", heightDp = 400, locale = "he", device = PIXEL_C)
@Composable
fun SignUpOther_Dark_Landscape_Preview() {
    NeevaTheme(useDarkTheme = true) {
        SignInScreenContainer(
            launchLoginIntent = {},
            onClose = {},
            navigateToSignUp = {},
            useDarkThemeForPreviews = true
        )
    }
}

package com.neeva.app.firstrun.signup

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
import com.neeva.app.firstrun.FirstRunConstants
import com.neeva.app.firstrun.LaunchLoginIntentParams
import com.neeva.app.firstrun.OnboardingButton
import com.neeva.app.firstrun.OnboardingContainer
import com.neeva.app.firstrun.ToggleSignUpText
import com.neeva.app.firstrun.widgets.OrSeparator
import com.neeva.app.firstrun.widgets.buttons.ToggleOnboardingButtons
import com.neeva.app.firstrun.widgets.textfields.PasswordTextField
import com.neeva.app.firstrun.widgets.texts.BadPasswordText
import com.neeva.app.firstrun.widgets.texts.OnboardingTextField
import com.neeva.app.firstrun.widgets.texts.WelcomeHeader
import com.neeva.app.ui.theme.NeevaTheme
import com.neeva.app.userdata.NeevaUser

@Composable
fun SignUpWithOtherContainer(
    launchLoginIntent: (LaunchLoginIntentParams) -> Unit,
    onClose: () -> Unit,
    navigateToSignIn: () -> Unit,
    useDarkThemeForPreviews: Boolean? = null
) {
    val useDarkTheme = useDarkThemeForPreviews ?: isSystemInDarkTheme()
    OnboardingContainer(
        showBrowser = onClose,
        useDarkThemeForPreviews = useDarkTheme
    ) {
        SignUpWithOtherScreen(
            launchLoginIntent = launchLoginIntent,
            navigateToSignIn = navigateToSignIn,
            useDarkThemeForPreviews = useDarkTheme
        )
    }
}

@Composable
fun SignUpWithOtherScreen(
    launchLoginIntent: (LaunchLoginIntentParams) -> Unit,
    navigateToSignIn: () -> Unit,
    useDarkThemeForPreviews: Boolean
) {
    val email = rememberSaveable { mutableStateOf("") }
    val password = rememberSaveable { mutableStateOf("") }

    Column(modifier = FirstRunConstants.getScreenModifier()) {
        WelcomeHeader(
            primaryLabel = stringResource(id = R.string.first_run_intro),
            secondaryLabel = stringResource(id = R.string.first_run_create_your_free_account),
            modifier = Modifier
                .padding(top = dimensionResource(id = R.dimen.sign_up_with_other_padding_top))
        )

        Spacer(modifier = Modifier.height(32.dp))

        OnboardingTextField(
            text = email.value,
            onTextChanged = { email.value = it },
            label = stringResource(id = R.string.email_label),
        )

        Spacer(modifier = Modifier.height(32.dp))

        // TODO(kobec): implement bad password text
        PasswordTextField(
            text = password.value,
            onTextChanged = { password.value = it },
            label = stringResource(id = R.string.password_label)
        )

        BadPasswordText(password = password.value)

        OnboardingButton(
            emailProvided = email.value,
            passwordProvided = password.value,
            signup = true,
            provider = NeevaUser.SSOProvider.OKTA,
            launchLoginIntent = launchLoginIntent,
            useDarkTheme = useDarkThemeForPreviews
        )

        Spacer(modifier = Modifier.height(16.dp))

        OrSeparator()

        Spacer(modifier = Modifier.height(16.dp))

        ToggleOnboardingButtons(
            signup = true,
            emailProvided = email.value,
            launchLoginIntent = launchLoginIntent,
            useDarkThemeForPreviews = useDarkThemeForPreviews
        )

        Spacer(modifier = Modifier.weight(1.0f))

        ToggleSignUpText(true) {
            navigateToSignIn()
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Preview("1x scale", locale = "en")
@Preview("RTL, 1x scale", locale = "he")
@Composable
fun SignUpOther_Light_Preview() {
    NeevaTheme {
        SignUpWithOtherContainer(
            launchLoginIntent = {},
            onClose = {},
            navigateToSignIn = {}
        )
    }
}

@Preview("Dark 1x scale", locale = "en")
@Preview("Dark RTL, 1x scale", locale = "he")
@Composable
fun SignUpOther_Dark_Preview() {
    NeevaTheme(useDarkTheme = true) {
        SignUpWithOtherContainer(
            launchLoginIntent = {},
            onClose = {},
            navigateToSignIn = {},
            useDarkThemeForPreviews = true
        )
    }
}

@Preview("Light Landscape, 1x scale", heightDp = 400, locale = "en", device = PIXEL_C)
@Preview("Light Landscape, RTL, 1x scale", heightDp = 400, locale = "he", device = PIXEL_C)
@Composable
fun SignUpOther_Landscape_Preview() {
    NeevaTheme {
        SignUpWithOtherContainer(
            launchLoginIntent = {},
            onClose = {},
            navigateToSignIn = {}
        )
    }
}

@Preview("Dark Landscape, 1x scale", heightDp = 400, locale = "en", device = PIXEL_C)
@Preview("Dark Landscape, RTL, 1x scale", heightDp = 400, locale = "he", device = PIXEL_C)
@Composable
fun SignUpOther_Dark_Landscape_Preview() {
    NeevaTheme(useDarkTheme = true) {
        SignUpWithOtherContainer(
            launchLoginIntent = {},
            onClose = {},
            navigateToSignIn = {},
            useDarkThemeForPreviews = true
        )
    }
}

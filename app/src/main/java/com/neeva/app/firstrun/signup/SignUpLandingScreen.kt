package com.neeva.app.firstrun.signup

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices.PIXEL_C
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.neeva.app.LocalEnvironment
import com.neeva.app.NeevaConstants
import com.neeva.app.R
import com.neeva.app.firstrun.LaunchLoginIntentParams
import com.neeva.app.firstrun.OnboardingContainer
import com.neeva.app.firstrun.widgets.buttons.OnboardingButton
import com.neeva.app.firstrun.widgets.texts.AcknowledgementText
import com.neeva.app.firstrun.widgets.texts.EmailPromoCheckbox
import com.neeva.app.firstrun.widgets.texts.WelcomeHeader
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.ui.theme.NeevaTheme
import com.neeva.app.userdata.NeevaUser

@Composable
fun SignUpLandingContainer(
    launchLoginIntent: (LaunchLoginIntentParams) -> Unit,
    openInCustomTabs: (Uri) -> Unit,
    onClose: () -> Unit,
    navigateToSignIn: () -> Unit,
    showSignUpWithOther: () -> Unit
) {
    val neevaConstants: NeevaConstants = LocalEnvironment.current.neevaConstants

    SignUpLandingContainer(
        launchLoginIntent = launchLoginIntent,
        openInCustomTabs = openInCustomTabs,
        onClose = onClose,
        navigateToSignIn = navigateToSignIn,
        showSignUpWithOther = showSignUpWithOther,
        neevaConstants = neevaConstants
    )
}

@Composable
fun SignUpLandingContainer(
    launchLoginIntent: (LaunchLoginIntentParams) -> Unit,
    openInCustomTabs: (Uri) -> Unit,
    onClose: () -> Unit,
    navigateToSignIn: () -> Unit,
    showSignUpWithOther: () -> Unit,
    neevaConstants: NeevaConstants
) {
    OnboardingContainer(
        showBrowser = onClose,
        useSignUpStickyFooter = true, stickyFooterOnClick = navigateToSignIn
    ) { modifier ->
        SignUpLandingScreen(
            launchLoginIntent = launchLoginIntent,
            openInCustomTabs = openInCustomTabs,
            showSignUpWithOther = showSignUpWithOther,
            neevaConstants = neevaConstants,
            modifier = modifier
        )
    }
}

@Composable
fun SignUpLandingScreen(
    launchLoginIntent: (LaunchLoginIntentParams) -> Unit,
    openInCustomTabs: (Uri) -> Unit,
    showSignUpWithOther: () -> Unit,
    neevaConstants: NeevaConstants,
    modifier: Modifier
) {
    Column(modifier = modifier) {
        WelcomeHeader(
            primaryLabel = stringResource(id = R.string.first_run_intro),
            secondaryLabel = stringResource(id = R.string.first_run_create_your_free_account),
            modifier = Modifier
                .padding(top = dimensionResource(id = R.dimen.sign_up_landing_padding_top))
        )

        Spacer(modifier = Modifier.height(28.dp))

        OnboardingButton(
            signup = true,
            provider = NeevaUser.SSOProvider.GOOGLE,
            launchLoginIntent = launchLoginIntent
        )

        Spacer(modifier = Modifier.height(20.dp))

        OnboardingButton(text = stringResource(id = R.string.other_sign_up_options)) {
            showSignUpWithOther()
        }

        Spacer(modifier = Modifier.height(30.dp))

        EmailPromoCheckbox()

        Spacer(modifier = Modifier.height(38.dp))

        AcknowledgementText(
            openInCustomTabs = openInCustomTabs,
            appTermsURL = neevaConstants.appTermsURL,
            appPrivacyURL = neevaConstants.appPrivacyURL
        )

        Spacer(modifier = Modifier.height(Dimensions.PADDING_SMALL))
    }
}

@Preview("1x scale", locale = "en")
@Preview("RTL, 1x scale", locale = "he")
@Composable
fun SignUpLanding_Light_Preview() {
    NeevaTheme {
        SignUpLandingContainer(
            launchLoginIntent = {},
            openInCustomTabs = {},
            onClose = {},
            navigateToSignIn = {},
            showSignUpWithOther = {},
            neevaConstants = NeevaConstants()
        )
    }
}

@Preview("Dark 1x scale", locale = "en")
@Preview("Dark RTL, 1x scale", locale = "he")
@Composable
fun SignUpLanding_Dark_Preview() {
    NeevaTheme(useDarkTheme = true) {
        SignUpLandingContainer(
            launchLoginIntent = {},
            openInCustomTabs = {},
            onClose = {},
            navigateToSignIn = {},
            showSignUpWithOther = {},
            neevaConstants = NeevaConstants()
        )
    }
}

@Preview("Light Landscape, 1x scale", heightDp = 400, locale = "en", device = PIXEL_C)
@Preview("Light Landscape, RTL, 1x scale", heightDp = 400, locale = "he", device = PIXEL_C)
@Composable
fun SignUpLanding_Landscape_Preview() {
    NeevaTheme {
        SignUpLandingContainer(
            launchLoginIntent = {},
            openInCustomTabs = {},
            onClose = {},
            navigateToSignIn = {},
            showSignUpWithOther = {},
            neevaConstants = NeevaConstants()
        )
    }
}

@Preview("Dark Landscape, 1x scale", heightDp = 400, locale = "en", device = PIXEL_C)
@Preview("Dark Landscape, RTL, 1x scale", heightDp = 400, locale = "he", device = PIXEL_C)
@Composable
fun SignUpLanding_Dark_Landscape_Preview() {
    NeevaTheme(useDarkTheme = true) {
        SignUpLandingContainer(
            launchLoginIntent = {},
            openInCustomTabs = {},
            onClose = {},
            navigateToSignIn = {},
            showSignUpWithOther = {},
            neevaConstants = NeevaConstants()
        )
    }
}

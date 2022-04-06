package com.neeva.app.firstrun

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.neeva.app.R
import com.neeva.app.ui.TwoBooleanPreviewContainer
import com.neeva.app.ui.theme.getClickableAlpha
import com.neeva.app.ui.widgets.icons.SSOProviderImage
import com.neeva.app.userdata.NeevaUser

// TODO(kobec): Make it styled differently for Okta buttons
@Composable
fun OnboardingButton(
    emailProvided: String? = null,
    signup: Boolean,
    provider: NeevaUser.SSOProvider,
    launchLoginIntent: (LaunchLoginIntentParams) -> Unit,
    enabled: Boolean = true,
    useDarkTheme: Boolean
) {
    OnboardingButton(
        text = GetSSOProviderOnboardingText(provider, signup),
        enabled = enabled,
        useDarkTheme = useDarkTheme,
        startComposable = { SSOProviderImage(ssoProvider = provider) }
    ) {
        launchLoginIntent(
            LaunchLoginIntentParams(
                provider = provider,
                signup = signup,
                emailProvided = emailProvided
            )
        )
    }
}

@Composable
fun GetSSOProviderOnboardingText(provider: NeevaUser.SSOProvider, signup: Boolean): String {
    when (provider) {
        NeevaUser.SSOProvider.MICROSOFT ->
            return if (signup) {
                stringResource(R.string.sign_up_with_microsoft)
            } else {
                stringResource(R.string.sign_in_with_microsoft)
            }

        NeevaUser.SSOProvider.GOOGLE ->
            return if (signup) {
                stringResource(R.string.sign_up_with_google)
            } else {
                stringResource(R.string.sign_in_with_google)
            }

        NeevaUser.SSOProvider.OKTA ->
            return if (signup) {
                stringResource(R.string.sign_up_with_okta)
            } else {
                stringResource(R.string.sign_in_with_okta)
            }

        else -> throw IllegalStateException("Unsupported SSO Provider!")
    }
}

/** Standardized button used for Onboarding. */
@Composable
fun OnboardingButton(
    text: String,
    enabled: Boolean = true,
    useDarkTheme: Boolean,
    startComposable: @Composable (() -> Unit)? = null,
    onClick: () -> Unit
) {

    val backgroundColor = if (useDarkTheme) {
        MaterialTheme.colorScheme.background
    } else {
        MaterialTheme.colorScheme.onPrimary
    }

    // Currently in the API: When a button border is set, button elevation is discarded.
    // In dark mode, we want it to have no elevation and a border
    // In light mode, we want it to have elevation but no border.
    // source: https://github.com/neevaco/neeva-android/pull/498#discussion_r843428796
    val border = if (useDarkTheme) {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    } else {
        null
    }

    ElevatedButton(
        onClick = onClick,
        enabled = enabled,
        border = border,
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor
        ),
        shape = RoundedCornerShape(36.dp),
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .alpha(getClickableAlpha(enabled))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 20.dp)
        ) {
            startComposable?.let {
                it()
                Spacer(modifier = Modifier.width(16.dp))
            }

            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Preview("OnboardingButton Light LTR 1x scale", locale = "en")
@Preview("OnboardingButton Light 2x scale", locale = "en", fontScale = 2.0f)
@Preview("OnboardingButton Light 1x scale", locale = "he")
@Composable
fun OnboardingButtonPreview_Light() {
    TwoBooleanPreviewContainer(useDarkTheme = false) { hasStartComposable, isEnabled ->
        val startComposable = @Composable {
            SSOProviderImage(NeevaUser.SSOProvider.GOOGLE)
        }
        OnboardingButton(
            enabled = isEnabled,
            text = stringResource(R.string.sign_in_with_google),
            startComposable = startComposable.takeIf { hasStartComposable },
            useDarkTheme = false
        ) {}
    }
}

@Preview("OnboardingButton Dark LTR 1x scale", locale = "en")
@Preview("OnboardingButton Dark 2x scale", locale = "en", fontScale = 2.0f)
@Preview("OnboardingButton Dark 1x scale", locale = "he")
@Composable
fun OnboardingButtonPreview_Dark() {
    TwoBooleanPreviewContainer(useDarkTheme = true) { hasStartComposable, isEnabled ->
        val startComposable = @Composable {
            SSOProviderImage(NeevaUser.SSOProvider.GOOGLE)
        }
        OnboardingButton(
            enabled = isEnabled,
            text = stringResource(R.string.sign_in_with_google),
            startComposable = startComposable.takeIf { hasStartComposable },
            useDarkTheme = true
        ) {}
    }
}

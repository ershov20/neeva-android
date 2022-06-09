package com.neeva.app.firstrun.signup

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.neeva.app.LocalEnvironment
import com.neeva.app.R
import com.neeva.app.firstrun.LocalFirstRunModel
import com.neeva.app.firstrun.rememberSignInFlowNavModel
import com.neeva.app.ui.theme.Dimensions

@Composable
fun PreviewModeSignUpPrompt(
    query: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val firstRunModel = LocalFirstRunModel.current
    val signInFlowNavModel = rememberSignInFlowNavModel()
    val neevaConstants = LocalEnvironment.current.neevaConstants

    SignUpLandingScreen(
        launchLoginIntent = {
            firstRunModel.getLaunchLoginIntent(context).invoke(it)
            onDismiss()
        },
        openInCustomTabs = firstRunModel.openInCustomTabs(context),
        showSignUpWithOther = {
            signInFlowNavModel.navigateToSignUpWithOther()
            onDismiss()
        },
        neevaConstants = neevaConstants,
        primaryLabelString = stringResource(id = R.string.preview_overlay_prompt, query),
        welcomeHeaderModifier = Modifier,
        modifier = Modifier.padding(Dimensions.PADDING_LARGE)
    )
}

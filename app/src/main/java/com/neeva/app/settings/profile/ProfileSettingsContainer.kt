package com.neeva.app.settings

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import com.neeva.app.browsing.WebLayerModel
import com.neeva.app.settings.profile.ProfileSettingsPane
import com.neeva.app.storage.NeevaUser

@OptIn(ExperimentalAnimationApi::class)
@Composable

fun ProfileSettingsContainer(
    neevaUser: NeevaUser,
    webLayerModel: WebLayerModel,
    onBackPressed: () -> Unit
) {
    ProfileSettingsPane(
        onBackPressed = onBackPressed,
        signUserOut = {
            neevaUser.signOut(webLayerModel)
            onBackPressed()
        },
        neevaUserData = neevaUser.data
    )
}

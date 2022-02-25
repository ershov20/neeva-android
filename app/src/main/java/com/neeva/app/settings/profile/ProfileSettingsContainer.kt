package com.neeva.app.settings

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import com.neeva.app.browsing.WebLayerModel
import com.neeva.app.settings.profile.ProfileSettingsPane
import com.neeva.app.userdata.NeevaUser

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ProfileSettingsContainer(
    webLayerModel: WebLayerModel,
    neevaUser: NeevaUser,
    settingsViewModel: SettingsViewModel
) {
    /** Surface used to block touch propagation behind the surface. */
    Surface {
        ProfileSettingsPane(
            settingsViewModel = settingsViewModel,
            signUserOut = {
                neevaUser.signOut(webLayerModel)
                settingsViewModel.onBackPressed()
            },
            neevaUserData = neevaUser.data
        )
    }
}

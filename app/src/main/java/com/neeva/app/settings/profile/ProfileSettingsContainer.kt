package com.neeva.app.settings

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import com.neeva.app.LocalBrowserWrapper
import com.neeva.app.LocalEnvironment
import com.neeva.app.browsing.WebLayerModel
import com.neeva.app.settings.profile.ProfileSettingsPane
import com.neeva.app.storage.NeevaUser

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ProfileSettingsContainer(webLayerModel: WebLayerModel) {
    val neevaUserToken = LocalEnvironment.current.neevaUserToken
    val appNavModel = LocalEnvironment.current.appNavModel
    val activeTabModel = LocalBrowserWrapper.current.activeTabModel
    ProfileSettingsPane(
        onBackPressed = appNavModel::popBackStack,
        signUserOut = {
            NeevaUser.shared = NeevaUser()
            neevaUserToken.removeToken()
            webLayerModel.clearNeevaCookies()
            appNavModel.popBackStack()
            activeTabModel.reload()
        }
    )
}
